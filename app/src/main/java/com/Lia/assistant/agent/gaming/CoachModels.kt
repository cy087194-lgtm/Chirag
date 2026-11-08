package com.Lia.assistant.agent.gaming

enum class CoachScreenMode(val label: String) {
    GAME_IN_MATCH("Game (In-Match)"),
    GAME_LOBBY("Game (Lobby / Menu)"),
    GAME_SETTINGS("Game (Settings / Sensitivity)"),
    GAME_OTHER("Game (Loadout / Shop / Results)"),
    BROWSER("Web Browser"),
    OTHER_APP("Other Application")
}

data class TacticalEvent(
    val type: String,
    val direction: String = "unknown",
    val urgency: Int = 2,
    val say: String
)

data class CoachAnalysisResult(
    val state: String,
    val events: List<TacticalEvent>,
    val rawJson: String
)

data class DecisionRecord(
    val timestamp: Long = System.currentTimeMillis(),
    val mode: CoachScreenMode,
    val foregroundPackage: String,
    val diffPercent: Float,
    val outcome: String,
    val tip: String?,
    val reason: String
)

data class StopDebugRecord(
    val lastHeard: String = "",
    val matchedWord: String? = null,
    val latencyMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
