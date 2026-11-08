package com.Lia.assistant.voice

import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityPromptBuilderTest {

    @Test
    fun testAllPersonalityAndLanguageCombinationsBuild() {
        for (personality in Personality.entries) {
            for (language in LanguagePreference.entries) {
                val prompt = PersonalityPromptBuilder.build(
                    personality = personality,
                    language = language,
                    detectedLanguage = "ENGLISH",
                    recap = null,
                    assistantName = "Lia"
                )
                assertNotNull(prompt)
                assertTrue("Prompt must contain assistant name", prompt.contains("Lia"))
                assertTrue("Prompt must contain personality instruction", prompt.contains(personality.systemInstruction))
                assertFalse("Prompt without recap must not contain continuing conversation notice", prompt.contains("This is a continuing conversation"))
            }
        }
    }

    @Test
    fun testRecapOnlyIncludedWhenPresent() {
        val noRecap = PersonalityPromptBuilder.build(
            personality = Personality.NORMAL,
            language = LanguagePreference.AUTO,
            detectedLanguage = null,
            recap = null,
            assistantName = "Lia"
        )
        assertFalse(noRecap.contains("Recap of recent turns"))

        val withRecap = PersonalityPromptBuilder.build(
            personality = Personality.NORMAL,
            language = LanguagePreference.AUTO,
            detectedLanguage = "HINGLISH",
            recap = "User: kya haal hai\nYou: badhiya!",
            assistantName = "Lia"
        )
        assertTrue(withRecap.contains("This is a continuing conversation"))
        assertTrue(withRecap.contains("Recap of recent turns"))
        assertTrue(withRecap.contains("User: kya haal hai"))
    }

    @Test
    fun testConversationMemoryRollingWindow() {
        ConversationMemory.clear()
        for (i in 1..20) {
            ConversationMemory.recordTurn(MemoryRole.USER, "Message $i")
            ConversationMemory.recordTurn(MemoryRole.ASSISTANT, "Reply $i")
        }
        val recap = ConversationMemory.recap()
        assertNotNull(recap)
        assertFalse("Old turns should be pruned", recap!!.contains("Message 1\n"))
        assertTrue("Recent turns must be present", recap.contains("Message 20"))
        ConversationMemory.clear()
    }
}
