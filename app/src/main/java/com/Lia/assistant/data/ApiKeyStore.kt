package com.Lia.assistant.data

import android.content.Context
import android.content.SharedPreferences
import com.Lia.assistant.BuildConfig

object ApiKeyStore {
    private const val PREF_NAME = "nova_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_OPENAI_API_KEY = "openai_api_key"
    private const val KEY_CLAUDE_API_KEY = "claude_api_key"
    private const val KEY_SEARCH_API_KEY = "search_api_key"
    private const val KEY_ROUTING_PREFERENCE = "model_routing_mode" // "AUTO", "GEMINI", "OPENAI", "CLAUDE"
    private const val KEY_DAILY_COST_LIMIT = "daily_cost_limit" // in USD (e.g. 2.00)
    private const val KEY_DAILY_TOKENS_DATE = "daily_tokens_date"
    private const val KEY_DAILY_TOKENS_COUNT = "daily_tokens_count"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Gemini
    fun getKey(context: Context): String = getGeminiKey(context)
    fun saveKey(context: Context, key: String) = saveGeminiKey(context, key)
    fun hasKey(context: Context): Boolean = getGeminiKey(context).isNotEmpty()

    fun getGeminiKey(context: Context): String {
        val saved = getPrefs(context).getString(KEY_GEMINI_API_KEY, "")?.trim() ?: ""
        if (saved.isNotEmpty()) return saved
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "your_api_key_here") buildKey.trim() else ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun saveGeminiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    // OpenAI (GPT)
    fun getOpenAiKey(context: Context): String {
        return getPrefs(context).getString(KEY_OPENAI_API_KEY, "")?.trim() ?: ""
    }

    fun saveOpenAiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_OPENAI_API_KEY, key.trim()).apply()
    }

    // Claude (Anthropic)
    fun getClaudeKey(context: Context): String {
        return getPrefs(context).getString(KEY_CLAUDE_API_KEY, "")?.trim() ?: ""
    }

    fun saveClaudeKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_CLAUDE_API_KEY, key.trim()).apply()
    }

    // Search Key (Brave/Tavily/SerpAPI)
    fun getSearchApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_SEARCH_API_KEY, "")?.trim() ?: ""
    }

    fun saveSearchApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_SEARCH_API_KEY, key.trim()).apply()
    }

    // Model Routing Override: "AUTO", "GEMINI", "OPENAI", "CLAUDE"
    fun getModelRoutingPreference(context: Context): String {
        return getPrefs(context).getString(KEY_ROUTING_PREFERENCE, "AUTO") ?: "AUTO"
    }

    fun saveModelRoutingPreference(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_ROUTING_PREFERENCE, mode).apply()
    }

    // Daily Limit & Cost Control
    fun getDailyCostLimit(context: Context): Float {
        return getPrefs(context).getFloat(KEY_DAILY_COST_LIMIT, 5.0f)
    }

    fun saveDailyCostLimit(context: Context, limit: Float) {
        getPrefs(context).edit().putFloat(KEY_DAILY_COST_LIMIT, limit).apply()
    }

    fun getDailyTokensUsed(context: Context): Long {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val savedDate = getPrefs(context).getString(KEY_DAILY_TOKENS_DATE, "")
        return if (savedDate == today) {
            getPrefs(context).getLong(KEY_DAILY_TOKENS_COUNT, 0L)
        } else {
            0L
        }
    }

    @Synchronized
    fun addTokensUsed(context: Context, tokens: Int) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val prefs = getPrefs(context)
        val savedDate = prefs.getString(KEY_DAILY_TOKENS_DATE, "")
        val currentCount = if (savedDate == today) prefs.getLong(KEY_DAILY_TOKENS_COUNT, 0L) else 0L
        prefs.edit()
            .putString(KEY_DAILY_TOKENS_DATE, today)
            .putLong(KEY_DAILY_TOKENS_COUNT, currentCount + tokens)
            .apply()
    }
}
