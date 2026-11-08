package com.Lia.assistant.forge

import android.content.Context
import com.Lia.assistant.data.ApiKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

object ForgeController {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var buildJob: Job? = null

    private val _state = MutableStateFlow(ForgeUiState())
    val state: StateFlow<ForgeUiState> = _state.asStateFlow()

    @Volatile
    var pendingOpen: Boolean = false
        private set

    @Volatile
    private var currentAccumulatedHtml: String = ""

    fun requestOpen() {
        pendingOpen = true
    }

    fun consumePendingOpen(): Boolean {
        val was = pendingOpen
        pendingOpen = false
        return was
    }

    fun partialHtml(): String = currentAccumulatedHtml

    fun start(context: Context, prompt: String): ForgeStartResult {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            return ForgeStartResult.Started("Please tell me what kind of website you want to build.")
        }

        if (!ApiKeyStore.hasKey(context)) {
            return ForgeStartResult.MissingKey()
        }

        if (_state.value.phase == ForgePhase.BUILDING) {
            return ForgeStartResult.Busy()
        }

        val buildId = UUID.randomUUID().toString()
        currentAccumulatedHtml = ""
        pendingOpen = true

        _state.value = ForgeUiState(
            phase = ForgePhase.BUILDING,
            prompt = trimmed,
            isEdit = false,
            stage = ForgeStage.INITIALIZING,
            progress = 0.05f,
            buildId = buildId,
            log = listOf("Initializing Forge Reactor...")
        )

        buildJob?.cancel()
        buildJob = scope.launch {
            WebsiteForgeClient.forgeWebsite(
                context = context,
                prompt = trimmed,
                isEdit = false,
                initialHtml = null
            )
        }

        return ForgeStartResult.Started("Starting the Forge for '$trimmed'.")
    }

    fun edit(context: Context, change: String): ForgeStartResult {
        val trimmed = change.trim()
        val currentFile = _state.value.file
        if (currentFile == null || !currentFile.exists()) {
            return ForgeStartResult.NothingToEdit()
        }

        if (!ApiKeyStore.hasKey(context)) {
            return ForgeStartResult.MissingKey()
        }

        if (_state.value.phase == ForgePhase.BUILDING) {
            return ForgeStartResult.Busy()
        }

        val existingHtml = currentFile.readText()
        currentAccumulatedHtml = existingHtml
        pendingOpen = true

        _state.value = _state.value.copy(
            phase = ForgePhase.BUILDING,
            prompt = trimmed,
            isEdit = true,
            stage = ForgeStage.INITIALIZING,
            progress = 0.05f,
            error = null
        )

        buildJob?.cancel()
        buildJob = scope.launch {
            WebsiteForgeClient.forgeWebsite(
                context = context,
                prompt = trimmed,
                isEdit = true,
                initialHtml = existingHtml
            )
        }

        return ForgeStartResult.Started("Refining your website with '$trimmed'...")
    }

    fun retry(context: Context): ForgeStartResult {
        val lastPrompt = _state.value.prompt
        return if (lastPrompt.isNotEmpty()) {
            start(context, lastPrompt)
        } else {
            ForgeStartResult.Started("Please provide a prompt to start building.")
        }
    }

    fun cancel() {
        buildJob?.cancel()
        buildJob = null
        _state.value = _state.value.copy(
            phase = ForgePhase.CANCELLED,
            log = _state.value.log + "Build cancelled by user."
        )
    }

    fun reset() {
        buildJob?.cancel()
        buildJob = null
        currentAccumulatedHtml = ""
        _state.value = ForgeUiState()
    }

    fun openSaved(file: File) {
        if (!file.exists()) return
        val content = file.readText()
        currentAccumulatedHtml = content
        pendingOpen = true
        _state.value = ForgeUiState(
            phase = ForgePhase.DONE,
            prompt = file.nameWithoutExtension,
            isEdit = false,
            stage = ForgeStage.FINALIZING,
            progress = 1.0f,
            file = file,
            metrics = ForgeMetrics(chars = content.length)
        )
    }

    fun appendChunk(chunk: String) {
        currentAccumulatedHtml += chunk
    }

    fun updateState(transform: (ForgeUiState) -> ForgeUiState) {
        _state.value = transform(_state.value)
    }
}
