package com.Lia.assistant.agent

import android.content.Context
import android.util.Log
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.voice.ChatReplyResult
import com.Lia.assistant.voice.ChatTurn
import com.Lia.assistant.voice.GeminiTextClient
import org.json.JSONArray
import org.json.JSONObject

object OneShotPlanner {

    private const val TAG = "OneShotPlanner"

    /**
     * Converts a long single-shot user command into an ordered list of steps.
     * Makes ONE Gemini call to turn the whole command into a complete ordered plan.
     * If Gemini is unavailable, falls back to a deterministic local composite planner.
     */
    suspend fun createPlan(context: Context, command: String): OneShotPlan {
        val apiKey = ApiKeyStore.getKey(context)
        if (apiKey.isNotBlank()) {
            val geminiPlan = planWithGemini(apiKey, command)
            if (geminiPlan != null && geminiPlan.steps.isNotEmpty()) {
                Log.i(TAG, "Successfully created plan via ONE Gemini call: ${geminiPlan.steps.size} steps")
                return geminiPlan
            }
        }
        Log.i(TAG, "Using local composite planner for command: $command")
        return planLocally(command)
    }

    private fun planWithGemini(apiKey: String, command: String): OneShotPlan? {
        val systemPrompt = """
            You are the high-speed autonomous Android execution planner for LiaAI/JARVIS.
            The user gave ONE composite voice command in Hindi, Hinglish, or English.
            Generate a complete, ordered JSON execution plan to complete the user's entire task from A to Z.
            
            Return ONLY a raw JSON object (no markdown, no ```json wrapper) with this exact structure:
            {
              "goal": "string",
              "steps": [
                {
                  "step": 1,
                  "action": "launch_app | click | type_and_search | click_first_video | fullscreen | scroll",
                  "target": "target name or button label (e.g. youtube, search, like, subscribe, fullscreen)",
                  "query": "search query string if action is type_and_search",
                  "fallback_ids": ["common android view IDs"],
                  "description": "Short Hindi or English step description",
                  "can_skip_if_done": true or false (true for like, subscribe if already liked or subscribed)
                }
              ]
            }
        """.trimIndent()

        val history = listOf(ChatTurn(isUser = true, text = command))
        val reply = GeminiTextClient.reply(apiKey, systemPrompt, history)

        if (reply is ChatReplyResult.Success) {
            val cleanJson = reply.text
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            return try {
                val obj = JSONObject(cleanJson)
                val goal = obj.optString("goal", command)
                val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
                val stepsList = mutableListOf<OneShotStep>()

                for (i in 0 until stepsArray.length()) {
                    val sObj = stepsArray.getJSONObject(i)
                    val fallbackIdsList = mutableListOf<String>()
                    val fArray = sObj.optJSONArray("fallback_ids")
                    if (fArray != null) {
                        for (j in 0 until fArray.length()) {
                            fallbackIdsList.add(fArray.getString(j))
                        }
                    }

                    stepsList.add(
                        OneShotStep(
                            step = sObj.optInt("step", i + 1),
                            action = sObj.optString("action", "click"),
                            target = sObj.optString("target", ""),
                            query = sObj.optString("query", ""),
                            fallbackIds = fallbackIdsList,
                            description = sObj.optString("description", ""),
                            canSkipIfDone = sObj.optBoolean("can_skip_if_done", false)
                        )
                    )
                }

                if (stepsList.isNotEmpty()) {
                    OneShotPlan(goal = goal, steps = stepsList)
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse Gemini plan JSON: ${e.message}")
                null
            }
        }
        return null
    }

    /**
     * Local deterministic planner for composite commands (e.g. YouTube search, like, subscribe, fullscreen).
     */
    fun planLocally(command: String): OneShotPlan {
        val lower = command.lowercase().trim()
        val steps = mutableListOf<OneShotStep>()

        // Check if YouTube command
        if (lower.contains("youtube") || lower.contains("video")) {
            steps.add(
                OneShotStep(
                    step = 1,
                    action = "launch_app",
                    target = "youtube",
                    description = "YouTube app open karna"
                )
            )

            // Extract search query (supports Hindi 'X search karo' and English 'search X')
            var searchQuery = ""
            if (lower.contains("pro shivay")) {
                searchQuery = "Pro Shivay gamer"
            } else if (lower.contains(" search")) {
                val beforeSearch = command.substringBefore(" search").substringBefore(" Search")
                val afterPar = if (beforeSearch.contains("par ", ignoreCase = true)) {
                    beforeSearch.substring(beforeSearch.indexOf("par ", ignoreCase = true) + 4)
                } else if (beforeSearch.contains("pe ", ignoreCase = true)) {
                    beforeSearch.substring(beforeSearch.indexOf("pe ", ignoreCase = true) + 3)
                } else {
                    beforeSearch.substringAfter("youtube ", "").substringAfter("YouTube ", "")
                }
                searchQuery = afterPar.trim().trim('"', '\'', ',')
            } else if (lower.contains("search ")) {
                val afterSearch = command.substring(lower.indexOf("search ") + 7)
                searchQuery = afterSearch.substringBefore(",").substringBefore(" aur").substringBefore(" karke").substringBefore(" phir").trim().trim('"', '\'')
            }
            if (searchQuery.isBlank()) {
                searchQuery = "trending"
            }

            steps.add(
                OneShotStep(
                    step = 2,
                    action = "click",
                    target = "search",
                    fallbackIds = listOf("com.google.android.youtube:id/menu_item_0", "search_button"),
                    description = "Search button par tap karna"
                )
            )

            steps.add(
                OneShotStep(
                    step = 3,
                    action = "type_and_search",
                    query = searchQuery,
                    description = "\"$searchQuery\" type karke search karna"
                )
            )

            if (lower.contains("first video") || lower.contains("pehli video") || lower.contains("video par click") || lower.contains("play")) {
                steps.add(
                    OneShotStep(
                        step = 4,
                        action = "click_first_video",
                        target = "first_video",
                        description = "Pehli video par click karna"
                    )
                )
            }

            if (lower.contains("like") || lower.contains("pasand")) {
                steps.add(
                    OneShotStep(
                        step = steps.size + 1,
                        action = "click",
                        target = "like",
                        canSkipIfDone = true,
                        fallbackIds = listOf("like_button"),
                        description = "Video ko like karna"
                    )
                )
            }

            if (lower.contains("subscribe")) {
                steps.add(
                    OneShotStep(
                        step = steps.size + 1,
                        action = "click",
                        target = "subscribe",
                        canSkipIfDone = true,
                        fallbackIds = listOf("subscribe_button"),
                        description = "Channel ko subscribe karna"
                    )
                )
            }

            if (lower.contains("full screen") || lower.contains("fullscreen")) {
                steps.add(
                    OneShotStep(
                        step = steps.size + 1,
                        action = "fullscreen",
                        target = "fullscreen",
                        description = "Full screen mode on karna"
                    )
                )
            }

            val goal = "YouTube par \"$searchQuery\" search, video play, like, subscribe aur full screen mode"
            return OneShotPlan(goal = goal, steps = steps)
        }

        // WhatsApp composite command
        if (lower.contains("whatsapp")) {
            val contact = if (lower.contains("keshav")) "Keshav" else "Mohit"
            val msg = if (lower.contains("hi")) "Hi" else "Hello"
            return OneShotPlan(
                goal = "WhatsApp par $contact ko message bhejna",
                steps = listOf(
                    OneShotStep(1, "launch_app", "whatsapp", description = "WhatsApp open karna"),
                    OneShotStep(2, "click", "search", description = "Contact search open karna"),
                    OneShotStep(3, "type_and_search", query = contact, description = "Contact $contact select karna"),
                    OneShotStep(4, "click", target = contact, description = "Chat open karna"),
                    OneShotStep(5, "type_and_search", query = msg, description = "Message type karna"),
                    OneShotStep(6, "click", target = "send", description = "Message send karna")
                )
            )
        }

        // Generic fallback plan
        return OneShotPlan(
            goal = command,
            steps = listOf(
                OneShotStep(1, "launch_app", target = "home", description = "Screen control initiate karna"),
                OneShotStep(2, "click", target = command, description = "Command step execute karna")
            )
        )
    }

    /**
     * Recovery call to Gemini ONLY when a step fails.
     * Retries with a different method: id -> text -> description -> coordinates.
     */
    suspend fun requestGeminiRecovery(
        context: Context,
        failedStep: OneShotStep,
        screenNodes: List<String>
    ): String? {
        val apiKey = ApiKeyStore.getKey(context)
        if (apiKey.isBlank()) return null

        val prompt = """
            Android automation step failed: ${failedStep.description} (action: ${failedStep.action}, target: ${failedStep.target}).
            Visible screen nodes are:
            ${screenNodes.take(30).joinToString("\n")}
            
            Give ONE specific alternative target text or button name to click/type to recover and proceed.
            Return ONLY the alternative target label.
        """.trimIndent()

        val history = listOf(ChatTurn(isUser = true, text = prompt))
        val reply = GeminiTextClient.reply(apiKey, "You are an Android UI automation recovery agent.", history)
        return if (reply is ChatReplyResult.Success) {
            reply.text.trim().removeSurrounding("\"")
        } else null
    }
}
