package com.Lia.assistant.brain

import android.content.Context
import android.util.Log
import com.Lia.assistant.agent.OneShotPlan
import com.Lia.assistant.data.ApiKeyStore

object ModelRouter {

    private const val TAG = "ModelRouter"

    /**
     * Resolves the primary provider based on task type and user settings.
     */
    fun selectProvider(context: Context, taskType: TaskType): ModelProvider {
        val userPref = ApiKeyStore.getModelRoutingPreference(context).uppercase()
        if (userPref != "AUTO") {
            val forced = when (userPref) {
                "GEMINI" -> ModelProvider.GEMINI
                "OPENAI" -> ModelProvider.OPENAI
                "CLAUDE" -> ModelProvider.CLAUDE
                else -> null
            }
            if (forced != null && getProviderInstance(context, forced).hasKey()) {
                return forced
            }
        }

        // Automatic routing per task type
        val candidate = when (taskType) {
            TaskType.PHONE_CONTROL -> ModelProvider.GEMINI
            TaskType.VISION -> ModelProvider.GEMINI
            TaskType.CODING, TaskType.REASONING -> ModelProvider.CLAUDE
            TaskType.GENERAL_CHAT, TaskType.IMAGE_GEN -> ModelProvider.OPENAI
            TaskType.DEEP_RESEARCH -> ModelProvider.GEMINI
        }

        // If candidate has key, use it
        if (getProviderInstance(context, candidate).hasKey()) {
            return candidate
        }

        // Fallback to whichever provider has a key
        val available = getAvailableProviders(context)
        return available.firstOrNull() ?: ModelProvider.GEMINI
    }

    fun getProviderInstance(context: Context, provider: ModelProvider): AiProvider {
        return when (provider) {
            ModelProvider.GEMINI -> GeminiProvider(context)
            ModelProvider.OPENAI -> OpenAiProvider(context)
            ModelProvider.CLAUDE -> ClaudeProvider(context)
        }
    }

    fun getAvailableProviders(context: Context): List<ModelProvider> {
        val list = mutableListOf<ModelProvider>()
        if (GeminiProvider(context).hasKey()) list.add(ModelProvider.GEMINI)
        if (OpenAiProvider(context).hasKey()) list.add(ModelProvider.OPENAI)
        if (ClaudeProvider(context).hasKey()) list.add(ModelProvider.CLAUDE)
        return list
    }

    /**
     * Executes text generation with automatic single-retry and multi-provider fallback.
     * Tells the user ONLY if all available providers fail.
     */
    suspend fun executeWithFallback(
        context: Context,
        taskType: TaskType,
        systemPrompt: String,
        history: List<ChatMessageTurn> = emptyList()
    ): ProviderReply {
        val available = getAvailableProviders(context)
        if (available.isEmpty()) {
            return ProviderReply.Error(
                ModelProvider.GEMINI,
                "Koi bhi AI Provider API key configure nahi hai. Kripya Settings mein Gemini, OpenAI ya Claude ki key enter karein."
            )
        }

        val primary = selectProvider(context, taskType)
        // Ordered queue: primary first, followed by others
        val providerQueue = mutableListOf<ModelProvider>()
        providerQueue.add(primary)
        available.filter { it != primary }.forEach { providerQueue.add(it) }

        val errors = mutableListOf<String>()

        for (providerEnum in providerQueue) {
            val provider = getProviderInstance(context, providerEnum)
            Log.i(TAG, "Attempting request with provider: ${providerEnum.displayName}")

            // Attempt 1
            var reply = provider.generateText(systemPrompt, history)
            if (reply is ProviderReply.Success) {
                return reply
            }

            // Retry once on failure
            Log.w(TAG, "${providerEnum.displayName} failed (Attempt 1). Retrying once...")
            reply = provider.generateText(systemPrompt, history)
            if (reply is ProviderReply.Success) {
                return reply
            }

            val err = if (reply is ProviderReply.Error) reply.error else "Unknown error"
            errors.add("${providerEnum.displayName}: $err")
            Log.w(TAG, "${providerEnum.displayName} failed after retry. Falling back to next available provider...")
        }

        // All providers failed
        val summary = "Sabhi AI providers fail ho gaye:\n" + errors.joinToString("\n")
        return ProviderReply.Error(primary, summary)
    }

    suspend fun ask(context: Context, taskType: TaskType = TaskType.GENERAL_CHAT, prompt: String): String {
        return when (val reply = executeWithFallback(context, taskType, prompt)) {
            is ProviderReply.Success -> reply.text
            is ProviderReply.Error -> reply.error
        }
    }

    /**
     * Generates a Phone Control plan using the fastest available model.
     */
    suspend fun generateFastPlan(context: Context, goal: String): OneShotPlan {
        val primary = selectProvider(context, TaskType.PHONE_CONTROL)
        val provider = getProviderInstance(context, primary)
        return provider.generatePlan(goal) ?: com.Lia.assistant.agent.OneShotPlanner.planLocally(goal)
    }
}
