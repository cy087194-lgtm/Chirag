package com.Lia.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectorTest {

    @Test
    fun testBlankAndEmpty() {
        assertEquals("UNKNOWN", LanguageDetector.detect(""))
        assertEquals("UNKNOWN", LanguageDetector.detect("   "))
    }

    @Test
    fun testDevanagariHindi() {
        assertEquals("HINDI", LanguageDetector.detect("नमस्ते, आप कैसे हैं?"))
        assertEquals("HINDI", LanguageDetector.detect("क्या हाल है"))
    }

    @Test
    fun testHinglishShortAndLong() {
        assertEquals("HINGLISH", LanguageDetector.detect("kya haal hai yaar"))
        assertEquals("HINGLISH", LanguageDetector.detect("bhai suno"))
        assertEquals("HINGLISH", LanguageDetector.detect("aaj mausam kaisa hai aur baarish hogi kya"))
    }

    @Test
    fun testEnglish() {
        assertEquals("ENGLISH", LanguageDetector.detect("what's the weather"))
        assertEquals("ENGLISH", LanguageDetector.detect("Can you open YouTube for me please?"))
    }
}
