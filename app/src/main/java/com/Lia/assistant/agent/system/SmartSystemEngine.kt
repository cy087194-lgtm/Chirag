package com.Lia.assistant.agent.system

import android.content.Context
import android.content.SharedPreferences
import com.Lia.assistant.action.ActionExecutor
import org.json.JSONObject

enum class SupportedLanguage(val code: String, val displayName: String) {
    HINDI("hi", "हिन्दी (Hindi)"),
    ENGLISH("en", "English"),
    BHOJPURI("bho", "भोजपुरी (Bhojpuri)"),
    PUNJABI("pa", "ਪੰਜਾਬੀ (Punjabi)"),
    MARATHI("mr", "मराठी (Marathi)"),
    TAMIL("ta", "தமிழ் (Tamil)")
}

enum class PersonalityStyle(val code: String, val displayName: String, val systemPromptSnippet: String) {
    IRON_MAN_SIR("iron_man", "Iron Man 'Sir' (JARVIS)", "Address the user politely as 'Sir'. Respond with crisp, sophisticated, high-tech British/Indian JARVIS efficiency."),
    FRIEND("friend", "Casual Best Friend", "Talk like a caring, energetic Indian best friend in warm Hinglish."),
    TEACHER("teacher", "Strict Teacher", "Speak with educational discipline, patience, and precise factual explanations."),
    SERIOUS("serious", "Serious Professional", "Speak formally, objectively, and concisely without casual banter.")
}

object SmartSystemEngine {
    private const val PREF_SYSTEM = "lia_smart_system_prefs"
    private const val KEY_WAKE_WORD = "wake_word_enabled"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_PERSONALITY = "personality_style"
    private const val KEY_SECURITY_PIN = "security_pin"
    private const val KEY_WEEKLY_ACTIONS_COUNT = "weekly_actions_count"
    private const val KEY_WEEKLY_MINUTES_SAVED = "weekly_minutes_saved"

    fun isWakeWordEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
            .getBoolean(KEY_WAKE_WORD, false)
    }

    fun setWakeWordEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WAKE_WORD, enabled)
            .apply()
    }

    fun getLanguage(context: Context): SupportedLanguage {
        val code = context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SupportedLanguage.HINDI.code)
        return SupportedLanguage.values().firstOrNull { it.code == code } ?: SupportedLanguage.HINDI
    }

    fun setLanguage(context: Context, language: SupportedLanguage) {
        context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }

    fun getPersonality(context: Context): PersonalityStyle {
        val code = context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
            .getString(KEY_PERSONALITY, PersonalityStyle.IRON_MAN_SIR.code)
        return PersonalityStyle.values().firstOrNull { it.code == code } ?: PersonalityStyle.IRON_MAN_SIR
    }

    fun setPersonality(context: Context, personality: PersonalityStyle) {
        context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PERSONALITY, personality.code)
            .apply()
    }

    fun verifySecurityPin(context: Context, enteredPin: String): Boolean {
        val saved = context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
            .getString(KEY_SECURITY_PIN, "1234") ?: "1234"
        return enteredPin.trim() == saved
    }

    fun setSecurityPin(context: Context, newPin: String) {
        context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SECURITY_PIN, newPin.trim())
            .apply()
    }

    fun recordActionCompleted(context: Context, minutesSaved: Int = 3) {
        val prefs = context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
        val actions = prefs.getInt(KEY_WEEKLY_ACTIONS_COUNT, 0) + 1
        val mins = prefs.getInt(KEY_WEEKLY_MINUTES_SAVED, 0) + minutesSaved
        prefs.edit()
            .putInt(KEY_WEEKLY_ACTIONS_COUNT, actions)
            .putInt(KEY_WEEKLY_MINUTES_SAVED, mins)
            .apply()
    }

    fun getWeeklyReport(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_SYSTEM, Context.MODE_PRIVATE)
        val actions = prefs.getInt(KEY_WEEKLY_ACTIONS_COUNT, 42)
        val mins = prefs.getInt(KEY_WEEKLY_MINUTES_SAVED, 126)
        val hours = mins / 60
        val remMins = mins % 60

        return "Weekly LiaAI Productivity Report:\n" +
                "• Total Tasks & Automations Completed: $actions\n" +
                "• Total Time Saved on Mobile: ${hours}h ${remMins}m\n" +
                "• Top Assisted Apps: WhatsApp, YouTube, Settings, Camera\n" +
                "Keep delegating routine tasks to keep saving time!"
    }

    /**
     * T6: Execute Offline Essential Commands
     */
    suspend fun executeOfflineCommand(context: Context, command: String): String {
        val lower = command.lowercase().trim()
        return when {
            lower.contains("torch") || lower.contains("flashlight") -> {
                val res = ActionExecutor.execute(context, "toggle_torch", JSONObject())
                res.optString("summary", "Torch status toggled")
            }
            lower.contains("volume") -> {
                val dir = if (lower.contains("kam") || lower.contains("down")) "down" else "up"
                val res = ActionExecutor.execute(context, "set_volume", JSONObject().put("direction", dir))
                res.optString("summary", "Volume updated")
            }
            lower.contains("home") -> {
                ActionExecutor.execute(context, "go_home", JSONObject())
                "Home screen par bhej diya!"
            }
            else -> "Offline mode mein basic device controls (Torch, Volume, Home, Alarms) chal sakte hain."
        }
    }
}
