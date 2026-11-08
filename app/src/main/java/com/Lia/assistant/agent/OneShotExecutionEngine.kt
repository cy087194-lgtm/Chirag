package com.Lia.assistant.agent

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.action.AppLauncher
import com.Lia.assistant.action.InstalledAppLabelCache
import com.Lia.assistant.agent.core.ActionHistoryManager
import com.Lia.assistant.agent.core.ActionVerificationEngine
import com.Lia.assistant.agent.core.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

object OneShotExecutionEngine {

    private const val TAG = "OneShotExecEngine"
    private const val MAX_WAIT_PER_STEP_MS = 3000L
    private const val POLL_INTERVAL_MS = 80L

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("hi", "IN")
                    isTtsReady = true
                }
            }
        }
    }

    /**
     * Executes the user's composite voice command end-to-end in FULL-SPEED ONE-SHOT MODE.
     * Back-to-back execution without intermediate speaking or stops.
     */
    suspend fun executeOneShotCommand(
        context: Context,
        command: String
    ): OneShotExecutionResult = withContext(Dispatchers.IO) {
        initTts(context)

        // 1. Generate the complete ordered plan (ONE Gemini call or local fallback)
        val plan = OneShotPlanner.createPlan(context, command)
        val totalSteps = plan.steps.size

        Log.i(TAG, "Starting One-Shot Execution: ${plan.goal} ($totalSteps steps)")
        OneShotProgressHolder.startTask(plan.goal, totalSteps)

        val service = LiaAccessibilityService.instance
        if (service == null) {
            OneShotProgressHolder.finishTask()
            val failMsg = "Accessibility Service enable nahi hai. Task shuru nahi ho saka."
            speakOnce(failMsg)
            return@withContext OneShotExecutionResult(
                isSuccess = false,
                goal = plan.goal,
                totalSteps = totalSteps,
                completedSteps = 0,
                failedStepIndex = 1,
                failureReason = "accessibility_not_enabled",
                voiceSummary = failMsg
            )
        }

        var completedCount = 0
        var failedStepIndex: Int? = null
        var failureReason: String? = null

        // 2. Loop through ALL steps back-to-back without voice or waiting for user
        for (step in plan.steps) {
            val stepNumber = step.step
            OneShotProgressHolder.updateStep(stepNumber, totalSteps)
            Log.i(TAG, "Executing Step $stepNumber/$totalSteps: ${step.description} (${step.action})")

            // Proactively dismiss any popup/ad banner before executing step
            service.dismissPopupsAndAds()

            var stepSucceeded = false

            // Dynamic polling: wait up to 3s only until target screen/node appears
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < MAX_WAIT_PER_STEP_MS) {
                // Check if target action is already done (e.g. already liked/subscribed)
                if (step.canSkipIfDone && service.isAlreadyDone(step.target)) {
                    Log.i(TAG, "Step $stepNumber already completed (like/subscribe). Skipping immediately.")
                    stepSucceeded = true
                    break
                }

                // Execute action
                val actionResult = executeSingleStep(context, service, step)
                if (actionResult) {
                    stepSucceeded = true
                    break
                }

                // Check again for ads or popups that might have intercepted
                if (service.dismissPopupsAndAds()) {
                    delay(100)
                    continue
                }

                delay(POLL_INTERVAL_MS)
            }

            // 3. Fallback Ladder / Call Gemini recovery ONLY if step failed
            if (!stepSucceeded) {
                Log.w(TAG, "Step $stepNumber failed primary execution. Attempting fallback ladder.")
                stepSucceeded = attemptFallbackLadder(context, service, step)
            }

            if (stepSucceeded) {
                completedCount++
            } else {
                failedStepIndex = stepNumber
                failureReason = "Step $stepNumber (${step.description}) execute nahi ho saka"
                Log.e(TAG, "Step $stepNumber failed after all retries: $failureReason")
                break
            }
        }

        OneShotProgressHolder.finishTask()

        // 4. Final verification & record in ActionHistoryManager
        val isOverallSuccess = (completedCount == totalSteps)
        val finalStatus = if (isOverallSuccess) VerificationStatus.SUCCESS_VERIFIED else VerificationStatus.FAILED

        ActionHistoryManager.recordAction(
            taskId = "one_shot_${System.currentTimeMillis()}",
            toolName = "one_shot_execution",
            command = plan.goal,
            status = finalStatus,
            evidence = "Completed $completedCount of $totalSteps steps. Result: $finalStatus"
        )

        // 5. Speak ONCE at the end: "ho gaya" or exactly which step failed and why
        val finalVoiceMessage = if (isOverallSuccess) {
            "Ho gaya"
        } else {
            "Step $failedStepIndex par ${plan.steps.getOrNull(failedStepIndex?.minus(1) ?: 0)?.description ?: "action"} fail ho gaya: $failureReason"
        }

        speakOnce(finalVoiceMessage)

        OneShotExecutionResult(
            isSuccess = isOverallSuccess,
            goal = plan.goal,
            totalSteps = totalSteps,
            completedSteps = completedCount,
            failedStepIndex = failedStepIndex,
            failureReason = failureReason,
            voiceSummary = finalVoiceMessage
        )
    }

    private suspend fun executeSingleStep(
        context: Context,
        service: LiaAccessibilityService,
        step: OneShotStep
    ): Boolean {
        return when (step.action) {
            "launch_app" -> {
                val apps = InstalledAppLabelCache.get(context)
                val pkg = AppLauncher.findApp(step.target, apps) ?: step.target
                AppLauncher.launch(context, pkg)
            }

            "click", "tap" -> {
                // 1. Check fallback IDs first if available
                for (id in step.fallbackIds) {
                    val node = service.findNodeById(id)
                    if (node != null && service.clickNode(node)) return true
                }
                // 2. Check text or description
                val targetText = step.target.ifEmpty { step.description }
                val node = service.findNode(targetText) ?: service.findNodeByDesc(targetText)
                if (node != null) {
                    service.clickNode(node)
                } else {
                    service.tapText(targetText).startsWith("tapped")
                }
            }

            "type_and_search" -> {
                val q = step.query.ifEmpty { step.target }
                val typeResult = service.typeText(q)
                if (typeResult == "typed") {
                    // Tap search icon or search button if on screen
                    val searchSubmit = service.findNode("search")
                        ?: service.findNode("khojein")
                        ?: service.findNodeById("search_button")
                    if (searchSubmit != null) {
                        service.clickNode(searchSubmit)
                    }
                    true
                } else false
            }

            "click_first_video" -> {
                service.clickFirstVideoItem()
            }

            "fullscreen" -> {
                service.toggleFullscreen()
            }

            "scroll" -> {
                service.scroll("forward").startsWith("scrolled")
            }

            else -> {
                service.tapText(step.target).startsWith("tapped")
            }
        }
    }

    private suspend fun attemptFallbackLadder(
        context: Context,
        service: LiaAccessibilityService,
        step: OneShotStep
    ): Boolean {
        // Fallback 1: Dismiss any unexpected overlay or popup
        service.dismissPopupsAndAds()
        delay(150)

        // Fallback 2: Check by description
        val byDesc = service.findNodeByDesc(step.target)
        if (byDesc != null && service.clickNode(byDesc)) return true

        // Fallback 3: Check by ID
        for (id in step.fallbackIds) {
            val byId = service.findNodeById(id)
            if (byId != null && service.clickNode(byId)) return true
        }

        // Fallback 4: Call Gemini recovery ONLY if previous attempts failed
        val currentNodes = service.readScreen().map { it.text.ifEmpty { it.contentDescription } }.filter { it.isNotBlank() }
        val recoveryTarget = OneShotPlanner.requestGeminiRecovery(context, step, currentNodes)
        if (!recoveryTarget.isNullOrBlank()) {
            Log.i(TAG, "Gemini suggested recovery target: \"$recoveryTarget\"")
            val recNode = service.findNode(recoveryTarget) ?: service.findNodeByDesc(recoveryTarget)
            if (recNode != null && service.clickNode(recNode)) return true
            if (service.tapText(recoveryTarget).startsWith("tapped")) return true
        }

        return false
    }

    private fun speakOnce(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "one_shot_finish")
    }
}
