package com.Lia.assistant.agent.gaming

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

enum class CoachState {
    IDLE,
    MONITORING,
    PAUSED
}

data class TacticalAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val priority: AlertPriority,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertPriority {
    CRITICAL,
    TACTICAL,
    INFO
}

/**
 * FreeFireGameCoach: Main facade for gaming coach.
 * Fully backed by AllRounderCoachEngine with fresh-frame vision, on-device HUD checks,
 * anti-repeat enforcement, and instant barge-in voice stop.
 * All fake/hardcoded timers have been completely removed.
 */
object FreeFireGameCoach {

    val coachState: StateFlow<CoachState>
        get() = AllRounderCoachEngine.coachState

    val latestAlert: StateFlow<TacticalAlert?>
        get() = AllRounderCoachEngine.latestAlert

    fun initTts(context: Context) {
        AllRounderCoachEngine.initTts(context)
    }

    fun startCoaching(context: Context, scope: CoroutineScope): String {
        return AllRounderCoachEngine.startCoaching(context, scope)
    }

    fun pauseCoaching(): String {
        return AllRounderCoachEngine.pauseCoaching()
    }

    fun resumeCoaching(): String {
        return AllRounderCoachEngine.resumeCoaching()
    }

    fun stopCoaching(): String {
        return AllRounderCoachEngine.stopCoaching()
    }

    fun toggleMute(): Boolean {
        return AllRounderCoachEngine.toggleMute()
    }
}
