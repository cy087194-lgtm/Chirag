package com.Lia.assistant.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartIntentParserTest {

    @Test
    fun testNegationDetection() {
        val intent1 = SmartIntentParser.parse("Wi-Fi band mat karna")
        assertTrue("Expected NegatedCommand", intent1 is ParsedIntent.NegatedCommand)

        val intent2 = SmartIntentParser.parse("Volume kam mat karo")
        assertTrue("Expected NegatedCommand", intent2 is ParsedIntent.NegatedCommand)

        val intent3 = SmartIntentParser.parse("Do not delete my files")
        assertTrue("Expected NegatedCommand", intent3 is ParsedIntent.NegatedCommand)
    }

    @Test
    fun testWhatsAppMultiStepParsing() {
        val intent = SmartIntentParser.parse("WhatsApp kholo aur Keshav ko Hi bhej do")
        assertTrue("Expected WhatsAppMultiStep", intent is ParsedIntent.WhatsAppMultiStep)
        val wa = intent as ParsedIntent.WhatsAppMultiStep
        assertEquals("Keshav", wa.contactName)
        assertEquals("Hi", wa.messageText)
    }

    @Test
    fun testGameCoachActivation() {
        val intent = SmartIntentParser.parse("LiaAI, meri screen dekhti rehna aur game mein tips aur alerts deti rehna")
        assertTrue("Expected GameCoachCommand", intent is ParsedIntent.GameCoachCommand)
        val coach = intent as ParsedIntent.GameCoachCommand
        assertEquals(GameCoachAction.START, coach.action)
    }

    @Test
    fun testWebResearchNewsDetection() {
        val intent = SmartIntentParser.parse("Aaj ki Breaking News kya hai?")
        assertTrue("Expected WebResearchCommand", intent is ParsedIntent.WebResearchCommand)
        val res = intent as ParsedIntent.WebResearchCommand
        assertEquals("breaking_news", res.topicCategory)
    }

    @Test
    fun testFreeFireResearchDetection() {
        val intent = SmartIntentParser.parse("Free Fire ke naye updates aur events batao")
        assertTrue("Expected WebResearchCommand", intent is ParsedIntent.WebResearchCommand)
        val res = intent as ParsedIntent.WebResearchCommand
        assertEquals("gaming_free_fire", res.topicCategory)
    }

    @Test
    fun testAppDownloadHelpDetection() {
        val intent1 = SmartIntentParser.parse("App download nahin ho raha bug fix karo")
        assertTrue("Expected AppDownloadHelp", intent1 is ParsedIntent.AppDownloadHelp)

        val intent2 = SmartIntentParser.parse("APK kaise download karein")
        assertTrue("Expected AppDownloadHelp", intent2 is ParsedIntent.AppDownloadHelp)

        val intent3 = SmartIntentParser.parse("phone mein app install nahi ho raha")
        assertTrue("Expected AppDownloadHelp", intent3 is ParsedIntent.AppDownloadHelp)
    }
}
