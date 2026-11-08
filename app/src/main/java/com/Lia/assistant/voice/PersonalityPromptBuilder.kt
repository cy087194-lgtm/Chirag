package com.Lia.assistant.voice

import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality

object PersonalityPromptBuilder {

    fun build(
        personality: Personality,
        language: LanguagePreference,
        detectedLanguage: String?,
        recap: String?,
        assistantName: String
    ): String {
        val sections = mutableListOf<String>()

        // (a) Base instruction
        sections.add(
            "You are $assistantName, a persistent personal voice assistant living on the user's phone. Keep replies short and conversational, like natural spoken conversation. Avoid long monologues, lists with bullet points, or formal essay structures unless specifically requested."
        )

        // (b) Capabilities from flavor
        sections.add(LiaCapabilityPrompts.toolInstruction(assistantName))

        // (c) Personality instruction
        sections.add(personality.systemInstruction)

        // (d) Language policy
        val langPolicy = when (language) {
            LanguagePreference.AUTO -> {
                val currentHint = if (!detectedLanguage.isNullOrBlank() && detectedLanguage != "UNKNOWN") {
                    "Currently detected spoken language: $detectedLanguage."
                } else {
                    ""
                }
                "Language Policy: Match the language of the user turn by turn (Hindi, Hinglish, or English). Never translate what the user said back to them; simply respond naturally in the exact same language they spoke. $currentHint"
            }
            LanguagePreference.HINDI -> {
                "Language Policy: Always respond in natural, fluent Hindi (using either conversational Devanagari or intuitive transliteration suited for spoken audio)."
            }
            LanguagePreference.HINGLISH -> {
                "Language Policy: Always respond in conversational Hinglish (a natural, friendly Indian blend of Hindi and English words commonly used in everyday speech)."
            }
            LanguagePreference.ENGLISH -> {
                "Language Policy: Always respond in clear, natural, and expressive English."
            }
        }
        sections.add(langPolicy.trim())

        // (e) Safety guidelines
        sections.add(
            "Safety & Interaction Guidelines:\n" +
            "1. Never claim to be human or possess a physical human body or senses.\n" +
            "2. Strictly refuse any sexually explicit, harmful, hateful, or dangerous content.\n" +
            "3. Immediately and gracefully respect any request by the user to change tone, stop, or change topic.\n" +
            "4. Vary your conversational phrasing and acknowledge context warmly without repeating robotic formulaic greetings."
        )

        // (f) Recap if present
        if (!recap.isNullOrBlank()) {
            sections.add("This is a continuing conversation — do not greet again.\nRecap of recent turns:\n$recap")
        }

        return sections.joinToString("\n\n")
    }
}
