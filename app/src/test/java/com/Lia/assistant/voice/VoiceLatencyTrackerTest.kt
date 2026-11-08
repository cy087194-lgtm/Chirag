package com.Lia.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VoiceLatencyTrackerTest {

    @Before
    fun setup() {
        VoiceLatencyTracker.reset()
    }

    @Test
    fun testInOrderMarksProduceCorrectDelta() {
        VoiceLatencyTracker.markUserTurnStart(1000L)
        val delta = VoiceLatencyTracker.markFirstAudioChunk(1350L)
        assertEquals(350L, delta)
        assertEquals(350L, VoiceLatencyTracker.lastTurnLatencyMs.value)
    }

    @Test
    fun testOutOfOrderMarksIgnored() {
        VoiceLatencyTracker.markUserTurnStart(2000L)
        // Timestamp before start time should be ignored
        val delta = VoiceLatencyTracker.markFirstAudioChunk(1500L)
        assertEquals(0L, delta)
        assertEquals(0L, VoiceLatencyTracker.lastTurnLatencyMs.value)
    }

    @Test
    fun testReconnectCounter() {
        assertEquals(0, VoiceLatencyTracker.reconnectCount.value)
        VoiceLatencyTracker.incrementReconnect()
        VoiceLatencyTracker.incrementReconnect()
        assertEquals(2, VoiceLatencyTracker.reconnectCount.value)
        VoiceLatencyTracker.resetReconnect()
        assertEquals(0, VoiceLatencyTracker.reconnectCount.value)
    }
}
