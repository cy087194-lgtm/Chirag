package com.Lia.assistant.forge

import org.json.JSONObject

object SseParser {

    fun extractText(dataLine: String): String? {
        val trimmed = dataLine.trim()
        if (trimmed.isEmpty() || trimmed == "[DONE]") return null

        return try {
            val json = JSONObject(trimmed)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val text = part.optString("text", "")
                if (text.isNotEmpty()) {
                    sb.append(text)
                }
            }
            if (sb.isNotEmpty()) sb.toString() else null
        } catch (_: Exception) {
            null
        }
    }

    fun extractFinishReason(dataLine: String): String? {
        val trimmed = dataLine.trim()
        if (trimmed.isEmpty() || trimmed == "[DONE]") return null

        return try {
            val json = JSONObject(trimmed)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val candidate = candidates.getJSONObject(0)
            candidate.optString("finishReason", null)
        } catch (_: Exception) {
            null
        }
    }

    fun extractError(dataLine: String): String? {
        val trimmed = dataLine.trim()
        if (trimmed.isEmpty() || trimmed == "[DONE]") return null

        return try {
            val json = JSONObject(trimmed)
            val error = json.optJSONObject("error") ?: return null
            error.optString("message", "API Error")
        } catch (_: Exception) {
            null
        }
    }
}
