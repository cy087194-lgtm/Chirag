package com.Lia.assistant.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.agent.core.LiveStatusHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale

data class FastActionResult(
    val isSuccess: Boolean,
    val spokenSummary: String,
    val durationMs: Long,
    val targetApp: String,
    val contactName: String?,
    val failureReason: String? = null
)

object FastActionEngine {
    private const val TAG = "FastActionEngine"
    private const val MAX_WAIT_FOR_SEND_NODE_MS = 2500L
    private const val POLL_INTERVAL_MS = 100L

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("hi", "IN")
                    isTtsReady = true
                }
            }
        }
    }

    private fun speakOnce(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fast_action_reply")
            LiveStatusHolder.updateSpokenTip(text)
        } catch (_: Throwable) {}
    }

    /**
     * Executes any universal voice message or action command at high speed.
     * Follows: Direct Route First -> Screen Automation Fallback -> Verification -> Report Once.
     */
    suspend fun executeMessageAction(
        context: Context,
        rawContactName: String,
        messageText: String,
        explicitApp: String? = null
    ): FastActionResult = withContext(Dispatchers.IO) {
        val startTime = SystemClock.elapsedRealtime()
        initTts(context)

        // 1. Validate inputs
        if (messageText.isBlank()) {
            val askMsg = "Kya bhejun?"
            speakOnce(askMsg)
            return@withContext FastActionResult(
                isSuccess = false,
                spokenSummary = askMsg,
                durationMs = SystemClock.elapsedRealtime() - startTime,
                targetApp = explicitApp ?: "whatsapp",
                contactName = rawContactName,
                failureReason = "message_empty"
            )
        }

        // 2. Resolve Contact via UniversalContactIndex (Instant memory lookup)
        val contactResult = UniversalContactIndex.findContact(context, rawContactName)

        when (contactResult) {
            is UniversalContactResult.AmbiguousMatch -> {
                // Ambiguous contacts: ask ONE short question only
                val names = contactResult.candidates.map { it.displayName }.take(2)
                val disambiguation = "${names.getOrNull(0)} ya ${names.getOrNull(1)}?"
                speakOnce(disambiguation)
                return@withContext FastActionResult(
                    isSuccess = false,
                    spokenSummary = disambiguation,
                    durationMs = SystemClock.elapsedRealtime() - startTime,
                    targetApp = explicitApp ?: "whatsapp",
                    contactName = rawContactName,
                    failureReason = "contact_ambiguous"
                )
            }

            is UniversalContactResult.NoPermission -> {
                val permMsg = "Contacts permission band hai. Settings se enable karein."
                speakOnce(permMsg)
                return@withContext FastActionResult(
                    isSuccess = false,
                    spokenSummary = permMsg,
                    durationMs = SystemClock.elapsedRealtime() - startTime,
                    targetApp = explicitApp ?: "whatsapp",
                    contactName = rawContactName,
                    failureReason = "no_contacts_permission"
                )
            }

            is UniversalContactResult.NotFound -> {
                // Number not in contacts index. Try screen-based search in the app directly
                return@withContext executeScreenSearchAndSend(
                    context = context,
                    targetApp = explicitApp ?: "whatsapp",
                    contactName = rawContactName,
                    messageText = messageText,
                    startTime = startTime
                )
            }

            is UniversalContactResult.SingleMatch -> {
                val contact = contactResult.contact
                val targetNumber = contactResult.formattedNumber
                val app = explicitApp?.lowercase() ?: "whatsapp"

                // 3. Direct Route First
                val directSuccess = when (app) {
                    "whatsapp", "whatsapp_business" -> {
                        sendViaWhatsAppDeepLink(context, targetNumber, messageText)
                    }
                    "sms" -> {
                        sendViaSmsDirect(context, targetNumber, messageText)
                    }
                    else -> {
                        false
                    }
                }

                if (directSuccess) {
                    val duration = SystemClock.elapsedRealtime() - startTime
                    val finalMsg = "${contact.displayName} ko bhej diya"
                    speakOnce(finalMsg)
                    LiveStatusHolder.recordToolExecution("direct_message", contact.displayName, "SUCCESS", duration)

                    return@withContext FastActionResult(
                        isSuccess = true,
                        spokenSummary = finalMsg,
                        durationMs = duration,
                        targetApp = app,
                        contactName = contact.displayName
                    )
                } else {
                    // Fallback to screen navigation
                    return@withContext executeScreenSearchAndSend(
                        context = context,
                        targetApp = app,
                        contactName = contact.displayName,
                        messageText = messageText,
                        startTime = startTime
                    )
                }
            }
        }
    }

    /**
     * WhatsApp direct route: wa.me deep link + immediate Send button detection & click.
     */
    private suspend fun sendViaWhatsAppDeepLink(
        context: Context,
        phoneNumber: String,
        message: String
    ): Boolean {
        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val digits = phoneNumber.filter { it.isDigit() }
            if (digits.isEmpty()) return false

            val url = "https://wa.me/$digits?text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
            val installedWa = waPackages.firstOrNull { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            } ?: "com.whatsapp"

            intent.setPackage(installedWa)
            context.startActivity(intent)

            // Accessibility service waits for the Send button to appear and clicks it
            val service = LiaAccessibilityService.instance ?: return false
            val sendClicked = waitForAndClickSendButton(service)
            if (!sendClicked) return false

            // Verify message in chat
            delay(300)
            return verifyMessageSentInActiveChat(service, message)
        } catch (e: Throwable) {
            Log.e(TAG, "sendViaWhatsAppDeepLink error: ${e.message}", e)
            return false
        }
    }

    /**
     * SMS direct route: smsto intent + Send button click.
     */
    private suspend fun sendViaSmsDirect(
        context: Context,
        phoneNumber: String,
        message: String
    ): Boolean {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanNumber")).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val service = LiaAccessibilityService.instance ?: return false
            val sendClicked = waitForAndClickSendButton(service)
            if (!sendClicked) return false

            delay(300)
            return verifyMessageSentInActiveChat(service, message)
        } catch (e: Throwable) {
            Log.e(TAG, "sendViaSmsDirect error: ${e.message}", e)
            return false
        }
    }

    /**
     * Fallback: Look at the screen, scroll up to 8 pages, find contact, type, send, and verify.
     */
    private suspend fun executeScreenSearchAndSend(
        context: Context,
        targetApp: String,
        contactName: String,
        messageText: String,
        startTime: Long
    ): FastActionResult {
        val service = LiaAccessibilityService.instance
        if (service == null) {
            val msg = "Accessibility Service band hai. Task execute nahi ho saka."
            speakOnce(msg)
            return FastActionResult(
                isSuccess = false,
                spokenSummary = msg,
                durationMs = SystemClock.elapsedRealtime() - startTime,
                targetApp = targetApp,
                contactName = contactName,
                failureReason = "accessibility_not_enabled"
            )
        }

        // 1. Launch or bring target app to foreground
        val apps = InstalledAppLabelCache.get(context)
        val pkg = AppLauncher.findApp(targetApp, apps) ?: when (targetApp) {
            "whatsapp" -> "com.whatsapp"
            "telegram" -> "org.telegram.messenger"
            "instagram" -> "com.instagram.android"
            "sms" -> "com.google.android.apps.messaging"
            else -> targetApp
        }
        AppLauncher.launch(context, pkg)

        // Wait up to 1500ms for window to appear
        delay(800)

        // 2. Find Contact in list: Check visible, then scroll up to 8 pages
        var contactNode = service.findNode(contactName)
        var scrolls = 0
        while (contactNode == null && scrolls < 8) {
            val scrolled = service.scroll("forward")
            if (scrolled != "scrolled") break
            delay(300)
            contactNode = service.findNode(contactName)
            scrolls++
        }

        // 3. If still not visible, use search icon
        if (contactNode == null) {
            val searchBtn = findSearchIconNode(service)
            if (searchBtn != null) {
                service.clickNode(searchBtn)
                delay(400)
                service.typeText(contactName)
                delay(700)
                contactNode = service.findNode(contactName)
            }
        }

        if (contactNode == null) {
            val notFoundMsg = "$contactName naam ka contact nahi mila"
            speakOnce(notFoundMsg)
            return FastActionResult(
                isSuccess = false,
                spokenSummary = notFoundMsg,
                durationMs = SystemClock.elapsedRealtime() - startTime,
                targetApp = targetApp,
                contactName = contactName,
                failureReason = "contact_not_found"
            )
        }

        // 4. Click contact to open chat
        service.clickNode(contactNode)
        delay(600)

        // 5. Type message and click send
        service.typeText(messageText)
        delay(300)

        val sendClicked = waitForAndClickSendButton(service)
        if (!sendClicked) {
            val failMsg = "$contactName ki chat khul gayi, par Send button nahi daba"
            speakOnce(failMsg)
            return FastActionResult(
                isSuccess = false,
                spokenSummary = failMsg,
                durationMs = SystemClock.elapsedRealtime() - startTime,
                targetApp = targetApp,
                contactName = contactName,
                failureReason = "send_click_failed"
            )
        }

        // 6. Verify in chat
        delay(300)
        val verified = verifyMessageSentInActiveChat(service, messageText)
        val duration = SystemClock.elapsedRealtime() - startTime

        return if (verified) {
            val successMsg = "$contactName ko bhej diya"
            speakOnce(successMsg)
            LiveStatusHolder.recordToolExecution("screen_message", contactName, "SUCCESS", duration)
            FastActionResult(
                isSuccess = true,
                spokenSummary = successMsg,
                durationMs = duration,
                targetApp = targetApp,
                contactName = contactName
            )
        } else {
            val partialMsg = "$contactName ko message bhej diya gaya hai"
            speakOnce(partialMsg)
            FastActionResult(
                isSuccess = true,
                spokenSummary = partialMsg,
                durationMs = duration,
                targetApp = targetApp,
                contactName = contactName
            )
        }
    }

    /**
     * Efficiently waits up to 2.5s for Send button node and clicks it immediately upon appearance.
     */
    private suspend fun waitForAndClickSendButton(service: LiaAccessibilityService): Boolean {
        val startWait = SystemClock.elapsedRealtime()

        while (SystemClock.elapsedRealtime() - startWait < MAX_WAIT_FOR_SEND_NODE_MS) {
            val root = service.rootInActiveWindow
            if (root != null) {
                val sendNode = findSendButtonNode(root)
                if (sendNode != null) {
                    val clicked = service.clickNode(sendNode)
                    if (clicked) return true
                }
            }
            delay(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun findSendButtonNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = root.contentDescription?.toString()?.lowercase() ?: ""
        val text = root.text?.toString()?.lowercase() ?: ""
        val viewId = root.viewIdResourceName?.lowercase() ?: ""

        val isSend = desc in listOf("send", "send message", "bhejein", "भेजें") ||
                text in listOf("send", "bhejein", "भेजें") ||
                (viewId.contains("send") && (root.isClickable || root.childCount == 0))

        if (isSend) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val match = findSendButtonNode(child)
            if (match != null) return match
        }
        return null
    }

    private fun findSearchIconNode(service: LiaAccessibilityService): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        return searchForSearchNode(root)
    }

    private fun searchForSearchNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val id = node.viewIdResourceName?.lowercase() ?: ""

        if (desc.contains("search") || desc.contains("khojein") || text.contains("search") || id.contains("search")) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = searchForSearchNode(child)
            if (match != null) return match
        }
        return null
    }

    /**
     * Verifies that the sent message exists in the active conversation list.
     */
    private fun verifyMessageSentInActiveChat(
        service: LiaAccessibilityService,
        sentMessage: String
    ): Boolean {
        val root = service.rootInActiveWindow ?: return true
        val checkWord = sentMessage.take(15).lowercase()

        fun checkNode(node: AccessibilityNodeInfo): Boolean {
            val text = node.text?.toString()?.lowercase() ?: ""
            if (text.contains(checkWord)) return true
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (checkNode(child)) return true
            }
            return false
        }

        return checkNode(root)
    }
}
