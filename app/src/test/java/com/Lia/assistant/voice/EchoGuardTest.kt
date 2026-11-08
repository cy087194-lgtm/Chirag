package com.Lia.assistant.voice

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EchoGuardTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        EchoGuard.testDispatcher = testDispatcher
        EchoGuard.reset()
    }

    @After
    fun tearDown() {
        EchoGuard.reset()
        EchoGuard.testDispatcher = null
    }

    @Test
    fun testPlaybackMutesImmediately() {
        assertFalse(EchoGuard.isMicMuted())
        EchoGuard.notifyPlaybackStarted()
        assertTrue(EchoGuard.isMicMuted())
        assertEquals(1, EchoGuard.getActivePlaybackCount())
    }

    @Test
    fun testUnmutesAfterEchoTail() = testScope.runTest {
        EchoGuard.notifyPlaybackStarted()
        assertTrue(EchoGuard.isMicMuted())

        EchoGuard.notifyPlaybackStopped()
        // Still muted before 450ms tail expires
        advanceTimeBy(300)
        assertTrue(EchoGuard.isMicMuted())

        // Past 450ms tail -> should be unmuted
        advanceTimeBy(200)
        assertFalse(EchoGuard.isMicMuted())
    }

    @Test
    fun testMultiplePlaybackOverlap() = testScope.runTest {
        EchoGuard.notifyPlaybackStarted() // count = 1
        EchoGuard.notifyPlaybackStarted() // count = 2
        assertEquals(2, EchoGuard.getActivePlaybackCount())

        EchoGuard.notifyPlaybackStopped() // count = 1
        advanceTimeBy(500)
        // Still muted because 1 chunk is still playing
        assertTrue(EchoGuard.isMicMuted())

        EchoGuard.notifyPlaybackStopped() // count = 0
        advanceTimeBy(400)
        assertTrue(EchoGuard.isMicMuted())

        advanceTimeBy(100) // Total 500ms > 450ms tail
        assertFalse(EchoGuard.isMicMuted())
    }

    @Test
    fun testWatchdogForceUnmute() = testScope.runTest {
        EchoGuard.notifyPlaybackStarted()
        assertTrue(EchoGuard.isMicMuted())

        // Simulate a scenario where playback stop was missed
        advanceTimeBy(59_000)
        assertTrue(EchoGuard.isMicMuted())

        advanceTimeBy(2_000) // Total 61 seconds > 60s watchdog
        assertFalse(EchoGuard.isMicMuted())
    }
}
