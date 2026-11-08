package com.Lia.assistant.agent

import com.Lia.assistant.agent.gaming.CoachAntiRepeatEngine
import com.Lia.assistant.agent.gaming.CoachFrequency
import com.Lia.assistant.agent.gaming.EvaluationDecision
import com.Lia.assistant.agent.gaming.ScreenCaptureManager
import com.Lia.assistant.agent.gaming.TacticalEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoachEngineTest {

    @Before
    fun setUp() {
        CoachAntiRepeatEngine.clear()
    }

    @Test
    fun testSameFrameReturnsZeroOrLowDiff() {
        // (d) The same frame twice -> no repeated tip
        val hashA = IntArray(256) { 120 }
        val hashB = IntArray(256) { 120 }
        val diff = ScreenCaptureManager.computeHashDiffPercent(hashA, hashB)
        assertEquals(0.0f, diff, 0.001f)

        // Different frame
        val hashC = IntArray(256) { if (it % 2 == 0) 250 else 30 }
        val diffC = ScreenCaptureManager.computeHashDiffPercent(hashA, hashC)
        assertTrue("Different frame should exceed 10% diff", diffC > 10.0f)
    }

    @Test
    fun testAntiRepeatPreventsConsecutiveDuplicateSentence() {
        val event = TacticalEvent(
            type = "enemy_near",
            direction = "right",
            urgency = 2,
            say = "right se enemy aa raha hai"
        )

        // 1. First time: Approved
        val firstDecision = CoachAntiRepeatEngine.evaluateEvent(event, CoachFrequency.NORMAL)
        assertTrue("First event should be approved", firstDecision is EvaluationDecision.Approved)
        val approvedTip = (firstDecision as EvaluationDecision.Approved).tipToSpeak
        CoachAntiRepeatEngine.recordSpoken(approvedTip, event.type, event.urgency)

        // 2. Exact same sentence immediately: Rejected!
        val secondDecision = CoachAntiRepeatEngine.evaluateEvent(event, CoachFrequency.NORMAL)
        assertTrue("Immediate duplicate sentence must be rejected", secondDecision is EvaluationDecision.Rejected)
        val reason = (secondDecision as EvaluationDecision.Rejected).reason
        assertTrue(reason.contains("Exact same sentence") || reason.contains("Cooldown active"))
    }

    @Test
    fun testAntiRepeatEnforcesCooldownPerEventType() {
        val event1 = TacticalEvent(
            type = "enemy_near",
            direction = "left",
            urgency = 2,
            say = "left se enemy"
        )
        val firstDecision = CoachAntiRepeatEngine.evaluateEvent(event1, CoachFrequency.NORMAL)
        assertTrue(firstDecision is EvaluationDecision.Approved)
        CoachAntiRepeatEngine.recordSpoken(
            (firstDecision as EvaluationDecision.Approved).tipToSpeak,
            event1.type,
            event1.urgency
        )

        // Different wording, but same event type ("enemy_near") within 4s cooldown
        val event2 = TacticalEvent(
            type = "enemy_near",
            direction = "right",
            urgency = 2,
            say = "right mein footstep"
        )
        val secondDecision = CoachAntiRepeatEngine.evaluateEvent(event2, CoachFrequency.NORMAL)
        assertTrue("Must be rejected due to 4s cooldown on enemy_near", secondDecision is EvaluationDecision.Rejected)
    }

    @Test
    fun testUrgencyThresholds() {
        val lowUrgencyEvent = TacticalEvent(
            type = "loot_nearby",
            direction = "front",
            urgency = 1,
            say = "medkit samne hai"
        )

        // Kam setting requires urgency 3
        val decKam = CoachAntiRepeatEngine.evaluateEvent(lowUrgencyEvent, CoachFrequency.KAM)
        assertTrue(decKam is EvaluationDecision.Rejected)

        // Normal setting requires urgency 2
        val decNormal = CoachAntiRepeatEngine.evaluateEvent(lowUrgencyEvent, CoachFrequency.NORMAL)
        assertTrue(decNormal is EvaluationDecision.Rejected)

        // Zyada setting allows urgency 1
        val decZyada = CoachAntiRepeatEngine.evaluateEvent(lowUrgencyEvent, CoachFrequency.ZYADA)
        assertTrue(decZyada is EvaluationDecision.Approved)
    }

    @Test
    fun testBrevityTrimsLongSentencesUnderEightWords() {
        val longEvent = TacticalEvent(
            type = "zone_shrinking",
            direction = "front",
            urgency = 2,
            say = "zone chhota ho raha hai jaldi safe zone ke andar bhaag kar cover lo"
        )
        val decision = CoachAntiRepeatEngine.evaluateEvent(longEvent, CoachFrequency.NORMAL)
        assertTrue(decision is EvaluationDecision.Approved)
        val tip = (decision as EvaluationDecision.Approved).tipToSpeak
        val wordCount = tip.split("\\s+".toRegex()).size
        assertTrue("Tip must be trimmed to 8 words or less, was $wordCount", wordCount <= 8)
    }

    @Test
    fun testStaleEventsDropped() {
        val staleEvent = TacticalEvent(
            type = "enemy_near",
            direction = "behind",
            urgency = 3,
            say = "peeche enemy hai"
        )
        // 4 seconds old (> 3000ms max stale age)
        val pastTimestamp = System.currentTimeMillis() - 4000L
        val decision = CoachAntiRepeatEngine.evaluateEvent(staleEvent, CoachFrequency.NORMAL, pastTimestamp)
        assertTrue("Stale event (>3s old) must be rejected", decision is EvaluationDecision.Rejected)
    }

    @Test
    fun testStopCommandsRecognizedInSmartIntentParser() {
        val stopPhrases = listOf(
            "stop",
            "ruko",
            "ruk jao",
            "bas",
            "bas karo",
            "chup",
            "coach off",
            "watch off"
        )

        for (phrase in stopPhrases) {
            val parsed = SmartIntentParser.parse(phrase)
            assertTrue("Expected GameCoachCommand for '$phrase', got $parsed", parsed is ParsedIntent.GameCoachCommand)
            assertEquals("Expected STOP action for '$phrase'", GameCoachAction.STOP, (parsed as ParsedIntent.GameCoachCommand).action)
        }
    }

    @Test
    fun testStartCommandsRecognizedInSmartIntentParser() {
        val startPhrases = listOf(
            "coach on",
            "watch on",
            "game coach on",
            "game shuru",
            "meri screen dekhte raho"
        )

        for (phrase in startPhrases) {
            val parsed = SmartIntentParser.parse(phrase)
            assertTrue("Expected GameCoachCommand for '$phrase', got $parsed", parsed is ParsedIntent.GameCoachCommand)
            assertEquals("Expected START action for '$phrase'", GameCoachAction.START, (parsed as ParsedIntent.GameCoachCommand).action)
        }
    }
}
