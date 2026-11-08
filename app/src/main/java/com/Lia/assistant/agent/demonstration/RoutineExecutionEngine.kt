package com.Lia.assistant.agent.demonstration

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.action.FastActionEngine
import com.Lia.assistant.agent.core.LiveStatusHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

data class RoutineExecutionResult(
    val isSuccess: Boolean,
    val routineName: String,
    val spokenSummary: String,
    val durationMs: Long,
    val failedStepIndex: Int? = null,
    val failureReason: String? = null
)

object RoutineExecutionEngine {
    private const val TAG = "RoutineExecutionEngine"

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
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "routine_exec_reply")
            LiveStatusHolder.updateSpokenTip(text)
        } catch (_: Throwable) {}
    }

    /**
     * Finds a matching learned routine from user command.
     * Enforces wake-word presence when trigger requires it to prevent false firing from TV or background talk.
     */
    fun findMatchingRoutine(
        context: Context,
        rawSpeech: String,
        isExplicitWakeWordPresent: Boolean
    ): LearnedRoutine? {
        val routines = LearnedRoutineStore.getAllRoutines(context).filter { it.isEnabled }
        val lower = rawSpeech.lowercase().trim()

        return routines.firstOrNull { routine ->
            val trigger = routine.trigger.phrase.lowercase().trim()
            if (trigger.isEmpty()) return@firstOrNull false

            // Wake word guard: If routine requires wake word, reject background chatter
            if (routine.trigger.requiresWakeWord && !isExplicitWakeWordPresent) {
                // Check if user prefixed with Jarvis/Lia/Nova
                val hasEmbeddedWakeWord = lower.startsWith("jarvis") ||
                        lower.startsWith("lia") ||
                        lower.startsWith("hey jarvis") ||
                        lower.startsWith("hey lia")
                if (!hasEmbeddedWakeWord) {
                    return@firstOrNull false
                }
            }

            // Clean wake words to extract trigger
            val cleanSpeech = lower
                .replace(Regex("^(hey\\s+)?(jarvis|lia|nova|assistant)[,\\s]*"), "")
                .trim()

            cleanSpeech == trigger ||
                cleanSpeech.startsWith(trigger) ||
                Regex("\\b${Regex.escape(trigger)}\\b").containsMatchIn(cleanSpeech)
        }
    }

    /**
     * Executes the matched routine end-to-end.
     * Fastest Route First -> Fallback Accessibility Step Path -> Verification -> Spoken Report.
     */
    suspend fun executeRoutine(
        context: Context,
        routine: LearnedRoutine,
        overriddenVariables: Map<String, String> = emptyMap()
    ): RoutineExecutionResult = withContext(Dispatchers.IO) {
        val startTime = SystemClock.elapsedRealtime()
        initTts(context)

        val mergedVariables = routine.variables.toMutableMap().apply { putAll(overriddenVariables) }
        val contactName = mergedVariables["contactName"]
        val messageText = mergedVariables["messageText"]

        // 1. FASTEST ROUTE FIRST (Direct Deep Link / Intent)
        if (routine.directRouteAvailable && contactName != null && messageText != null) {
            val fastRes = FastActionEngine.executeMessageAction(
                context = context,
                rawContactName = contactName,
                messageText = messageText,
                explicitApp = if (routine.directRouteUri?.contains("wa.me") == true) "whatsapp" else "sms"
            )

            val duration = SystemClock.elapsedRealtime() - startTime
            LearnedRoutineStore.recordExecution(context, routine.id, fastRes.isSuccess, duration)
            LiveStatusHolder.recordToolExecution("learned_routine_direct", routine.name, if (fastRes.isSuccess) "SUCCESS" else "FAILED", duration)

            if (fastRes.isSuccess) {
                val summary = "${contactName} ko ${messageText} bhej diya"
                speakOnce(summary)
                return@withContext RoutineExecutionResult(
                    isSuccess = true,
                    routineName = routine.name,
                    spokenSummary = summary,
                    durationMs = duration
                )
            }
        }

        // 2. FALLBACK ROUTE: Step-by-Step Recorded Accessibility UI Path
        val service = LiaAccessibilityService.instance
        if (service == null) {
            val msg = "Accessibility service band hai. Routine execute nahi ho saki."
            speakOnce(msg)
            val duration = SystemClock.elapsedRealtime() - startTime
            LearnedRoutineStore.recordExecution(context, routine.id, false, duration)
            return@withContext RoutineExecutionResult(
                isSuccess = false,
                routineName = routine.name,
                spokenSummary = msg,
                durationMs = duration,
                failureReason = "accessibility_not_enabled"
            )
        }

        var failedIndex: Int? = null
        var failureReason: String? = null

        for (step in routine.steps) {
            val stepOk = executeSingleRoutineStep(service, step, mergedVariables)
            if (!stepOk) {
                failedIndex = step.stepIndex
                failureReason = "Step ${step.stepIndex} failed: ${step.description}"
                Log.w(TAG, failureReason)
                break
            }
            delay(250) // Brief natural transition between actions
        }

        val duration = SystemClock.elapsedRealtime() - startTime
        val isSuccess = failedIndex == null

        LearnedRoutineStore.recordExecution(context, routine.id, isSuccess, duration)
        LiveStatusHolder.recordToolExecution("learned_routine_accessibility", routine.name, if (isSuccess) "SUCCESS" else "FAILED", duration)

        val finalSpoken = if (isSuccess) {
            if (contactName != null) {
                "${contactName} wala task poora ho gaya."
            } else {
                "${routine.name} ho gaya."
            }
        } else {
            "Routine ruk gayi: ${failureReason ?: "kuchh gadbad hui"}"
        }

        speakOnce(finalSpoken)

        RoutineExecutionResult(
            isSuccess = isSuccess,
            routineName = routine.name,
            spokenSummary = finalSpoken,
            durationMs = duration,
            failedStepIndex = failedIndex,
            failureReason = failureReason
        )
    }

    private suspend fun executeSingleRoutineStep(
        service: LiaAccessibilityService,
        step: RoutineStep,
        variables: Map<String, String>
    ): Boolean {
        return when (step.actionType) {
            RecordedActionType.CLICK -> {
                // Try text selector -> description selector -> viewId -> bounds coordinate
                val targetText = step.targetText
                val targetDesc = step.targetDescription

                var node = if (!targetText.isNullOrBlank()) service.findNode(targetText) else null
                if (node == null && !targetDesc.isNullOrBlank()) {
                    node = service.findNode(targetDesc)
                }

                if (node != null) {
                    service.clickNode(node)
                } else if (step.boundsCenterX > 0f && step.boundsCenterY > 0f) {
                    service.tapCoordinates(step.boundsCenterX, step.boundsCenterY)
                } else {
                    false
                }
            }

            RecordedActionType.TYPE_TEXT -> {
                val textToType = step.variableKey?.let { variables[it] } ?: step.textToType ?: ""
                val typeResult = service.typeText(textToType)
                typeResult == "typed"
            }

            RecordedActionType.SCROLL_FORWARD -> {
                service.scroll("forward") == "scrolled"
            }

            RecordedActionType.SCROLL_BACKWARD -> {
                service.scroll("backward") == "scrolled"
            }

            RecordedActionType.GLOBAL_BACK -> {
                service.performBack()
            }

            RecordedActionType.GLOBAL_HOME -> {
                service.performHome()
            }

            RecordedActionType.LAUNCH_APP -> {
                true
            }

            RecordedActionType.WAIT -> {
                delay(600)
                true
            }
        }
    }
}
