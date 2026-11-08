package com.Lia.assistant.agent.demonstration

import android.content.Context
import android.util.Log
import com.Lia.assistant.action.UniversalContactIndex
import com.Lia.assistant.action.UniversalContactResult
import java.util.UUID

object RoutineCompiler {
    private const val TAG = "RoutineCompiler"

    /**
     * Compiles raw recorded accessibility actions into an optimized LearnedRoutine.
     */
    fun compile(
        context: Context? = null,
        rawActions: List<RecordedAction>,
        customName: String? = null,
        customTriggerPhrase: String? = null
    ): LearnedRoutine {
        if (rawActions.isEmpty()) {
            throw IllegalArgumentException("No actions to compile")
        }

        val cleanedActions = cleanMistakesAndDeduplicate(rawActions)
        val primaryPackage = detectPrimaryPackage(cleanedActions)

        val variables = mutableMapOf<String, String>()
        val routineSteps = mutableListOf<RoutineStep>()

        var directRouteUri: String? = null
        var directRouteAvailable = false

        // Extract typed text and targets
        var lastTypedText: String? = null
        var contactCandidate: String? = null

        cleanedActions.forEachIndexed { idx, action ->
            when (action.type) {
                RecordedActionType.TYPE_TEXT -> {
                    action.inputText?.let {
                        lastTypedText = it
                        variables["messageText"] = it
                    }
                }
                RecordedActionType.CLICK -> {
                    // Check if clicked text resembles a contact name
                    val text = action.text ?: action.contentDescription
                    if (!text.isNullOrBlank() && text.length in 3..25 && !text.equals("send", ignoreCase = true)) {
                        contactCandidate = text
                        variables["contactName"] = text
                    }
                }
                else -> {}
            }

            routineSteps.add(
                RoutineStep(
                    stepIndex = idx + 1,
                    description = generateStepDescription(action),
                    actionType = action.type,
                    targetPackage = action.packageName,
                    targetText = action.text,
                    targetDescription = action.contentDescription,
                    targetViewId = action.viewId,
                    boundsCenterX = action.boundsCenterX,
                    boundsCenterY = action.boundsCenterY,
                    textToType = action.inputText,
                    variableKey = if (action.type == RecordedActionType.TYPE_TEXT) "messageText" else null,
                    expectedResultNode = if (action.type == RecordedActionType.CLICK && action.text?.equals("send", ignoreCase = true) == true) "message_sent" else null
                )
            )
        }

        // Direct Route Optimization (PART W / PART Z requirement)
        if (primaryPackage.contains("whatsapp") && contactCandidate != null && lastTypedText != null) {
            val contactLookup = if (context != null) UniversalContactIndex.findContact(context, contactCandidate!!) else null
            if (contactLookup is UniversalContactResult.SingleMatch) {
                directRouteAvailable = true
                directRouteUri = "https://wa.me/${contactLookup.formattedNumber}?text=${lastTypedText}"
            } else {
                directRouteAvailable = true
                directRouteUri = "https://wa.me/?text=${lastTypedText}"
            }
        } else if (primaryPackage.contains("messaging") || primaryPackage.contains("sms")) {
            if (contactCandidate != null && lastTypedText != null) {
                directRouteAvailable = true
                directRouteUri = "smsto:$contactCandidate"
            }
        }

        val triggerWord = customTriggerPhrase?.trim()?.ifEmpty { null }
            ?: contactCandidate
            ?: "routine ${cleanedActions.size}"

        val routineName = customName?.trim()?.ifEmpty { null }
            ?: if (contactCandidate != null && lastTypedText != null) {
                "Send \"$lastTypedText\" to $contactCandidate"
            } else {
                "Automated $primaryPackage routine"
            }

        val trigger = RoutineTrigger(
            type = TriggerType.VOICE_PHRASE,
            phrase = triggerWord,
            requiresWakeWord = true
        )

        return LearnedRoutine(
            id = UUID.randomUUID().toString(),
            name = routineName,
            trigger = trigger,
            directRouteAvailable = directRouteAvailable,
            directRouteUri = directRouteUri,
            steps = routineSteps,
            variables = variables,
            isEnabled = true,
            createdAt = System.currentTimeMillis()
        )
    }

    fun cleanMistakesAndDeduplicate(actions: List<RecordedAction>): List<RecordedAction> {
        val result = mutableListOf<RecordedAction>()
        actions.forEach { action ->
            val prev = result.lastOrNull()
            // Drop identical back-to-back clicks within 400ms
            if (prev != null && prev.type == action.type && prev.packageName == action.packageName) {
                if (action.type == RecordedActionType.CLICK &&
                    prev.text == action.text &&
                    prev.viewId == action.viewId &&
                    action.timestamp - prev.timestamp < 400
                ) {
                    return@forEach
                }
            }
            result.add(action)
        }
        return result
    }

    private fun detectPrimaryPackage(actions: List<RecordedAction>): String {
        return actions.map { it.packageName }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "com.whatsapp"
    }

    private fun generateStepDescription(action: RecordedAction): String {
        return when (action.type) {
            RecordedActionType.CLICK -> {
                val target = action.text ?: action.contentDescription ?: action.viewId ?: "button"
                "Tap \"$target\""
            }
            RecordedActionType.TYPE_TEXT -> "Type \"${action.inputText ?: ""}\""
            RecordedActionType.SCROLL_FORWARD -> "Scroll screen forward"
            RecordedActionType.SCROLL_BACKWARD -> "Scroll screen backward"
            RecordedActionType.GLOBAL_BACK -> "Press Back"
            RecordedActionType.GLOBAL_HOME -> "Go to Home"
            RecordedActionType.LAUNCH_APP -> "Open app (${action.packageName})"
            RecordedActionType.WAIT -> "Wait for screen update"
        }
    }
}
