package com.Lia.assistant.agent

data class OneShotStep(
    val step: Int,
    val action: String,
    val target: String = "",
    val query: String = "",
    val fallbackIds: List<String> = emptyList(),
    val fallbackTexts: List<String> = emptyList(),
    val description: String = "",
    val canSkipIfDone: Boolean = false
)

data class OneShotPlan(
    val goal: String,
    val steps: List<OneShotStep>
)

data class OneShotExecutionResult(
    val isSuccess: Boolean,
    val goal: String,
    val totalSteps: Int,
    val completedSteps: Int,
    val failedStepIndex: Int? = null,
    val failureReason: String? = null,
    val voiceSummary: String
)
