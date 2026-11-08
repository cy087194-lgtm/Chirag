package com.Lia.assistant.forge

import java.io.File

enum class ForgePhase {
    IDLE,
    BUILDING,
    DONE,
    FAILED,
    CANCELLED
}

enum class ForgeStage(val displayName: String, val progressWeight: Float) {
    INITIALIZING("Initializing Forge Reactor", 0.05f),
    SKELETON("Generating HTML & Layout", 0.20f),
    STYLING("Compiling 3D CSS & Gradients", 0.45f),
    VISUALS("Rendering Three.js 3D Scene", 0.70f),
    INTERACTIVITY("Wiring GSAP & Interactions", 0.90f),
    FINALIZING("Polishing Responsive Experience", 1.0f)
}

data class ForgeMetrics(
    val chars: Int = 0,
    val sections: Int = 0,
    val elapsedSec: Int = 0,
    val tokensPerSec: Float = 0f
)

data class ForgeUiState(
    val phase: ForgePhase = ForgePhase.IDLE,
    val prompt: String = "",
    val isEdit: Boolean = false,
    val stage: ForgeStage = ForgeStage.INITIALIZING,
    val progress: Float = 0f,
    val metrics: ForgeMetrics = ForgeMetrics(),
    val sections: List<String> = emptyList(),
    val log: List<String> = emptyList(),
    val logSeq: Int = 0,
    val buildId: String = "",
    val file: File? = null,
    val error: String? = null
)

sealed class ForgeStartResult(val message: String) {
    class Started(message: String = "Opening the Forge...") : ForgeStartResult(message)
    class MissingKey(message: String = "Please add your Gemini API key in Settings first.") : ForgeStartResult(message)
    class Offline(message: String = "You appear to be offline.") : ForgeStartResult(message)
    class Busy(message: String = "A website is already being forged.") : ForgeStartResult(message)
    class NothingToEdit(message: String = "No existing website to edit.") : ForgeStartResult(message)
}
