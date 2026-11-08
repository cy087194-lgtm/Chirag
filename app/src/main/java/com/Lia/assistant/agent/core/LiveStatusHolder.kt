package com.Lia.assistant.agent.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SelfTestResult(
    val testName: String,
    val isPassed: Boolean,
    val durationMs: Long,
    val message: String
)

object LiveStatusHolder {

    private val _accessibilityActive = MutableStateFlow(false)
    val accessibilityActive: StateFlow<Boolean> = _accessibilityActive.asStateFlow()

    private val _captureActive = MutableStateFlow(false)
    val captureActive: StateFlow<Boolean> = _captureActive.asStateFlow()

    private val _contactsGranted = MutableStateFlow(false)
    val contactsGranted: StateFlow<Boolean> = _contactsGranted.asStateFlow()

    private val _overlayGranted = MutableStateFlow(false)
    val overlayGranted: StateFlow<Boolean> = _overlayGranted.asStateFlow()

    private val _lastFrameTime = MutableStateFlow(0L)
    val lastFrameTime: StateFlow<Long> = _lastFrameTime.asStateFlow()

    private val _lastFrameDiff = MutableStateFlow(0f)
    val lastFrameDiff: StateFlow<Float> = _lastFrameDiff.asStateFlow()

    private val _currentMode = MutableStateFlow("IDLE")
    val currentMode: StateFlow<String> = _currentMode.asStateFlow()

    private val _foregroundPackage = MutableStateFlow("")
    val foregroundPackage: StateFlow<String> = _foregroundPackage.asStateFlow()

    private val _lastModelReply = MutableStateFlow("No model query yet")
    val lastModelReply: StateFlow<String> = _lastModelReply.asStateFlow()

    private val _lastModelJson = MutableStateFlow("{}")
    val lastModelJson: StateFlow<String> = _lastModelJson.asStateFlow()

    private val _lastSpokenTip = MutableStateFlow("None")
    val lastSpokenTip: StateFlow<String> = _lastSpokenTip.asStateFlow()

    private val _lastToolCall = MutableStateFlow("None")
    val lastToolCall: StateFlow<String> = _lastToolCall.asStateFlow()

    private val _lastToolTarget = MutableStateFlow("")
    val lastToolTarget: StateFlow<String> = _lastToolTarget.asStateFlow()

    private val _lastToolResult = MutableStateFlow("None")
    val lastToolResult: StateFlow<String> = _lastToolResult.asStateFlow()

    private val _lastToolDurationMs = MutableStateFlow(0L)
    val lastToolDurationMs: StateFlow<Long> = _lastToolDurationMs.asStateFlow()

    private val _selfTestResults = MutableStateFlow<Map<String, SelfTestResult>>(emptyMap())
    val selfTestResults: StateFlow<Map<String, SelfTestResult>> = _selfTestResults.asStateFlow()

    fun updatePermissions(
        accessibility: Boolean,
        capture: Boolean,
        contacts: Boolean,
        overlay: Boolean
    ) {
        _accessibilityActive.value = accessibility
        _captureActive.value = capture
        _contactsGranted.value = contacts
        _overlayGranted.value = overlay
    }

    fun updateFrame(diff: Float, mode: String, pkg: String) {
        _lastFrameTime.value = System.currentTimeMillis()
        _lastFrameDiff.value = diff
        _currentMode.value = mode
        _foregroundPackage.value = pkg
    }

    fun updateModelReply(reply: String, json: String) {
        _lastModelReply.value = reply
        _lastModelJson.value = json
    }

    fun updateSpokenTip(tip: String) {
        _lastSpokenTip.value = tip
    }

    fun recordToolExecution(tool: String, target: String, result: String, durationMs: Long) {
        _lastToolCall.value = tool
        _lastToolTarget.value = target
        _lastToolResult.value = result
        _lastToolDurationMs.value = durationMs
    }

    fun recordTestResult(testName: String, passed: Boolean, durationMs: Long, message: String) {
        val current = _selfTestResults.value.toMutableMap()
        current[testName] = SelfTestResult(testName, passed, durationMs, message)
        _selfTestResults.value = current
    }
}
