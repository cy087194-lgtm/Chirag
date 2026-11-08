package com.Lia.assistant.agent.gaming

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

object BargeInStopListener {
    private const val TAG = "BargeInStopListener"

    private val scope = CoroutineScope(Dispatchers.Main)
    private var recognizer: SpeechRecognizer? = null
    private var restartJob: Job? = null
    private var isListening = false

    private val _lastDebugRecord = MutableStateFlow(StopDebugRecord())
    val lastDebugRecord: StateFlow<StopDebugRecord> = _lastDebugRecord.asStateFlow()

    private var currentSpokenTip: String = ""

    fun updateCurrentlySpeakingTip(text: String) {
        currentSpokenTip = text.lowercase().trim()
    }

    fun startListening(context: Context, onStopTriggered: () -> Unit, onCommandTriggered: ((String) -> Unit)? = null) {
        if (isListening) return

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission || !SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Audio permission not granted or SpeechRecognizer unavailable")
            return
        }

        isListening = true
        initRecognizer(context, onStopTriggered, onCommandTriggered)
    }

    private fun initRecognizer(
        context: Context,
        onStopTriggered: () -> Unit,
        onCommandTriggered: ((String) -> Unit)?
    ) {
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        scheduleRestart(context, onStopTriggered, onCommandTriggered)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        handleSpokenMatches(context, matches, onStopTriggered, onCommandTriggered)
                        scheduleRestart(context, onStopTriggered, onCommandTriggered)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        handleSpokenMatches(context, matches, onStopTriggered, onCommandTriggered)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            startListeningIntent(context)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize SpeechRecognizer for stop detection: ${e.message}")
        }
    }

    private fun startListeningIntent(context: Context) {
        if (!isListening) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer?.startListening(intent)
        } catch (e: Throwable) {
            Log.w(TAG, "Recognizer startListening error: ${e.message}")
        }
    }

    private fun handleSpokenMatches(
        context: Context,
        matches: List<String>?,
        onStopTriggered: () -> Unit,
        onCommandTriggered: ((String) -> Unit)?
    ) {
        if (matches.isNullOrEmpty() || !isListening) return
        val startTime = System.currentTimeMillis()
        val stopWords = CoachPreferences.getStopWords(context)

        for (phrase in matches) {
            val lower = phrase.lowercase().trim()
            if (lower.isBlank()) continue

            // 1. Echo filter: If the phrase is part of what the coach itself is speaking, ignore
            if (currentSpokenTip.isNotBlank() && currentSpokenTip.contains(lower)) {
                Log.d(TAG, "Ignoring self-echo match: \"$lower\"")
                continue
            }

            // 2. Check for stop words
            val matchedWord = stopWords.firstOrNull { stopWord ->
                lower == stopWord || lower.startsWith("$stopWord ") || lower.endsWith(" $stopWord") || lower.contains(" $stopWord ")
            }

            if (matchedWord != null) {
                val latency = System.currentTimeMillis() - startTime
                _lastDebugRecord.value = StopDebugRecord(
                    lastHeard = phrase,
                    matchedWord = matchedWord,
                    latencyMs = latency,
                    timestamp = System.currentTimeMillis()
                )
                Log.i(TAG, "INSTANT STOP TRIGGERED by word: \"$matchedWord\" in ${latency}ms")
                stopListening()
                onStopTriggered()
                return
            }

            // 3. Barge-in other command: e.g. "YouTube kholo"
            if (lower.contains("kholo") || lower.contains("open") || lower.contains("search") || lower.contains("batao")) {
                Log.i(TAG, "User real command heard while coach was active: \"$phrase\"")
                onCommandTriggered?.invoke(phrase)
                return
            }
        }
    }

    private fun scheduleRestart(
        context: Context,
        onStopTriggered: () -> Unit,
        onCommandTriggered: ((String) -> Unit)?
    ) {
        if (!isListening) return
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(350)
            if (isListening) {
                startListeningIntent(context)
            }
        }
    }

    fun stopListening() {
        isListening = false
        restartJob?.cancel()
        restartJob = null
        currentSpokenTip = ""
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Throwable) {}
        recognizer = null
    }
}
