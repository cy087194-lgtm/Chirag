package com.Lia.assistant.agent

import com.Lia.assistant.action.FastActionEngine
import com.Lia.assistant.action.UniversalContactIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalFastActionTest {

    @Test
    fun testSpellingAndTransliterationNormalization() {
        assertEquals("keshav", UniversalContactIndex.normalizeSpelling("केशव"))
        assertEquals("keshav", UniversalContactIndex.normalizeSpelling("Keshaw"))
        assertEquals("keshav", UniversalContactIndex.normalizeSpelling("Kesav"))
        assertEquals("rahul", UniversalContactIndex.normalizeSpelling("राहुल"))
        assertEquals("aman", UniversalContactIndex.normalizeSpelling("अमन"))
        assertEquals("keshav", UniversalContactIndex.normalizeSpelling("Keshav Bhaiya"))
        assertEquals("keshav", UniversalContactIndex.normalizeSpelling("Keshav Ji"))
    }

    @Test
    fun testInternationalNumberFormatting() {
        val formatted = UniversalContactIndex.formatInternational("9876543210", "91")
        assertEquals("919876543210", formatted)

        val alreadyWithPlus = UniversalContactIndex.formatInternational("+919876543210", "91")
        assertEquals("919876543210", alreadyWithPlus)

        val withZero = UniversalContactIndex.formatInternational("09876543210", "91")
        assertEquals("919876543210", withZero)
    }

    @Test
    fun testUniversalMessagingIntentParsing() {
        // WhatsApp with no explicit app name
        val intent1 = SmartIntentParser.parse("Keshav ko hay bhej do")
        assertTrue(intent1 is ParsedIntent.WhatsAppMultiStep)
        val wa1 = intent1 as ParsedIntent.WhatsAppMultiStep
        assertEquals("Keshav", wa1.contactName)
        assertEquals("hay", wa1.messageText)

        // WhatsApp with explicit app name and quotes
        val intent2 = SmartIntentParser.parse("Keshav ko WhatsApp par 'kal milte hain' bhejo")
        assertTrue(intent2 is ParsedIntent.WhatsAppMultiStep)
        val wa2 = intent2 as ParsedIntent.WhatsAppMultiStep
        assertEquals("Keshav", wa2.contactName)
        assertEquals("kal milte hain", wa2.messageText)

        // SMS command
        val intent3 = SmartIntentParser.parse("Rahul ko SMS bhejo hello")
        assertTrue(intent3 is ParsedIntent.UniversalMessageCommand)
        val sms = intent3 as ParsedIntent.UniversalMessageCommand
        assertEquals("Rahul", sms.contactName)
        assertEquals("hello", sms.messageText)
        assertEquals("sms", sms.app)

        // Call command
        val intent4 = SmartIntentParser.parse("Mummy ko call karo")
        assertTrue(intent4 is ParsedIntent.DirectCallCommand)
        val call = intent4 as ParsedIntent.DirectCallCommand
        assertEquals("mummy", call.contactName.lowercase())
    }

    @Test
    fun testDisambiguationRule() {
        val names = listOf("Keshav Sharma", "Keshav Kumar")
        val question = "${names[0]} ya ${names[1]}?"
        assertEquals("Keshav Sharma ya Keshav Kumar?", question)
    }
}
