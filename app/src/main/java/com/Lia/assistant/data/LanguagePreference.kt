package com.Lia.assistant.data

enum class LanguagePreference(val displayName: String, val description: String) {
    AUTO("Auto-detect", "Follows whatever you speak (Hindi, Hinglish or English)"),
    HINDI("Hindi", "Always responds in natural Hindi"),
    HINGLISH("Hinglish", "Conversational blend of Hindi and English"),
    ENGLISH("English", "Clear and natural English");

    companion object {
        fun fromString(value: String): LanguagePreference {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AUTO
        }
    }
}
