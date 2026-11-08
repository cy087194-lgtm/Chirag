package com.Lia.assistant.voice

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object EchoGuard {
    const val ECHO_TAIL_MS = 450L
    const val WATCHDOG_TIMEOUT_MS = 60_000L

    private val activePlaybackCount = AtomicInteger(0)
    private val muted = AtomicBoolean(false)
    private var tailJob: Job? = null
    private var watchdogJob: Job? = null
    private val lock = Any()

    var testDispatcher: CoroutineDispatcher? = null

    private fun getScope(): CoroutineScope {
        val dispatcher = testDispatcher ?: Dispatchers.Default
        return CoroutineScope(dispatcher)
    }

    fun isMicMuted(): Boolean = muted.get()

    fun getActivePlaybackCount(): Int = activePlaybackCount.get()

    fun notifyPlaybackStarted() {
        synchronized(lock) {
            val count = activePlaybackCount.incrementAndGet()
            muted.set(true)
            tailJob?.cancel()
            tailJob = null

            if (count == 1) {
                // Arm 60s watchdog to prevent stuck mute states
                watchdogJob?.cancel()
                watchdogJob = getScope().launch {
                    delay(WATCHDOG_TIMEOUT_MS)
                    synchronized(lock) {
                        activePlaybackCount.set(0)
                        muted.set(false)
                    }
                }
            }
        }
    }

    fun notifyPlaybackStopped() {
        synchronized(lock) {
            val count = activePlaybackCount.decrementAndGet()
            if (count <= 0) {
                activePlaybackCount.set(0)
                tailJob?.cancel()
                tailJob = getScope().launch {
                    delay(ECHO_TAIL_MS)
                    synchronized(lock) {
                        if (activePlaybackCount.get() == 0) {
                            muted.set(false)
                            watchdogJob?.cancel()
                            watchdogJob = null
                        }
                    }
                }
            }
        }
    }

    fun reset() {
        synchronized(lock) {
            activePlaybackCount.set(0)
            muted.set(false)
            tailJob?.cancel()
            tailJob = null
            watchdogJob?.cancel()
            watchdogJob = null
        }
    }
}
