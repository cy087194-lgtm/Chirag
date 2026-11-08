package com.Lia.assistant.agent.gaming

import android.content.Context
import com.Lia.assistant.accessibility.AccessibleNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

object GameCoachEngine {
    private const val PREF_NAME = "game_coach_prefs"
    private const val KEY_PROACTIVE_STUDY = "proactive_study_hint"

    val isActive: StateFlow<Boolean>
        get() = kotlinx.coroutines.flow.MutableStateFlow(
            AllRounderCoachEngine.coachState.value == CoachState.MONITORING
        )

    val lastAlert: StateFlow<String?>
        get() = kotlinx.coroutines.flow.MutableStateFlow(
            AllRounderCoachEngine.latestAlert.value?.text
        )

    // Tracking for proactive study watcher
    private var lastSeenScreenHash: Int = 0
    private var sameScreenDurationMs: Long = 0L

    fun getFrequency(context: Context): CoachFrequency {
        return CoachPreferences.getFrequency(context)
    }

    fun setFrequency(context: Context, frequency: CoachFrequency) {
        CoachPreferences.setFrequency(context, frequency)
    }

    fun isProactiveStudyEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PROACTIVE_STUDY, true)
    }

    fun setProactiveStudyEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PROACTIVE_STUDY, enabled)
            .apply()
    }

    /**
     * T1: Starts real-time screen coach via AllRounderCoachEngine
     */
    fun startCoach(context: Context, scope: CoroutineScope): String {
        return AllRounderCoachEngine.startCoaching(context, scope)
    }

    /**
     * T1: "chup / bas karo / game coach off" stops it instantly.
     */
    fun stopCoach(): String {
        return AllRounderCoachEngine.stopCoaching()
    }

    fun getPostMatchReview(): String {
        return "Match Review: Booyah ke liye 2 important improvements: 1) Open ground mein rush karne se pehle gloo wall deploy karo. 2) Zone countdown ke 20s rehte hi cover-to-cover rotate karo. Well played!"
    }

    fun getGameGuide(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("sensitivity") -> "Free Fire Best Sensitivity: General: 95-100, Red Dot: 90, 2x Scope: 85, 4x Scope: 75, Sniper Scope: 55, Free Look: 65. Smooth headshots ke liye perfect starting point hai. Training ground mein test karo."
            q.contains("landing") || q.contains("map") -> "Bermuda Landing Tips: Safe loot ke liye Cape Town ya Rim Nam Village utro. Aggressive gameplay aur quick kills ke liye Clock Tower ya Peak landing best hai."
            q.contains("gun") || q.contains("weapon") || q.contains("loadout") -> "Best Combo: Close-range mein MP40 ya M1887, aur mid-to-long range mein Woodpecker ya M4A1. Sath mein Chrono ya Alok character skill use karo."
            else -> "Game Guide: Level clear karne ke liye pehle surroundings ko observe karo, cover to cover move karo, aur resources save karke final circle mein aggressive bano."
        }
    }

    /**
     * Proactive Study & Work Watcher
     */
    fun checkStudyScreenProgress(nodes: List<AccessibleNode>): String? {
        val currentHash = nodes.map { it.text }.hashCode()
        if (currentHash == lastSeenScreenHash) {
            sameScreenDurationMs += 2000L
            if (sameScreenDurationMs >= 10 * 60 * 1000L) { // 10 minutes
                sameScreenDurationMs = 0L
                return "Tum 10 minute se ek hi sawal par atke ho, kya main hint doon?"
            }
        } else {
            lastSeenScreenHash = currentHash
            sameScreenDurationMs = 0L
        }
        return null
    }
}
