package com.Lia.assistant.voice

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatTurn(
    val isUser: Boolean,
    val text: String
)

sealed class ChatReplyResult {
    data class Success(val text: String) : ChatReplyResult()
    data class Error(val message: String) : ChatReplyResult()
}

object GeminiTextClient {

    val internalHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedResolvedModel: String? = null

    suspend fun resolveModel(apiKey: String): String {
        cachedResolvedModel?.let { return it }

        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            internalHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val fallback = "models/gemini-2.0-flash"
                    cachedResolvedModel = fallback
                    return fallback
                }

                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val models = json.optJSONArray("models") ?: JSONArray()

                var flashCandidate: String? = null
                var anyGenerateCandidate: String? = null

                for (i in 0 until models.length()) {
                    val modelObj = models.getJSONObject(i)
                    val name = modelObj.optString("name", "")
                    val methods = modelObj.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                    var canGenerate = false
                    for (m in 0 until methods.length()) {
                        if (methods.getString(m) == "generateContent") {
                            canGenerate = true
                            break
                        }
                    }

                    if (canGenerate) {
                        if (anyGenerateCandidate == null) {
                            anyGenerateCandidate = name
                        }
                        val lowerName = name.lowercase()
                        if (lowerName.contains("flash") &&
                            !lowerName.contains("live") &&
                            !lowerName.contains("image") &&
                            !lowerName.contains("tts") &&
                            !lowerName.contains("native-audio")
                        ) {
                            // Prefer gemini-2.0 or newer over older models
                            if (flashCandidate == null ||
                                (lowerName.contains("2.0") || lowerName.contains("2.5")) &&
                                !flashCandidate!!.lowercase().contains("2.0") &&
                                !flashCandidate!!.lowercase().contains("2.5")
                            ) {
                                flashCandidate = name
                            }
                        }
                    }
                }

                val chosen = flashCandidate ?: anyGenerateCandidate ?: "models/gemini-2.0-flash"
                cachedResolvedModel = chosen
                return chosen
            }
        } catch (_: Exception) {
            val fallback = "models/gemini-2.0-flash"
            cachedResolvedModel = fallback
            return fallback
        }
    }

    fun reply(
        apiKey: String,
        systemInstruction: String,
        history: List<ChatTurn>
    ): ChatReplyResult {
        if (apiKey.isBlank()) {
            return ChatReplyResult.Error("Gemini API key is missing. Please add it in Settings.")
        }
        if (history.isEmpty()) {
            return ChatReplyResult.Error("No conversation history provided.")
        }

        val modelName = runCatching {
            kotlinx.coroutines.runBlocking { resolveModel(apiKey) }
        }.getOrDefault("models/gemini-2.0-flash")

        val modelEndpoint = if (modelName.startsWith("models/")) modelName else "models/$modelName"
        val url = "https://generativelanguage.googleapis.com/v1beta/$modelEndpoint:generateContent?key=$apiKey"

        val root = JSONObject()

        // System Instruction
        val sysInstructionObj = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().apply { put("text", systemInstruction) })
        sysInstructionObj.put("parts", sysParts)
        root.put("systemInstruction", sysInstructionObj)

        // Contents
        val contentsArray = JSONArray()
        for (turn in history) {
            val contentObj = JSONObject()
            contentObj.put("role", if (turn.isUser) "user" else "model")
            val partsArray = JSONArray()
            partsArray.put(JSONObject().apply { put("text", turn.text) })
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }
        root.put("contents", contentsArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = root.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            internalHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    val reason = runCatching {
                        JSONObject(errorBody).optJSONObject("error")?.optString("message", "")
                    }.getOrNull() ?: "HTTP error $code"
                    return ChatReplyResult.Error(reason)
                }

                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return ChatReplyResult.Error("The model returned an empty response.")
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return if (text.isNotBlank()) {
                        ChatReplyResult.Success(text)
                    } else {
                        ChatReplyResult.Error("The model returned an empty reply.")
                    }
                }

                return ChatReplyResult.Error("The response was filtered or unavailable.")
            }
        } catch (e: IOException) {
            return ChatReplyResult.Error("Couldn't reach Gemini. Please check your internet connection.")
        } catch (e: Exception) {
            return ChatReplyResult.Error("Failed to communicate with Gemini: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
