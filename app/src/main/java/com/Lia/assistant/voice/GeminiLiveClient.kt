package com.Lia.assistant.voice

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveClient(private val listener: Listener) {

    interface Listener {
        fun onSetupComplete()
        fun onAudioChunk(bytes: ByteArray)
        fun onInputTranscript(text: String)
        fun onOutputTranscript(text: String)
        fun onToolCall(id: String, name: String, args: JSONObject)
        fun onTurnComplete()
        fun onInterrupted()
        fun onError(message: String)
        fun onClosed()
    }

    companion object {
        const val MODEL = "models/gemini-3.1-flash-live-preview"
        const val BASE_WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val deduplicator = ToolCallDeduplicator(maxSize = 100)
    private val isConnected = AtomicBoolean(false)

    fun isLive(): Boolean = isConnected.get()

    fun connect(apiKey: String, systemInstruction: String, voiceName: String = "Aoede") {
        if (apiKey.isBlank()) {
            listener.onError("Missing Gemini API Key. Please configure it in Settings.")
            return
        }

        disconnect()
        deduplicator.clear()

        val url = "$BASE_WS_URL?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected.set(true)
                sendSetupMessage(ws, systemInstruction, voiceName)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                handleServerMessage(bytes.utf8())
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                val msg = t.localizedMessage ?: "WebSocket failure"
                listener.onError(msg)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                listener.onClosed()
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket, systemInstruction: String, voiceName: String) {
        val root = JSONObject()
        val setup = JSONObject()

        setup.put("model", MODEL)

        val generationConfig = JSONObject()
        val modalities = JSONArray()
        modalities.put("AUDIO")
        generationConfig.put("responseModalities", modalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", voiceName)
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        generationConfig.put("speechConfig", speechConfig)

        setup.put("generationConfig", generationConfig)

        val sysInstructionObj = JSONObject()
        val parts = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", systemInstruction)
        parts.put(partObj)
        sysInstructionObj.put("parts", parts)
        setup.put("systemInstruction", sysInstructionObj)

        setup.put("inputAudioTranscription", JSONObject())
        setup.put("outputAudioTranscription", JSONObject())

        val tools = JSONArray()
        val toolObj = JSONObject()
        toolObj.put("functionDeclarations", LiaToolCatalog.declarations())
        tools.put(toolObj)
        setup.put("tools", tools)

        root.put("setup", setup)
        ws.send(root.toString())
    }

    fun sendAudio(pcmBytes: ByteArray) {
        val ws = webSocket ?: return
        if (!isConnected.get() || pcmBytes.isEmpty()) return

        val base64Audio = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        val root = JSONObject()
        val realtimeInput = JSONObject()
        val audio = JSONObject()
        audio.put("mimeType", "audio/pcm;rate=16000")
        audio.put("data", base64Audio)
        realtimeInput.put("audio", audio)
        root.put("realtimeInput", realtimeInput)

        ws.send(root.toString())
    }

    fun sendToolResponse(id: String, name: String, resultJson: JSONObject) {
        val ws = webSocket ?: return
        if (!isConnected.get()) return

        val root = JSONObject()
        val toolResponse = JSONObject()
        val functionResponses = JSONArray()
        val singleResponse = JSONObject()
        singleResponse.put("id", id)
        singleResponse.put("name", name)
        singleResponse.put("response", resultJson)
        functionResponses.put(singleResponse)
        toolResponse.put("functionResponses", functionResponses)
        root.put("toolResponse", toolResponse)

        ws.send(root.toString())
    }

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)

            // 1. Setup complete
            if (json.has("setupComplete")) {
                listener.onSetupComplete()
                return
            }

            // 2. Silently ignore control signals
            if (json.has("sessionResumptionUpdate") || json.has("goAway")) {
                return
            }

            // 3. Tool calls
            if (json.has("toolCall")) {
                val toolCall = json.getJSONObject("toolCall")
                if (toolCall.has("functionCalls")) {
                    val calls = toolCall.getJSONArray("functionCalls")
                    for (i in 0 until calls.length()) {
                        val call = calls.getJSONObject(i)
                        val id = call.optString("id", "")
                        val name = call.optString("name", "")
                        val args = call.optJSONObject("args") ?: JSONObject()

                        if (deduplicator.shouldProcess(id)) {
                            listener.onToolCall(id, name, args)
                        }
                    }
                }
            }

            // 4. Server content
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                // Interrupted
                if (serverContent.optBoolean("interrupted", false)) {
                    listener.onInterrupted()
                }

                // Model turn (audio chunks)
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    if (modelTurn.has("parts")) {
                        val parts = modelTurn.getJSONArray("parts")
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val data = inlineData.optString("data", "")
                                if (data.isNotEmpty()) {
                                    val audioBytes = Base64.decode(data, Base64.NO_WRAP)
                                    listener.onAudioChunk(audioBytes)
                                }
                            }
                        }
                    }
                }

                // User audio transcription
                if (serverContent.has("inputTranscription")) {
                    val inputTranscription = serverContent.getJSONObject("inputTranscription")
                    val text = inputTranscription.optString("text", "")
                    if (text.isNotBlank()) {
                        listener.onInputTranscript(text)
                    }
                }

                // Model output audio transcription
                if (serverContent.has("outputTranscription")) {
                    val outputTranscription = serverContent.getJSONObject("outputTranscription")
                    val text = outputTranscription.optString("text", "")
                    if (text.isNotBlank()) {
                        listener.onOutputTranscript(text)
                    }
                }

                // Turn complete
                if (serverContent.optBoolean("turnComplete", false)) {
                    listener.onTurnComplete()
                }
            }
        } catch (_: Exception) {
            // Silently swallow parse errors without leaking raw payload
        }
    }

    fun disconnect() {
        isConnected.set(false)
        try {
            webSocket?.close(1000, "Normal Closure")
        } catch (_: Exception) {}
        webSocket = null
        deduplicator.clear()
    }
}
