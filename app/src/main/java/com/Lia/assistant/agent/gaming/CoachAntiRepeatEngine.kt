package com.Lia.assistant.agent.gaming

data class SpokenEntry(
    val text: String,
    val type: String,
    val urgency: Int,
    val timestamp: Long
)

object CoachAntiRepeatEngine {

    private val COOLDOWNS_MS = mapOf(
        "enemy_near" to 4000L,
        "low_health" to 15000L,
        "zone_shrinking" to 20000L,
        "loot_nearby" to 20000L,
        "reload" to 12000L,
        "low_ammo" to 12000L,
        "vehicle" to 12000L,
        "knocked" to 10000L,
        "squad_down" to 10000L
    )
    private const val DEFAULT_COOLDOWN_MS = 12000L
    private const val MAX_STALE_AGE_MS = 3000L

    private val spokenHistory = mutableListOf<SpokenEntry>()
    private var lastSpokenSentence: String = ""

    @Synchronized
    fun evaluateEvent(
        event: TacticalEvent,
        frequency: CoachFrequency,
        eventCreatedAt: Long = System.currentTimeMillis()
    ): EvaluationDecision {
        val now = System.currentTimeMillis()

        // 1. Stale event check: drop tips older than 3 seconds
        if (now - eventCreatedAt > MAX_STALE_AGE_MS) {
            return EvaluationDecision.Rejected("Event is stale (> 3s old)")
        }

        // 2. Urgency check against user setting (Kam / Normal / Zyada)
        if (event.urgency < frequency.minUrgency) {
            return EvaluationDecision.Rejected("Urgency ${event.urgency} < setting minimum ${frequency.minUrgency} (${frequency.label})")
        }

        // 3. Duplicate consecutive sentence check: Never say the same sentence twice in a row
        val cleanSay = event.say.trim()
        if (cleanSay.equals(lastSpokenSentence, ignoreCase = true)) {
            return EvaluationDecision.Rejected("Exact same sentence as previous tip")
        }

        // 4. Per-event type cooldown check
        val cooldown = COOLDOWNS_MS[event.type] ?: DEFAULT_COOLDOWN_MS
        val lastSameType = spokenHistory.lastOrNull { it.type == event.type }
        if (lastSameType != null) {
            val elapsed = now - lastSameType.timestamp
            if (elapsed < cooldown) {
                // If situation became strictly more urgent (e.g. was 2, now 3), allow override
                if (event.urgency <= lastSameType.urgency) {
                    val remaining = (cooldown - elapsed) / 1000.0
                    return EvaluationDecision.Rejected("Cooldown active for '${event.type}' (%.1fs remaining)".format(remaining))
                }
            }
        }

        // 5. Ensure brevity (under 8 words for in-match combat)
        val words = cleanSay.split("\\s+".toRegex())
        val formattedSay = if (words.size > 8) {
            words.take(8).joinToString(" ")
        } else {
            cleanSay
        }

        return EvaluationDecision.Approved(formattedSay)
    }

    @Synchronized
    fun recordSpoken(say: String, type: String, urgency: Int) {
        val now = System.currentTimeMillis()
        lastSpokenSentence = say.trim()
        spokenHistory.add(SpokenEntry(say.trim(), type, urgency, now))
        if (spokenHistory.size > 20) {
            spokenHistory.removeAt(0)
        }
    }

    @Synchronized
    fun getRecentSpokenTips(): List<String> {
        return spokenHistory.takeLast(5).map { it.text }
    }

    @Synchronized
    fun clear() {
        spokenHistory.clear()
        lastSpokenSentence = ""
    }
}

sealed class EvaluationDecision {
    data class Approved(val tipToSpeak: String) : EvaluationDecision()
    data class Rejected(val reason: String) : EvaluationDecision()
}
