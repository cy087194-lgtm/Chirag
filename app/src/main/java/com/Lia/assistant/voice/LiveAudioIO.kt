package com.Lia.assistant.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class LiveAudioIO(
    private val onMicChunk: (ByteArray) -> Unit,
    private val onMicAmplitude: (Float) -> Unit
) {
    companion object {
        private const val TAG = "LiveAudioIO"
        const val MIC_SAMPLE_RATE = 16000
        const val SPEAKER_SAMPLE_RATE = 24000
        const val MIC_CHUNK_SIZE = 2048

        fun calculateRms(pcmBytes: ByteArray): Float {
            if (pcmBytes.size < 2) return 0f
            var sumSquares = 0.0
            val sampleCount = pcmBytes.size / 2
            for (i in 0 until sampleCount) {
                val low = pcmBytes[i * 2].toInt() and 0xFF
                val high = pcmBytes[i * 2 + 1].toInt()
                val sample = (high shl 8) or low
                sumSquares += (sample * sample).toDouble()
            }
            val mean = sumSquares / sampleCount
            val rawRms = sqrt(mean) / 32768.0
            return max(0f, min(1f, rawRms.toFloat()))
        }
    }

    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null

    private var audioTrack: AudioTrack? = null
    private val isRecording = AtomicBoolean(false)
    private var recordThread: Thread? = null
    private val lock = Any()
    private var currentVolume = 1.0f

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording.get()) return

        val minBuf = AudioRecord.getMinBufferSize(
            MIC_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = max(minBuf, 4096)

        try {
            var record: AudioRecord? = null

            // 1. Attempt VOICE_COMMUNICATION source (AEC-friendly)
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MIC_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize
                )
            } catch (_: Throwable) {
                record = null
            }

            // 2. Fallback to standard MIC source if VOICE_COMMUNICATION fails or is uninitialized
            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                try {
                    record?.release()
                } catch (_: Throwable) {}

                try {
                    record = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        MIC_SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufSize
                    )
                } catch (_: Throwable) {
                    record = null
                }
            }

            // 3. Verify initialization state before startRecording to prevent IllegalStateException
            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                try {
                    record?.release()
                } catch (_: Throwable) {}
                Log.w(TAG, "AudioRecord could not be initialized on this hardware")
                isRecording.set(false)
                return
            }

            val sessionId = record.audioSessionId
            if (sessionId != 0) {
                try {
                    if (AcousticEchoCanceler.isAvailable()) {
                        echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
                    }
                } catch (_: Throwable) {}

                try {
                    if (NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
                    }
                } catch (_: Throwable) {}

                try {
                    if (AutomaticGainControl.isAvailable()) {
                        gainControl = AutomaticGainControl.create(sessionId)?.apply { enabled = true }
                    }
                } catch (_: Throwable) {}
            }

            record.startRecording()
            audioRecord = record
            isRecording.set(true)

            recordThread = Thread({
                val buffer = ByteArray(MIC_CHUNK_SIZE)
                while (isRecording.get()) {
                    try {
                        val read = record.read(buffer, 0, MIC_CHUNK_SIZE)
                        if (read > 0) {
                            val chunk = if (read == MIC_CHUNK_SIZE) buffer.clone() else buffer.copyOf(read)
                            val amp = calculateRms(chunk)
                            onMicAmplitude(amp)
                            onMicChunk(chunk)
                        } else if (read < 0) {
                            break
                        }
                    } catch (_: Throwable) {
                        break
                    }
                }
            }, "LiveAudioIO-MicThread").apply { start() }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start AudioRecord safely: ${e.message}", e)
            isRecording.set(false)
        }
    }

    fun stopRecording() {
        isRecording.set(false)
        recordThread?.interrupt()
        recordThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Throwable) {}
        audioRecord = null

        try { echoCanceler?.release() } catch (_: Throwable) {}
        try { noiseSuppressor?.release() } catch (_: Throwable) {}
        try { gainControl?.release() } catch (_: Throwable) {}
        echoCanceler = null
        noiseSuppressor = null
        gainControl = null
    }

    private fun getOrCreateSpeaker(): AudioTrack? {
        synchronized(lock) {
            audioTrack?.let { return it }

            val minBuf = AudioTrack.getMinBufferSize(
                SPEAKER_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = max(minBuf * 4, 4096)

            return try {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SPEAKER_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack(
                    attributes,
                    format,
                    bufSize,
                    AudioTrack.MODE_STREAM,
                    0
                )
                track.setVolume(currentVolume)
                try {
                    track.play()
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not start AudioTrack playback immediately: ${e.message}")
                }
                audioTrack = track
                track
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize AudioTrack: ${e.message}", e)
                null
            }
        }
    }

    fun playChunk(pcmBytes: ByteArray): Float {
        if (pcmBytes.isEmpty()) return 0f
        return try {
            val track = getOrCreateSpeaker() ?: return 0f
            track.write(pcmBytes, 0, pcmBytes.size)
            calculateRms(pcmBytes)
        } catch (_: Throwable) {
            0f
        }
    }

    fun flushPlayback() {
        synchronized(lock) {
            audioTrack?.let { track ->
                try {
                    track.pause()
                    track.flush()
                    track.play()
                } catch (_: Throwable) {}
            }
        }
    }

    fun setVolume(volume: Float) {
        val clamped = max(0f, min(1f, volume))
        currentVolume = clamped
        synchronized(lock) {
            try {
                audioTrack?.setVolume(clamped)
            } catch (_: Throwable) {}
        }
    }

    fun release() {
        stopRecording()
        synchronized(lock) {
            audioTrack?.let { track ->
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
            }
            audioTrack = null
        }
    }
}
