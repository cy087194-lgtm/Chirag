package com.Lia.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VoiceSafetyTest {

    @Test
    fun testLiveAudioIOSafeEmptyChunk() {
        val audioIO = LiveAudioIO(onMicChunk = {}, onMicAmplitude = {})
        val amp = audioIO.playChunk(ByteArray(0))
        assertEquals(0f, amp, 0.0001f)
        // Ensure release and stop can be called multiple times without exception
        audioIO.stopRecording()
        audioIO.release()
    }

    @Test
    fun testDebouncedSpeechInputManagerBufferJoining() {
        val manager = DebouncedSpeechInputManager
        manager.stopListening()
        assertFalse(manager.isListening.value)
    }
}
