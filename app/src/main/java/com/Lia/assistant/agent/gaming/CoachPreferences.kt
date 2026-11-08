package com.Lia.assistant.agent.gaming

import android.content.Context
import android.content.SharedPreferences

enum class CoachFrequency(val label: String, val minUrgency: Int) {
    KAM("Kam", 3),       // Critical only
    NORMAL("Normal", 2), // Tactical + Critical
    ZYADA("Zyada", 1)    // All helpful cues
}

enum class StopSensitivity {
    NORMAL,
    HIGH
}

object CoachPreferences {
    private const val PREFS_NAME = "coach_settings_prefs"

    private const val KEY_FREQUENCY = "coach_frequency"
    private const val KEY_LOBBY_DELAY_SEC = "coach_lobby_delay_sec"
    private const val KEY_GAME_PACKAGES = "coach_game_packages"
    private const val KEY_STOP_WORDS = "coach_stop_words"
    private const val KEY_STOP_SENSITIVITY = "coach_stop_sensitivity"
    private const val KEY_AUTO_RESUME_GAME = "coach_auto_resume_game"
    private const val KEY_COACH_ACTIVE = "coach_persisted_active"

    val DEFAULT_GAME_PACKAGES = setOf(
        "com.dts.freefireth",
        "com.dts.freefiremax"
    )

    val DEFAULT_STOP_WORDS = listOf(
        "stop",
        "ruk",
        "ruko",
        "ruk jao",
        "bas",
        "bas karo",
        "chup",
        "chup ho ja",
        "band karo",
        "coach off",
        "watch off",
        "cancel",
        "enough",
        "shut up"
    )

    const val DISCLAIMER_TEXT =
        "Vision processing has a 1 to 3 seconds delay. Advice about direction and zone is reliable, but split-second fights are not. This is pure strategic coaching: NO auto-play, NO aim assist, NO cheating. Some competitive games may treat screen overlays as a rule violation; use at your own discretion."

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getFrequency(context: Context): CoachFrequency {
        val name = getPrefs(context).getString(KEY_FREQUENCY, CoachFrequency.NORMAL.name)
        return try {
            CoachFrequency.valueOf(name ?: CoachFrequency.NORMAL.name)
        } catch (_: Exception) {
            CoachFrequency.NORMAL
        }
    }

    fun setFrequency(context: Context, frequency: CoachFrequency) {
        getPrefs(context).edit().putString(KEY_FREQUENCY, frequency.name).apply()
    }

    fun getLobbyDelaySeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_LOBBY_DELAY_SEC, 5)
    }

    fun setLobbyDelaySeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_LOBBY_DELAY_SEC, seconds).apply()
    }

    fun getGamePackages(context: Context): Set<String> {
        val saved = getPrefs(context).getStringSet(KEY_GAME_PACKAGES, null)
        return saved ?: DEFAULT_GAME_PACKAGES
    }

    fun addGamePackage(context: Context, packageName: String) {
        val current = getGamePackages(context).toMutableSet()
        current.add(packageName.trim().lowercase())
        getPrefs(context).edit().putStringSet(KEY_GAME_PACKAGES, current).apply()
    }

    fun removeGamePackage(context: Context, packageName: String) {
        val current = getGamePackages(context).toMutableSet()
        current.remove(packageName.trim().lowercase())
        getPrefs(context).edit().putStringSet(KEY_GAME_PACKAGES, current).apply()
    }

    fun getStopWords(context: Context): List<String> {
        val saved = getPrefs(context).getString(KEY_STOP_WORDS, null)
        return if (!saved.isNullOrBlank()) {
            saved.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        } else {
            DEFAULT_STOP_WORDS
        }
    }

    fun setStopWords(context: Context, words: List<String>) {
        val joined = words.joinToString(",") { it.trim().lowercase() }
        getPrefs(context).edit().putString(KEY_STOP_WORDS, joined).apply()
    }

    fun getStopSensitivity(context: Context): StopSensitivity {
        val name = getPrefs(context).getString(KEY_STOP_SENSITIVITY, StopSensitivity.NORMAL.name)
        return try {
            StopSensitivity.valueOf(name ?: StopSensitivity.NORMAL.name)
        } catch (_: Exception) {
            StopSensitivity.NORMAL
        }
    }

    fun setStopSensitivity(context: Context, sensitivity: StopSensitivity) {
        getPrefs(context).edit().putString(KEY_STOP_SENSITIVITY, sensitivity.name).apply()
    }

    fun isAutoResumeOnGameOpen(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_RESUME_GAME, false)
    }

    fun setAutoResumeOnGameOpen(context: Context, enable: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_RESUME_GAME, enable).apply()
    }

    fun isCoachPersistedActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_COACH_ACTIVE, false)
    }

    fun setCoachPersistedActive(context: Context, active: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_COACH_ACTIVE, active).apply()
    }
}
