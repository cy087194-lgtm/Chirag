package com.Lia.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VoiceLatencyTracker {
    private val _reconnectCount = MutableStateFlow(0)
    val reconnectCount: StateFlow<Int> = _reconnectCount.asStateFlow()

    private val _lastTurnLatencyMs = MutableStateFlow(0L)
    val lastTurnLatencyMs: StateFlow<Long> = _lastTurnLatencyMs.asStateFlow()

    private var turnStartTime: Long = 0L
    private var firstByteReceived: Boolean = false
    private val lock = Any()

    fun incrementReconnect() {
        _reconnectCount.value += 1
    }

    fun resetReconnect() {
        _reconnectCount.value = 0
    }

    fun markUserTurnStart(timestamp: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            turnStartTime = timestamp
            firstByteReceived = false
        }
    }

    fun markFirstAudioChunk(timestamp: Long = System.currentTimeMillis()): Long {
        synchronized(lock) {
            if (turnStartTime > 0L && !firstByteReceived) {
                if (timestamp >= turnStartTime) {
                    val delta = timestamp - turnStartTime
                    _lastTurnLatencyMs.value = delta
                    firstByteReceived = true
                    return delta
                }
            }
            return _lastTurnLatencyMs.value
        }
    }

    fun markTurnComplete() {
        synchronized(lock) {
            turnStartTime = 0L
            firstByteReceived = false
        }
    }

    fun reset() {
        synchronized(lock) {
            _reconnectCount.value = 0
            _lastTurnLatencyMs.value = 0L
            turnStartTime = 0L
            firstByteReceived = false
        }
    }
}
