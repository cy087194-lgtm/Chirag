package com.Lia.assistant.agent.demonstration

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class RecordedActionType {
    CLICK,
    TYPE_TEXT,
    SCROLL_FORWARD,
    SCROLL_BACKWARD,
    GLOBAL_BACK,
    GLOBAL_HOME,
    LAUNCH_APP,
    WAIT
}

data class RecordedAction(
    val type: RecordedActionType,
    val packageName: String,
    val text: String? = null,
    val contentDescription: String? = null,
    val viewId: String? = null,
    val boundsCenterX: Float = 0f,
    val boundsCenterY: Float = 0f,
    val inputText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isPassword: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type.name)
        put("packageName", packageName)
        put("text", text ?: "")
        put("contentDescription", contentDescription ?: "")
        put("viewId", viewId ?: "")
        put("boundsCenterX", boundsCenterX.toDouble())
        put("boundsCenterY", boundsCenterY.toDouble())
        put("inputText", inputText ?: "")
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(obj: JSONObject): RecordedAction = RecordedAction(
            type = RecordedActionType.valueOf(obj.optString("type", RecordedActionType.CLICK.name)),
            packageName = obj.optString("packageName", ""),
            text = obj.optString("text").ifEmpty { null },
            contentDescription = obj.optString("contentDescription").ifEmpty { null },
            viewId = obj.optString("viewId").ifEmpty { null },
            boundsCenterX = obj.optDouble("boundsCenterX", 0.0).toFloat(),
            boundsCenterY = obj.optDouble("boundsCenterY", 0.0).toFloat(),
            inputText = obj.optString("inputText").ifEmpty { null },
            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        )
    }
}

data class RoutineStep(
    val stepIndex: Int,
    val description: String,
    val actionType: RecordedActionType,
    val targetPackage: String,
    val targetText: String? = null,
    val targetDescription: String? = null,
    val targetViewId: String? = null,
    val boundsCenterX: Float = 0f,
    val boundsCenterY: Float = 0f,
    val textToType: String? = null,
    val directRouteScheme: String? = null,
    val variableKey: String? = null,
    val expectedResultNode: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("stepIndex", stepIndex)
        put("description", description)
        put("actionType", actionType.name)
        put("targetPackage", targetPackage)
        put("targetText", targetText ?: "")
        put("targetDescription", targetDescription ?: "")
        put("targetViewId", targetViewId ?: "")
        put("boundsCenterX", boundsCenterX.toDouble())
        put("boundsCenterY", boundsCenterY.toDouble())
        put("textToType", textToType ?: "")
        put("directRouteScheme", directRouteScheme ?: "")
        put("variableKey", variableKey ?: "")
        put("expectedResultNode", expectedResultNode ?: "")
    }

    companion object {
        fun fromJson(obj: JSONObject): RoutineStep = RoutineStep(
            stepIndex = obj.optInt("stepIndex", 1),
            description = obj.optString("description", ""),
            actionType = RecordedActionType.valueOf(obj.optString("actionType", RecordedActionType.CLICK.name)),
            targetPackage = obj.optString("targetPackage", ""),
            targetText = obj.optString("targetText").ifEmpty { null },
            targetDescription = obj.optString("targetDescription").ifEmpty { null },
            targetViewId = obj.optString("targetViewId").ifEmpty { null },
            boundsCenterX = obj.optDouble("boundsCenterX", 0.0).toFloat(),
            boundsCenterY = obj.optDouble("boundsCenterY", 0.0).toFloat(),
            textToType = obj.optString("textToType").ifEmpty { null },
            directRouteScheme = obj.optString("directRouteScheme").ifEmpty { null },
            variableKey = obj.optString("variableKey").ifEmpty { null },
            expectedResultNode = obj.optString("expectedResultNode").ifEmpty { null }
        )
    }
}

enum class TriggerType {
    VOICE_PHRASE,
    SCHEDULE,
    SYSTEM_EVENT,
    MANUAL
}

data class RoutineTrigger(
    val type: TriggerType,
    val phrase: String,
    val requiresWakeWord: Boolean = true,
    val scheduleTime: String? = null,
    val systemEventName: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type.name)
        put("phrase", phrase)
        put("requiresWakeWord", requiresWakeWord)
        put("scheduleTime", scheduleTime ?: "")
        put("systemEventName", systemEventName ?: "")
    }

    companion object {
        fun fromJson(obj: JSONObject): RoutineTrigger = RoutineTrigger(
            type = TriggerType.valueOf(obj.optString("type", TriggerType.VOICE_PHRASE.name)),
            phrase = obj.optString("phrase", ""),
            requiresWakeWord = obj.optBoolean("requiresWakeWord", true),
            scheduleTime = obj.optString("scheduleTime").ifEmpty { null },
            systemEventName = obj.optString("systemEventName").ifEmpty { null }
        )
    }
}

data class LearnedRoutine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trigger: RoutineTrigger,
    val directRouteAvailable: Boolean = false,
    val directRouteUri: String? = null,
    val steps: List<RoutineStep> = emptyList(),
    val variables: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunTime: Long = 0L,
    val lastRunDurationMs: Long = 0L,
    val lastRunStatus: String = "READY",
    val successCount: Int = 0,
    val failureCount: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("trigger", trigger.toJson())
        put("directRouteAvailable", directRouteAvailable)
        put("directRouteUri", directRouteUri ?: "")
        val stepsArray = JSONArray()
        steps.forEach { stepsArray.put(it.toJson()) }
        put("steps", stepsArray)
        val varsObj = JSONObject()
        variables.forEach { (k, v) -> varsObj.put(k, v) }
        put("variables", varsObj)
        put("isEnabled", isEnabled)
        put("createdAt", createdAt)
        put("lastRunTime", lastRunTime)
        put("lastRunDurationMs", lastRunDurationMs)
        put("lastRunStatus", lastRunStatus)
        put("successCount", successCount)
        put("failureCount", failureCount)
    }

    companion object {
        fun fromJson(obj: JSONObject): LearnedRoutine {
            val stepsList = mutableListOf<RoutineStep>()
            val stepsArr = obj.optJSONArray("steps")
            if (stepsArr != null) {
                for (i in 0 until stepsArr.length()) {
                    stepsList.add(RoutineStep.fromJson(stepsArr.getJSONObject(i)))
                }
            }

            val varsMap = mutableMapOf<String, String>()
            val varsObj = obj.optJSONObject("variables")
            if (varsObj != null) {
                varsObj.keys().forEach { k ->
                    varsMap[k] = varsObj.getString(k)
                }
            }

            return LearnedRoutine(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Learned Routine"),
                trigger = RoutineTrigger.fromJson(obj.getJSONObject("trigger")),
                directRouteAvailable = obj.optBoolean("directRouteAvailable", false),
                directRouteUri = obj.optString("directRouteUri").ifEmpty { null },
                steps = stepsList,
                variables = varsMap,
                isEnabled = obj.optBoolean("isEnabled", true),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                lastRunTime = obj.optLong("lastRunTime", 0L),
                lastRunDurationMs = obj.optLong("lastRunDurationMs", 0L),
                lastRunStatus = obj.optString("lastRunStatus", "READY"),
                successCount = obj.optInt("successCount", 0),
                failureCount = obj.optInt("failureCount", 0)
            )
        }
    }
}
