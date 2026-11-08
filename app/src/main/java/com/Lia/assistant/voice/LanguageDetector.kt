package com.Lia.assistant.voice

object LanguageDetector {

    private val HINGLISH_MARKERS = setOf(
        "hai", "hain", "ho", "kya", "kyun", "kaise", "nahi", "haan",
        "mujhe", "tumhe", "aap", "mera", "meri", "mere", "yaar", "bhai",
        "accha", "theek", "matlab", "karna", "karo", "raha", "rahi", "rahe",
        "chahiye", "batao", "suno", "dekho", "abhi", "phir", "lekin", "bahut",
        "thoda", "kuch", "sab", "wala", "wali", "wale", "hoga", "hogi", "hoge",
        "bolo", "samjha", "samjhe", "arre", "namaste", "dost", "kaun", "kab",
        "kahan", "bhi", "toh", "aur", "pe", "se", "ko", "ne", "ka", "ki", "ke"
    )

    fun detect(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "UNKNOWN"

        // Check for Devanagari script (U+0900..U+097F)
        for (ch in trimmed) {
            if (ch in '\u0900'..'\u097F') {
                return "HINDI"
            }
        }

        // Tokenize Latin text
        val words = trimmed.lowercase()
            .split(Regex("[^a-zA-Z']+"))
            .filter { it.isNotBlank() }

        if (words.isEmpty()) return "UNKNOWN"

        val hits = words.count { HINGLISH_MARKERS.contains(it) }
        val wordCount = words.size
        val hitRatio = hits.toDouble() / wordCount.toDouble()

        return if (wordCount <= 4) {
            if (hits >= 1) "HINGLISH" else "ENGLISH"
        } else {
            if (hits >= 2 || hitRatio >= 0.15) "HINGLISH" else "ENGLISH"
        }
    }
}
