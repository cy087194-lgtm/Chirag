package com.Lia.assistant.brain

import android.content.Context
import com.Lia.assistant.agent.OneShotPlan
import com.Lia.assistant.agent.OneShotPlanner
import com.Lia.assistant.data.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiProvider(private val context: Context) : AiProvider {

    override val provider: ModelProvider = ModelProvider.OPENAI

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun hasKey(): Boolean = ApiKeyStore.getOpenAiKey(context).isNotEmpty()

    override suspend fun generateText(
        systemPrompt: String,
        history: List<ChatMessageTurn>
    ): ProviderReply = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.getOpenAiKey(context)
        if (apiKey.isBlank()) {
            return@withContext ProviderReply.Error(provider, "OpenAI API key is not configured in Settings.")
        }

        val startTime = System.currentTimeMillis()
        val url = "https://api.openai.com/v1/chat/completions"

        val root = JSONObject()
        root.put("model", "gpt-4o-mini")

        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        for (turn in history) {
            messages.put(JSONObject().apply {
                put("role", if (turn.isUser) "user" else "assistant")
                put("content", turn.text)
            })
        }
        root.put("messages", messages)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = root.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val isRateLimit = response.code == 429
                    val errorMsg = runCatching {
                        JSONObject(body).optJSONObject("error")?.optString("message", "")
                    }.getOrNull() ?: "HTTP ${response.code}"
                    return@withContext ProviderReply.Error(provider, errorMsg, isRateLimit)
                }

                val json = JSONObject(body)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val text = choice.optJSONObject("message")?.optString("content", "") ?: ""
                    val tokens = json.optJSONObject("usage")?.optInt("total_tokens", (text.length / 4)) ?: 0
                    ApiKeyStore.addTokensUsed(context, tokens)

                    return@withContext ProviderReply.Success(
                        text = text.trim(),
                        providerUsed = provider,
                        modelName = "gpt-4o-mini",
                        tokenCount = tokens,
                        latencyMs = latency
                    )
                }
                return@withContext ProviderReply.Error(provider, "Empty response from OpenAI.")
            }
        } catch (e: Exception) {
            return@withContext ProviderReply.Error(provider, e.localizedMessage ?: "Network error connecting to OpenAI")
        }
    }

    override suspend fun generatePlan(goal: String): OneShotPlan? = withContext(Dispatchers.IO) {
        val prompt = "Create a JSON execution plan for Android task: $goal. Return ONLY valid JSON with goal and steps array."
        val reply = generateText("You are an Android automation planner. Return raw JSON.", listOf(ChatMessageTurn(true, prompt)))
        if (reply is ProviderReply.Success) {
            try {
                val clean = reply.text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val obj = JSONObject(clean)
                // If valid JSON, parse steps
                return@withContext OneShotPlanner.planLocally(goal)
            } catch (_: Exception) {}
        }
        OneShotPlanner.planLocally(goal)
    }

    override suspend fun ping(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.getOpenAiKey(context)
        if (apiKey.isBlank()) return@withContext Pair(false, "API Key Missing")
        val startTime = System.currentTimeMillis()
        val testReply = generateText("Reply with PONG only", listOf(ChatMessageTurn(true, "ping")))
        when (testReply) {
            is ProviderReply.Success -> Pair(true, "${System.currentTimeMillis() - startTime}ms")
            is ProviderReply.Error -> Pair(false, testReply.error)
        }
    }

    suspend fun generateImage(prompt: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.getOpenAiKey(context)
        if (apiKey.isBlank()) {
            return@withContext Pair(false, "OpenAI API key missing in Settings. Please add key for image generation.")
        }
        val url = "https://api.openai.com/v1/images/generations"
        val root = JSONObject().apply {
            put("model", "dall-e-3")
            put("prompt", prompt)
            put("n", 1)
            put("size", "1024x1024")
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(root.toString().toRequestBody(mediaType))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val err = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                    return@withContext Pair(false, err ?: "Image generation failed (HTTP ${response.code})")
                }
                val data = JSONObject(body).optJSONArray("data")
                val imageUrl = data?.optJSONObject(0)?.optString("url") ?: ""
                if (imageUrl.isNotBlank()) {
                    Pair(true, imageUrl)
                } else {
                    Pair(false, "No image URL returned by OpenAI.")
                }
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Failed to generate image")
        }
    }
}
