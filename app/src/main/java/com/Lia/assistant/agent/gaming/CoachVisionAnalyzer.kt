package com.Lia.assistant.agent.gaming

import android.content.Context
import android.util.Base64
import android.util.Log
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.voice.GeminiTextClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CoachVisionAnalyzer {
    private const val TAG = "CoachVisionAnalyzer"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeFrame(
        context: Context,
        jpegBytes: ByteArray,
        mode: CoachScreenMode,
        recentTips: List<String>
    ): CoachAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyStore.getKey(context)
        if (apiKey.isBlank()) {
            return@withContext CoachAnalysisResult(
                state = "other",
                events = emptyList(),
                rawJson = "{\"error\": \"Missing Gemini API Key\"}"
            )
        }

        val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        val prompt = buildPromptForMode(mode, recentTips)

        val modelEndpoint = runCatching {
            GeminiTextClient.resolveModel(apiKey)
        }.getOrDefault("models/gemini-2.0-flash")

        val url = "https://generativelanguage.googleapis.com/v1beta/$modelEndpoint:generateContent?key=$apiKey"

        val root = JSONObject()

        // Generation Config with JSON mime-type
        val genConfig = JSONObject()
        genConfig.put("responseMimeType", "application/json")
        genConfig.put("temperature", 0.2)
        root.put("generationConfig", genConfig)

        // Contents
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        contentObj.put("role", "user")

        val partsArray = JSONArray()
        // 1. Text Prompt
        partsArray.put(JSONObject().apply { put("text", prompt) })

        // 2. Image Inline Data
        val inlineDataObj = JSONObject().apply {
            put("mimeType", "image/jpeg")
            put("data", base64Image)
        }
        partsArray.put(JSONObject().apply { put("inlineData", inlineDataObj) })

        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        try {
            val reqBody = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(reqBody).build()
            val response = httpClient.newCall(request).execute()

            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini Vision error (${response.code}): $bodyStr")
                return@withContext CoachAnalysisResult("other", emptyList(), bodyStr)
            }

            parseResponseJson(bodyStr)
        } catch (e: Throwable) {
            Log.e(TAG, "Vision call failed: ${e.message}", e)
            CoachAnalysisResult(
                state = "other",
                events = emptyList(),
                rawJson = "{\"error\": \"${e.message}\"}"
            )
        }
    }

    private fun buildPromptForMode(mode: CoachScreenMode, recentTips: List<String>): String {
        val historySection = if (recentTips.isNotEmpty()) {
            "Already said recently (DO NOT repeat these unless urgency escalated):\n" +
                    recentTips.takeLast(5).joinToString("\n") { "- \"$it\"" } + "\n"
        } else ""

        return when (mode) {
            CoachScreenMode.GAME_IN_MATCH -> """
You are a real-time tactical game coach watching this Free Fire screen.
Analyze ONLY what is VISIBLE in this exact frame:
1. Minimap: red dots (enemies), safe-zone circle, timer.
2. Health/armor level and ammo count.
3. Visible enemies, cover, buildings, vehicles, loot.
4. Direction of incoming fire.

Strict JSON Output format ONLY:
{
  "state": "in_match",
  "events": [
    {
      "type": "enemy_near|low_health|zone_shrinking|reload|low_ammo|loot_nearby|vehicle|knocked|squad_down|none",
      "direction": "left|right|front|behind|unknown",
      "urgency": 1,
      "say": "short Hinglish sentence under 8 words"
    }
  ]
}

RULES:
- Only report what is VISIBLE in this exact frame. If nothing important is visible, return events as [].
- Never invent enemies. Never give generic tips.
- Every tip MUST be under 8 words in Hinglish (e.g. "samne enemy hai cover lo", "zone chhota ho raha hai", "health kam hai heal karo").
- Urgency: 3 = critical life danger, 2 = tactical action, 1 = minor cue.
$historySection
""".trimIndent()

            CoachScreenMode.GAME_LOBBY, CoachScreenMode.GAME_SETTINGS, CoachScreenMode.GAME_OTHER -> """
You are assisting in the game lobby / settings / loadout screen.
Analyze the visible UI:
- If Sensitivity Settings: read visible sliders (general, red dot, scope) and suggest 1 improvement.
- If Character / Loadout: suggest 1 combination tip.
- If match results: give a short 1-sentence recap.
- If nothing notable, return events as []. Never click or buy anything.

Strict JSON Output format ONLY:
{
  "state": "lobby",
  "events": [
    {
      "type": "none",
      "direction": "unknown",
      "urgency": 2,
      "say": "short Hinglish advice under 15 words"
    }
  ]
}
$historySection
""".trimIndent()

            CoachScreenMode.BROWSER, CoachScreenMode.OTHER_APP -> """
You are watching the user's phone screen.
Look for:
- Urgent security risk / scam alert / fake prize warning.
- Critical app errors.
Otherwise return events: []. Do NOT narrate every action.

Strict JSON Output format ONLY:
{
  "state": "other",
  "events": []
}
$historySection
""".trimIndent()
        }
    }

    private fun parseResponseJson(rawResponse: String): CoachAnalysisResult {
        return try {
            val root = JSONObject(rawResponse)
            val candidates = root.optJSONArray("candidates")
            val firstCand = candidates?.optJSONObject(0)
            val content = firstCand?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "{}")?.trim() ?: "{}"

            // Clean markdown code blocks if present
            val cleanedJson = text.replace("```json", "").replace("```", "").trim()
            val parsedObj = JSONObject(cleanedJson)

            val state = parsedObj.optString("state", "other")
            val eventsList = mutableListOf<TacticalEvent>()
            val eventsArr = parsedObj.optJSONArray("events")

            if (eventsArr != null) {
                for (i in 0 until eventsArr.length()) {
                    val ev = eventsArr.getJSONObject(i)
                    val type = ev.optString("type", "none")
                    if (type != "none") {
                        val dir = ev.optString("direction", "unknown")
                        val urgency = ev.optInt("urgency", 2)
                        val say = ev.optString("say", "").trim()
                        if (say.isNotBlank()) {
                            eventsList.add(TacticalEvent(type, dir, urgency, say))
                        }
                    }
                }
            }

            CoachAnalysisResult(state, eventsList, cleanedJson)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse model JSON: ${e.message}")
            CoachAnalysisResult("other", emptyList(), rawResponse)
        }
    }
}
