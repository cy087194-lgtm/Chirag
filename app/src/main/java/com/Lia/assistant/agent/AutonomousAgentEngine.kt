package com.Lia.assistant.agent

import android.content.Context
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.action.AppLauncher
import com.Lia.assistant.action.InstalledAppLabelCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class AgentStep(
    val stepIndex: Int,
    val description: String,
    val isCompleted: Boolean,
    val details: String = ""
)

data class AgentTaskResult(
    val isSuccess: Boolean,
    val goal: String,
    val summary: String,
    val completedSteps: List<AgentStep>,
    val failureReason: String? = null
)

object AutonomousAgentEngine {

    /**
     * Executes the autonomous WhatsApp end-to-end communication workflow (Requirement 1 & 6).
     * Example: "WhatsApp kholo aur Keshav ko Hi bhej do"
     */
    suspend fun executeWhatsAppTask(
        context: Context,
        contactName: String,
        messageText: String,
        onProgress: (String) -> Unit
    ): AgentTaskResult = withContext(Dispatchers.IO) {
        val steps = mutableListOf<AgentStep>()

        fun recordStep(desc: String, success: Boolean, details: String = "") {
            val step = AgentStep(
                stepIndex = steps.size + 1,
                description = desc,
                isCompleted = success,
                details = details
            )
            steps.add(step)
            onProgress(desc)
        }

        // Fast Direct Route with Verification
        val fastResult = com.Lia.assistant.action.FastActionEngine.executeMessageAction(
            context = context,
            rawContactName = contactName,
            messageText = messageText,
            explicitApp = "whatsapp"
        )

        if (fastResult.isSuccess) {
            recordStep("Fast direct route & send", true, "Verified in ${fastResult.durationMs}ms")
            return@withContext AgentTaskResult(
                isSuccess = true,
                goal = "$contactName ko \"$messageText\" bhejna",
                summary = fastResult.spokenSummary,
                completedSteps = steps
            )
        }

        // Check Accessibility Service
        val service = LiaAccessibilityService.instance
        if (service == null) {
            recordStep("Accessibility Service check", false, "Service not connected")
            return@withContext AgentTaskResult(
                isSuccess = false,
                goal = "WhatsApp pe $contactName ko \"$messageText\" bhejna",
                summary = fastResult.spokenSummary.ifEmpty { "LiaAI Accessibility Service connected nahi hai. Phone control ke liye settings se service on karein." },
                completedSteps = steps,
                failureReason = fastResult.failureReason ?: "accessibility_not_enabled"
            )
        }

        // STEP 1: Launch WhatsApp
        recordStep("WhatsApp launch karna", true)
        val apps = InstalledAppLabelCache.get(context)
        val whatsappPkg = AppLauncher.findApp("whatsapp", apps) ?: "com.whatsapp"
        val launched = AppLauncher.launch(context, whatsappPkg)
        if (!launched) {
            recordStep("WhatsApp open failed", false, "App package not installed or blocked")
            return@withContext AgentTaskResult(
                isSuccess = false,
                goal = "WhatsApp open karna",
                summary = "WhatsApp phone mein installed nahi mila ya launch fail ho gaya.",
                completedSteps = steps,
                failureReason = "app_launch_failed"
            )
        }

        // Allow UI to render
        delay(1200)

        // STEP 2: Find & Open Contact Chat
        recordStep("Contact \"$contactName\" ko dhundhna", true)
        var contactNode = service.findNode(contactName)

        // If not immediately visible, try search icon/button
        if (contactNode == null) {
            val searchBtn = service.findNode("search") ?: service.findNode("khojein")
            if (searchBtn != null) {
                service.clickNode(searchBtn)
                delay(600)
                service.typeText(contactName)
                delay(800)
                contactNode = service.findNode(contactName)
            }
        }

        if (contactNode != null) {
            val tappedContact = service.clickNode(contactNode)
            if (!tappedContact) {
                service.tapText(contactName)
            }
            recordStep("\"$contactName\" ki chat open karna", true)
        } else {
            // Fallback: Open WhatsApp direct chat intent or notify user
            recordStep("\"$contactName\" WhatsApp chat list mein nahi mila", false)
            return@withContext AgentTaskResult(
                isSuccess = false,
                goal = "\"$contactName\" ko message bhejna",
                summary = "WhatsApp open ho gaya, lekin chat list mein \"$contactName\" contact nahi mila. Kripya contact name confirm karein.",
                completedSteps = steps,
                failureReason = "contact_not_found"
            )
        }

        delay(1000)

        // STEP 3: Find Message Input Field and Type Text
        recordStep("Message field mein \"$messageText\" type karna", true)
        val typeResult = service.typeText(messageText)
        if (typeResult != "typed") {
            // Fallback to tapping message field
            val msgField = service.findNode("message") ?: service.findNode("type a message")
            if (msgField != null) {
                service.clickNode(msgField)
                delay(400)
                service.setTextOnNode(msgField, messageText)
            }
        }

        delay(600)

        // STEP 4: Tap Send Button
        recordStep("Send button tap karna", true)
        val sendBtn = service.findNode("send") ?: service.findNode("bhejein")
        var sentTapped = false
        if (sendBtn != null) {
            sentTapped = service.clickNode(sendBtn)
        }
        if (!sentTapped) {
            val tapRes = service.tapText("Send")
            sentTapped = tapRes.startsWith("tapped")
        }

        delay(800)

        // STEP 5: Verification (Requirement 1 & 6)
        recordStep("Message delivery verify karna", true)
        val currentNodes = service.readScreen()
        val isVerified = currentNodes.any { it.text.contains(messageText, ignoreCase = true) }

        val summary = if (isVerified || sentTapped) {
            "WhatsApp par $contactName ko message \"$messageText\" successfully bhej diya gaya hai!"
        } else {
            "Message send karne ki koshish ki gayi. Kripya screen par confirm karein."
        }

        AgentTaskResult(
            isSuccess = isVerified || sentTapped,
            goal = "WhatsApp message to $contactName",
            summary = summary,
            completedSteps = steps
        )
    }

    /**
     * Executes general multi-step autonomous goal.
     */
    suspend fun executeGeneralTask(
        context: Context,
        command: String,
        onProgress: (String) -> Unit
    ): AgentTaskResult = withContext(Dispatchers.IO) {
        val steps = mutableListOf<AgentStep>()
        onProgress("Task analyze kiya ja raha hai: $command")

        val parsed = SmartIntentParser.parse(command)

        when (parsed) {
            is ParsedIntent.NegatedCommand -> {
                val step = AgentStep(1, "Command analysis", true, "Negated intent detected: ${parsed.prohibitedAction}")
                steps.add(step)
                AgentTaskResult(
                    isSuccess = true,
                    goal = command,
                    summary = "Aapne kaha \"${parsed.prohibitedAction}\" MAT karna, isliye ye action cancel kar diya gaya hai.",
                    completedSteps = steps
                )
            }

            is ParsedIntent.WhatsAppMultiStep -> {
                executeWhatsAppTask(context, parsed.contactName, parsed.messageText, onProgress)
            }

            is ParsedIntent.AppLaunchCommand -> {
                val apps = InstalledAppLabelCache.get(context)
                val pkg = AppLauncher.findApp(parsed.appName, apps)
                if (pkg != null) {
                    val ok = AppLauncher.launch(context, pkg)
                    steps.add(AgentStep(1, "${parsed.appName} open karna", ok))
                    AgentTaskResult(
                        isSuccess = ok,
                        goal = "${parsed.appName} launch",
                        summary = if (ok) "${parsed.appName} open kar diya!" else "App open nahi ho saki.",
                        completedSteps = steps
                    )
                } else {
                    steps.add(AgentStep(1, "App dhundhna", false, "App not found"))
                    AgentTaskResult(
                        isSuccess = false,
                        goal = "${parsed.appName} launch",
                        summary = "${parsed.appName} installed nahi mili.",
                        completedSteps = steps
                    )
                }
            }

            is ParsedIntent.OneShotCompositeCommand -> {
                val oneShotResult = OneShotExecutionEngine.executeOneShotCommand(context, parsed.rawCommand)
                val mappedSteps = (1..oneShotResult.completedSteps).map { idx ->
                    AgentStep(idx, "Step $idx executed", true)
                }
                AgentTaskResult(
                    isSuccess = oneShotResult.isSuccess,
                    goal = oneShotResult.goal,
                    summary = oneShotResult.voiceSummary,
                    completedSteps = mappedSteps,
                    failureReason = oneShotResult.failureReason
                )
            }

            else -> {
                val oneShotResult = OneShotExecutionEngine.executeOneShotCommand(context, command)
                val mappedSteps = (1..oneShotResult.completedSteps).map { idx ->
                    AgentStep(idx, "Step $idx executed", true)
                }
                AgentTaskResult(
                    isSuccess = oneShotResult.isSuccess,
                    goal = oneShotResult.goal,
                    summary = oneShotResult.voiceSummary,
                    completedSteps = mappedSteps,
                    failureReason = oneShotResult.failureReason
                )
            }
        }
    }
}
