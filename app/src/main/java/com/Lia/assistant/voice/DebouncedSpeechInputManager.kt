package com.Lia.assistant.voice

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

/**
 * Manages speech recognition with strict 3-second silence debounce.
 * Ensures the user is never cut off during natural pauses while speaking long commands.
 * All intermediate pauses are joined into ONE single command.
 */
object DebouncedSpeechInputManager {

    private const val TAG = "DebouncedSpeechInput"
    private const val SILENCE_DEBOUNCE_MS = 3000L

    private val scope = CoroutineScope(Dispatchers.Main)
    private var speechRecognizer: SpeechRecognizer? = null
    private var silenceTimerJob: Job? = null

    private val accumulatedText = StringBuilder()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentBuffer = MutableStateFlow("")
    val currentBuffer: StateFlow<String> = _currentBuffer.asStateFlow()

    private var onFinalCommandCallback: ((String) -> Unit)? = null

    fun startListening(
        context: Context,
        onFinalCommand: (String) -> Unit
    ) {
        onFinalCommandCallback = onFinalCommand
        accumulatedText.clear()
        _currentBuffer.value = ""

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            Log.w(TAG, "Cannot start listening: RECORD_AUDIO permission not granted")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer not available on device")
            return
        }

        try {
            stopListening()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        Log.i(TAG, "SpeechRecognizer ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        // User started speaking, cancel any pending silence commit
                        silenceTimerJob?.cancel()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Audio amplitude update
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        // Native recognizer pause: don't commit yet! Start 3s silence debounce
                        scheduleSilenceCommit(context)
                    }

                    override fun onError(error: Int) {
                        Log.w(TAG, "SpeechRecognizer error: $error")
                        // If error is speech timeout but we have text, commit it after silence
                        if (accumulatedText.isNotEmpty()) {
                            scheduleSilenceCommit(context)
                        } else {
                            _isListening.value = false
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        try {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            appendSpeechSegment(context, text)
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error parsing onResults: ${e.message}")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        try {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                // User is still speaking: cancel silence timer
                                silenceTimerJob?.cancel()
                                _currentBuffer.value = if (accumulatedText.isEmpty()) text else "$accumulatedText $text"
                            }
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error parsing onPartialResults: ${e.message}")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            startRecognizerIntent(context)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize SpeechRecognizer: ${e.message}", e)
            _isListening.value = false
        }
    }

    private fun startRecognizerIntent(context: Context) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start speech listening: ${e.message}", e)
            _isListening.value = false
        }
    }

    /**
     * Appends a recognized speech chunk and resets the 3-second silence countdown.
     * Joins user pauses into one unified command string.
     */
    fun appendSpeechSegment(context: Context, segment: String) {
        if (segment.isBlank()) return

        if (accumulatedText.isNotEmpty() && !accumulatedText.endsWith(" ")) {
            accumulatedText.append(" ")
        }
        accumulatedText.append(segment)
        _currentBuffer.value = accumulatedText.toString()

        Log.i(TAG, "Appended speech segment. Current accumulated command: \"$accumulatedText\"")
        scheduleSilenceCommit(context)
    }

    private fun scheduleSilenceCommit(context: Context) {
        silenceTimerJob?.cancel()
        silenceTimerJob = scope.launch {
            // Wait 3 seconds of continuous silence before treating command as finished
            delay(SILENCE_DEBOUNCE_MS)

            val fullCommand = accumulatedText.toString().trim()
            if (fullCommand.isNotEmpty()) {
                Log.i(TAG, "3 seconds of silence elapsed. Committing full command: \"$fullCommand\"")
                _isListening.value = false
                accumulatedText.clear()
                _currentBuffer.value = ""
                onFinalCommandCallback?.invoke(fullCommand)
            }
        }
    }

    fun stopListening() {
        try {
            silenceTimerJob?.cancel()
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Throwable) {
            Log.w(TAG, "Error in stopListening: ${e.message}")
        } finally {
            speechRecognizer = null
            _isListening.value = false
        }
    }
}
