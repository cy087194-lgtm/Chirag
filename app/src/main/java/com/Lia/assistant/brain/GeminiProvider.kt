package com.Lia.assistant.brain

import android.content.Context
import com.Lia.assistant.agent.OneShotPlan
import com.Lia.assistant.agent.OneShotPlanner
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.voice.ChatReplyResult
import com.Lia.assistant.voice.ChatTurn
import com.Lia.assistant.voice.GeminiTextClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiProvider(private val context: Context) : AiProvider {

    override val provider: ModelProvider = ModelProvider.GEMINI

    override fun hasKey(): Boolean = ApiKeyStore.getGeminiKey(context).isNotEmpty()

    override suspend fun generateText(
        systemPrompt: String,
        history: List<ChatMessageTurn>
    ): ProviderReply = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.getGeminiKey(context)
        if (apiKey.isBlank()) {
            return@withContext ProviderReply.Error(provider, "Gemini API key is not configured in Settings.")
        }

        val startTime = System.currentTimeMillis()
        val geminiHistory = history.map { ChatTurn(isUser = it.isUser, text = it.text) }

        when (val res = GeminiTextClient.reply(apiKey, systemPrompt, geminiHistory)) {
            is ChatReplyResult.Success -> {
                val latency = System.currentTimeMillis() - startTime
                val estTokens = (res.text.length + history.sumOf { it.text.length }) / 4
                ApiKeyStore.addTokensUsed(context, estTokens)
                ProviderReply.Success(
                    text = res.text,
                    providerUsed = provider,
                    modelName = "gemini-2.0-flash",
                    tokenCount = estTokens,
                    latencyMs = latency
                )
            }
            is ChatReplyResult.Error -> {
                val isRateLimit = res.message.contains("429") || res.message.contains("quota", ignoreCase = true)
                ProviderReply.Error(provider, res.message, isRateLimit = isRateLimit)
            }
        }
    }

    override suspend fun generatePlan(goal: String): OneShotPlan? = withContext(Dispatchers.IO) {
        OneShotPlanner.createPlan(context, goal)
    }

    override suspend fun ping(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.getGeminiKey(context)
        if (apiKey.isBlank()) return@withContext Pair(false, "API Key Missing")
        val startTime = System.currentTimeMillis()
        val testHistory = listOf(ChatTurn(isUser = true, text = "Ping"))
        when (val res = GeminiTextClient.reply(apiKey, "Respond only with PONG", testHistory)) {
            is ChatReplyResult.Success -> Pair(true, "${System.currentTimeMillis() - startTime}ms")
            is ChatReplyResult.Error -> Pair(false, res.message)
        }
    }
}
