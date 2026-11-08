package com.Lia.assistant.forge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.Lia.assistant.MainActivity
import com.Lia.assistant.R
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.voice.GeminiTextClient
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object WebsiteForgeClient {

    private const val NOTIFICATION_CHANNEL_ID = "forge_channel"
    private const val NOTIFICATION_ID = 2002

    suspend fun forgeWebsite(
        context: Context,
        prompt: String,
        isEdit: Boolean,
        initialHtml: String?
    ) {
        val apiKey = ApiKeyStore.getKey(context)
        if (apiKey.isEmpty()) {
            ForgeController.updateState {
                it.copy(
                    phase = ForgePhase.FAILED,
                    error = "Missing Gemini API Key. Please add it in Settings."
                )
            }
            return
        }

        val resolvedModel = GeminiTextClient.resolveModel(apiKey)
        val systemPrompt = if (isEdit && initialHtml != null) {
            ForgeSystemPrompt.forEdit(initialHtml, prompt)
        } else {
            ForgeSystemPrompt.forNewSite(prompt)
        }

        var attempt = 0
        var success = false
        var currentPromptToSend = systemPrompt

        while (attempt < 2 && !success) {
            attempt++
            try {
                val ok = executeStream(
                    context = context,
                    apiKey = apiKey,
                    model = resolvedModel,
                    systemPrompt = currentPromptToSend
                )

                if (ok) {
                    success = true
                } else if (attempt < 2) {
                    delay(4000)
                    currentPromptToSend = "$systemPrompt\n\nNote: Write completely original markup, copy and code; finish the whole page completely from <!DOCTYPE html> to </html>."
                }
            } catch (e: Exception) {
                if (attempt >= 2) {
                    ForgeController.updateState {
                        it.copy(
                            phase = ForgePhase.FAILED,
                            error = "Forge build failed: ${e.localizedMessage ?: "Unknown error"}"
                        )
                    }
                } else {
                    delay(4000)
                }
            }
        }
    }

    private suspend fun executeStream(
        context: Context,
        apiKey: String,
        model: String,
        systemPrompt: String
    ): Boolean {
        val modelEndpoint = if (model.startsWith("models/")) model else "models/$model"
        val url = "https://generativelanguage.googleapis.com/v1beta/$modelEndpoint:streamGenerateContent?alt=sse"

        val root = JSONObject()

        val contents = JSONArray()
        val contentObj = JSONObject()
        contentObj.put("role", "user")
        val parts = JSONArray()
        parts.put(JSONObject().apply { put("text", systemPrompt) })
        contentObj.put("parts", parts)
        contents.put(contentObj)
        root.put("contents", contents)

        val genConfig = JSONObject()
        if (model.contains("2.5")) {
            val thinkingConfig = JSONObject().apply { put("thinkingBudget", 0) }
            genConfig.put("thinkingConfig", thinkingConfig)
        }
        root.put("generationConfig", genConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = root.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(requestBody)
            .build()

        val startTime = System.currentTimeMillis()
        var lastStateEmission = 0L
        val accumulatedText = StringBuilder()
        var finalFinishReason: String? = null

        val response = GeminiTextClient.internalHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            if (code == 503) return false // trigger retry
            ForgeController.updateState {
                it.copy(
                    phase = ForgePhase.FAILED,
                    error = "Gemini service returned HTTP $code"
                )
            }
            return true
        }

        val inputStream = response.body?.byteStream() ?: return false
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        var line: String? = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("data:")) {
                val dataContent = trimmed.substring(5).trim()
                val chunk = SseParser.extractText(dataContent)
                val finishReason = SseParser.extractFinishReason(dataContent)
                if (finishReason != null) {
                    finalFinishReason = finishReason
                }

                if (!chunk.isNullOrEmpty()) {
                    accumulatedText.append(chunk)
                    ForgeController.appendChunk(chunk)

                    val now = System.currentTimeMillis()
                    if (now - lastStateEmission > 120) {
                        lastStateEmission = now
                        val currentText = accumulatedText.toString()
                        val currentStage = ForgeStageDetector.detect(currentText)
                        val sections = ForgeStageDetector.extractSectionNames(currentText)
                        val elapsed = ((now - startTime) / 1000).toInt()
                        val tokensPerSec = if (elapsed > 0) (currentText.length / 4f) / elapsed else 0f

                        ForgeController.updateState {
                            it.copy(
                                stage = currentStage,
                                progress = currentStage.progressWeight,
                                metrics = ForgeMetrics(
                                    chars = currentText.length,
                                    sections = sections.size,
                                    elapsedSec = elapsed,
                                    tokensPerSec = tokensPerSec
                                ),
                                sections = sections
                            )
                        }
                    }
                }
            }
            line = reader.readLine()
        }

        var fullHtml = ForgeHtml.stripFences(accumulatedText.toString())

        // Check completeness
        if (fullHtml.length < 1500 || (!fullHtml.contains("</html>", ignoreCase = true) && finalFinishReason == "RECITATION")) {
            return false // trigger retry
        }

        if (!fullHtml.contains("</html>", ignoreCase = true)) {
            if (fullHtml.length >= 14000) {
                fullHtml = ForgeHtml.repairTruncated(fullHtml)
            } else {
                ForgeController.updateState {
                    it.copy(
                        phase = ForgePhase.FAILED,
                        error = "Gemini stopped before finishing the website generation."
                    )
                }
                return true
            }
        }

        // Apply safety net
        fullHtml = ForgeHtml.withSafetyNet(fullHtml)

        // Save generated website
        val dir = File(context.filesDir, "Websites")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "website_${System.currentTimeMillis()}.html")
        file.writeText(fullHtml)

        val totalTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val sections = ForgeStageDetector.extractSectionNames(fullHtml)

        ForgeController.updateState {
            it.copy(
                phase = ForgePhase.DONE,
                stage = ForgeStage.FINALIZING,
                progress = 1.0f,
                file = file,
                sections = sections,
                metrics = ForgeMetrics(
                    chars = fullHtml.length,
                    sections = sections.size,
                    elapsedSec = totalTime
                ),
                log = it.log + "Website successfully forged!"
            )
        }

        sendNotificationIfBackground(context, file.name)
        return true
    }

    private fun sendNotificationIfBackground(context: Context, filename: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Website Forge",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("EXTRA_OPEN_FORGE", true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            200,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Your website is ready!")
            .setContentText("Tap to view and interact with your newly forged website.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notif)
    }
}
