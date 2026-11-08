package com.Lia.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveAudioIOTest {

    @Test
    fun testRmsSilence() {
        val silence = ByteArray(2048) { 0 }
        val rms = LiveAudioIO.calculateRms(silence)
        assertEquals(0f, rms, 0.0001f)
    }

    @Test
    fun testRmsMaxAmplitude() {
        val maxWave = ByteArray(2048)
        for (i in 0 until 1024) {
            // 32767 in little endian: 0xFF, 0x7F
            maxWave[i * 2] = 0xFF.toByte()
            maxWave[i * 2 + 1] = 0x7F.toByte()
        }
        val rms = LiveAudioIO.calculateRms(maxWave)
        assertTrue("RMS should be close to 1.0", rms > 0.99f)
    }
}
