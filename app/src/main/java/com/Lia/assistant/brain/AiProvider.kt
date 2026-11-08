package com.Lia.assistant.brain

import com.Lia.assistant.agent.OneShotPlan

enum class ModelProvider(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI("OpenAI (GPT)"),
    CLAUDE("Anthropic Claude")
}

enum class TaskType {
    PHONE_CONTROL,
    GENERAL_CHAT,
    CODING,
    VISION,
    REASONING,
    IMAGE_GEN,
    DEEP_RESEARCH
}

data class ChatMessageTurn(
    val isUser: Boolean,
    val text: String
)

sealed class ProviderReply {
    data class Success(
        val text: String,
        val providerUsed: ModelProvider,
        val modelName: String,
        val tokenCount: Int = 0,
        val latencyMs: Long = 0
    ) : ProviderReply()

    data class Error(
        val provider: ModelProvider,
        val error: String,
        val isRateLimit: Boolean = false
    ) : ProviderReply()
}

interface AiProvider {
    val provider: ModelProvider
    fun hasKey(): Boolean
    suspend fun generateText(systemPrompt: String, history: List<ChatMessageTurn>): ProviderReply
    suspend fun generatePlan(goal: String): OneShotPlan?
    suspend fun ping(): Pair<Boolean, String>
}
