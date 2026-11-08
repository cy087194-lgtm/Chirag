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

class ClaudeProvider(private val context: Context) : AiProvider {

    override val provider: ModelProvider = ModelProvider.CLAUDE

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun hasKey(): Boolean = ApiKeyStore.getClaudeKey(context).isNotEmpty()

    override suspend fun generateText(
        systemPrompt: String,
        history: List<ChatMessageTurn>
    ): ProviderReply = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.getClaudeKey(context)
        if (apiKey.isBlank()) {
            return@withContext ProviderReply.Error(provider, "Anthropic Claude API key is not configured in Settings.")
        }

        val startTime = System.currentTimeMillis()
        val url = "https://api.anthropic.com/v1/messages"

        val root = JSONObject()
        root.put("model", "claude-3-5-haiku-20241022")
        root.put("max_tokens", 2048)
        root.put("system", systemPrompt)

        val messages = JSONArray()
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
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
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
                val contentArray = json.optJSONArray("content")
                if (contentArray != null && contentArray.length() > 0) {
                    val firstBlock = contentArray.getJSONObject(0)
                    val text = firstBlock.optString("text", "")
                    val usage = json.optJSONObject("usage")
                    val tokens = (usage?.optInt("input_tokens", 0) ?: 0) + (usage?.optInt("output_tokens", 0) ?: 0)
                    ApiKeyStore.addTokensUsed(context, tokens)

                    return@withContext ProviderReply.Success(
                        text = text.trim(),
                        providerUsed = provider,
                        modelName = "claude-3-5-haiku",
                        tokenCount = tokens,
                        latencyMs = latency
                    )
                }
                return@withContext ProviderReply.Error(provider, "Empty response from Claude.")
            }
        } catch (e: Exception) {
            return@withContext ProviderReply.Error(provider, e.localizedMessage ?: "Network error connecting to Claude")
        }
    }

    override suspend fun generatePlan(goal: String): OneShotPlan? = withContext(Dispatchers.IO) {
        OneShotPlanner.planLocally(goal)
    }

    override suspend fun ping(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.getClaudeKey(context)
        if (apiKey.isBlank()) return@withContext Pair(false, "API Key Missing")
        val startTime = System.currentTimeMillis()
        val testReply = generateText("Reply with PONG only", listOf(ChatMessageTurn(true, "ping")))
        when (testReply) {
            is ProviderReply.Success -> Pair(true, "${System.currentTimeMillis() - startTime}ms")
            is ProviderReply.Error -> Pair(false, testReply.error)
        }
    }
}
