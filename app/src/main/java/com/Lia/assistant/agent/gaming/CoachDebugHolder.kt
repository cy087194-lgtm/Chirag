package com.Lia.assistant.agent.gaming

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CoachDebugHolder {

    private val _currentMode = MutableStateFlow(CoachScreenMode.OTHER_APP)
    val currentMode: StateFlow<CoachScreenMode> = _currentMode.asStateFlow()

    private val _activePackage = MutableStateFlow("")
    val activePackage: StateFlow<String> = _activePackage.asStateFlow()

    private val _lastFrame = MutableStateFlow<Bitmap?>(null)
    val lastFrame: StateFlow<Bitmap?> = _lastFrame.asStateFlow()

    private val _lastModelJson = MutableStateFlow("{}")
    val lastModelJson: StateFlow<String> = _lastModelJson.asStateFlow()

    private val _lastDecision = MutableStateFlow<DecisionRecord?>(null)
    val lastDecision: StateFlow<DecisionRecord?> = _lastDecision.asStateFlow()

    private val _recentDecisions = MutableStateFlow<List<DecisionRecord>>(emptyList())
    val recentDecisions: StateFlow<List<DecisionRecord>> = _recentDecisions.asStateFlow()

    private val _lastTip = MutableStateFlow<String?>(null)
    val lastTip: StateFlow<String?> = _lastTip.asStateFlow()

    private val _statusText = MutableStateFlow("Coach is idle")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun updateModeAndPackage(mode: CoachScreenMode, pkg: String) {
        _currentMode.value = mode
        _activePackage.value = pkg
    }

    fun updateFrame(bitmap: Bitmap, diffPercent: Float) {
        _lastFrame.value = bitmap
    }

    fun updateModelJson(json: String) {
        _lastModelJson.value = json
    }

    fun recordDecision(decision: DecisionRecord) {
        _lastDecision.value = decision
        val list = _recentDecisions.value.toMutableList()
        list.add(0, decision)
        if (list.size > 25) {
            list.removeAt(list.size - 1)
        }
        _recentDecisions.value = list
        if (decision.tip != null && decision.outcome == "SPOKEN") {
            _lastTip.value = decision.tip
        }
    }

    fun setStatus(status: String) {
        _statusText.value = status
    }
}
