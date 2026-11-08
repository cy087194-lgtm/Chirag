package com.Lia.assistant.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global holder for live progress during fast one-shot autonomous task execution.
 * Allows the overlay orb and UI components to display live step progress "(3/6)"
 * without interrupting the user or generating voice chatter during execution.
 */
object OneShotProgressHolder {

    private val _liveProgressText = MutableStateFlow<String?>(null)
    val liveProgressText: StateFlow<String?> = _liveProgressText.asStateFlow()

    private val _currentGoal = MutableStateFlow<String?>(null)
    val currentGoal: StateFlow<String?> = _currentGoal.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    fun startTask(goal: String, totalSteps: Int) {
        _isExecuting.value = true
        _currentGoal.value = goal
        _liveProgressText.value = "(1/$totalSteps)"
    }

    fun updateStep(stepIndex: Int, totalSteps: Int) {
        _liveProgressText.value = "($stepIndex/$totalSteps)"
    }

    fun finishTask() {
        _isExecuting.value = false
        _liveProgressText.value = null
        _currentGoal.value = null
    }

    fun reset() {
        finishTask()
    }
}
