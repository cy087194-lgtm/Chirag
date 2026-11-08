package com.Lia.assistant.agent.vision

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.agent.core.ActionHistoryManager
import com.Lia.assistant.agent.core.VerificationStatus
import com.Lia.assistant.brain.ModelRouter
import com.Lia.assistant.brain.TaskType
import com.Lia.assistant.brain.ChatMessageTurn
import com.Lia.assistant.brain.ProviderReply
import com.Lia.assistant.voice.NovaOrbState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

object AlwaysWatchManager {

    private const val TAG = "AlwaysWatchManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var watchJob: Job? = null

    private val _isWatching = MutableStateFlow(false)
    val isWatching: StateFlow<Boolean> = _isWatching.asStateFlow()

    private val _watchCondition = MutableStateFlow<String?>(null)
    val watchCondition: StateFlow<String?> = _watchCondition.asStateFlow()

    private val _lastAlert = MutableStateFlow<String?>(null)
    val lastAlert: StateFlow<String?> = _lastAlert.asStateFlow()

    private var previousTreeHash: Int = 0
    private var tts: TextToSpeech? = null

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("hi", "IN")
                }
            }
        }
    }

    /**
     * Starts continuous screen watching until user explicitly commands stop.
     */
    fun startWatching(context: Context, condition: String? = null): String {
        initTts(context)
        val service = LiaAccessibilityService.instance
        if (service == null) {
            return "LiaAI Accessibility Service connected nahi hai. Screen watch karne ke liye Settings se accessibility enable karein."
        }

        _isWatching.value = true
        _watchCondition.value = condition
        _lastAlert.value = null

        watchJob?.cancel()
        watchJob = scope.launch {
            Log.i(TAG, "Always-Watch Mode started. Condition: \"$condition\"")
            val confirmation = if (condition.isNullOrBlank()) {
                "Screen monitor kar rahi hoon. Jab tak aap 'stop' ya 'bas karo' nahi kahenge, main monitor karti rahoongi."
            } else {
                "Screen watch mode active hai. Jaise hi \"$condition\" trigger hoga, main aapko alert karungi."
            }
            speakAlert(confirmation)

            ActionHistoryManager.recordAction(
                taskId = "always_watch_${System.currentTimeMillis()}",
                toolName = "always_watch_mode",
                command = "Start watching: ${condition ?: "continuous"}",
                status = VerificationStatus.SUCCESS_VERIFIED,
                evidence = "Always-Watch loop initiated."
            )

            while (isActive && _isWatching.value) {
                try {
                    // Check every 2.0 seconds (battery saving change detection)
                    delay(2000L)
                    val nodes = service.readScreen()

                    // Check for protected screens (FLAG_SECURE, banking, UPI)
                    val activePackage = LiaAccessibilityService.currentPackage.value.lowercase()
                    if (isProtectedScreen(activePackage, nodes.map { it.text })) {
                        speakAlert("Yeh protected screen hai (jaise banking/UPI), Android security policy ki wajah se isko read nahi kiya ja sakta.")
                        delay(6000L)
                        continue
                    }

                    // Hash check: Only evaluate if the screen content has changed!
                    val textList = nodes.map { it.text.ifEmpty { it.contentDescription } }.filter { it.isNotBlank() }
                    val currentHash = textList.hashCode()

                    if (currentHash != previousTreeHash) {
                        previousTreeHash = currentHash
                        val fullScreenText = textList.joinToString(" | ")

                        // Evaluate condition or general alert
                        if (!condition.isNullOrBlank()) {
                            evaluateCondition(context, condition, fullScreenText)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Watch loop iteration error: ${e.message}")
                }
            }
        }

        return if (condition.isNullOrBlank()) {
            "Always-Watch Mode on ho gaya hai. Main screen monitor kar rahi hoon."
        } else {
            "Always-Watch Mode shuru ho gaya hai. Condition: \"$condition\""
        }
    }

    private suspend fun evaluateCondition(context: Context, condition: String, screenText: String) {
        val lowerText = screenText.lowercase()
        val lowerCondition = condition.lowercase()

        val keywordTrigger = when {
            lowerCondition.contains("error") -> lowerText.contains("error") || lowerText.contains("fail") || lowerText.contains("galti")
            lowerCondition.contains("price") -> lowerText.contains("₹") || lowerText.contains("price") || lowerText.contains("off")
            lowerCondition.contains("otp") -> lowerText.contains("otp") || lowerText.contains("code")
            else -> lowerText.contains(lowerCondition)
        }

        if (keywordTrigger) {
            val alertMsg = "Alert: Screen par condition detect hui: $condition!"
            _lastAlert.value = alertMsg
            speakAlert(alertMsg)
        }
    }

    /**
     * Checks if current screen is protected (Banking, UPI, Google Pay, PhonePe, Paytm).
     */
    fun isProtectedScreen(packageName: String, screenTexts: List<String>): Boolean {
        val protectedPackages = listOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.phonepe.app",
            "net.one97.paytm",
            "com.sbi.upi",
            "com.icicibank",
            "com.hdfcbank"
        )
        if (protectedPackages.any { packageName.contains(it) }) return true

        val lowerJoined = screenTexts.joinToString(" ").lowercase()
        return lowerJoined.contains("upi pin") || lowerJoined.contains("enter atm pin") || lowerJoined.contains("cvv")
    }

    /**
     * Stops continuous screen watching.
     */
    fun stopWatching(): String {
        _isWatching.value = false
        _watchCondition.value = null
        watchJob?.cancel()
        watchJob = null
        speakAlert("Screen watch mode band kar diya gaya hai.")
        return "Screen watch mode band kar diya gaya hai."
    }

    private fun speakAlert(msg: String) {
        tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "always_watch_alert")
    }
}
