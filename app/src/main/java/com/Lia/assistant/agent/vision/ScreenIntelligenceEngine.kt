package com.Lia.assistant.agent.vision

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.brain.ModelRouter
import com.Lia.assistant.brain.TaskType
import com.Lia.assistant.brain.ChatMessageTurn
import com.Lia.assistant.brain.ProviderReply

data class ScamScanResult(
    val isScamRisk: Boolean,
    val warning: String,
    val detectedTriggers: List<String>
)

object ScreenIntelligenceEngine {

    /**
     * Scans active screen content for phishing, fake bank messages, lottery traps, and OTP stealing.
     */
    fun scanForScam(screenNodesText: List<String>): ScamScanResult {
        val joined = screenNodesText.joinToString(" ").lowercase()
        val triggers = mutableListOf<String>()

        val suspiciousPatterns = listOf(
            "lottery" to "Lottery / Prize money scam trigger",
            "kbc" to "Fake KBC / Lottery claim",
            "account blocked" to "Urgent account suspension scare",
            "pan card expired" to "Fake KYC / PAN update fraud",
            "share otp" to "Unsafe OTP request",
            "enter upi pin to receive" to "UPI Pin to receive money fraud (PIN only sends money!)",
            "apk download" to "Malicious APK download link",
            "claim reward" to "Unverified reward claim",
            "electric power disconnect" to "Fake electricity disconnection notice"
        )

        for ((pattern, desc) in suspiciousPatterns) {
            if (joined.contains(pattern)) {
                triggers.add(desc)
            }
        }

        return if (triggers.isNotEmpty()) {
            ScamScanResult(
                isScamRisk = true,
                warning = "⚠️ CHEAT / SCAM ALERT: Screen par suspicious messages ya fraudulent links mile hain:\n• " +
                        triggers.joinToString("\n• ") +
                        "\nKabhi bhi apna OTP, UPI PIN ya personal details kisi ke saath share na karein!",
                detectedTriggers = triggers
            )
        } else {
            ScamScanResult(isScamRisk = false, warning = "Screen safe lag rahi hai.", detectedTriggers = emptyList())
        }
    }

    /**
     * Answers questions about the screen content (e.g. "is bill mein total kitna hai?").
     */
    suspend fun answerScreenQuestion(context: Context, question: String): String {
        val service = LiaAccessibilityService.instance
            ?: return "Screen padhne ke liye LiaAI Accessibility permission enable karein."

        val nodes = service.readScreen()
        val texts = nodes.map { it.text.ifEmpty { it.contentDescription } }.filter { it.isNotBlank() }
        if (texts.isEmpty()) {
            return "Screen par koi readable text nahi mila."
        }

        // Quick heuristic check for bill / total
        val lowerQ = question.lowercase()
        if (lowerQ.contains("total") || lowerQ.contains("bill") || lowerQ.contains("amount") || lowerQ.contains("kitna")) {
            val totalMatch = texts.firstOrNull { it.contains("total", ignoreCase = true) || it.contains("₹") }
            if (totalMatch != null) {
                return "Screen analysis result: \"$totalMatch\""
            }
        }

        val prompt = "User question: $question\nVisible screen content:\n${texts.take(40).joinToString("\n")}\nAnswer concisely in Hindi/Hinglish."
        val reply = ModelRouter.executeWithFallback(
            context,
            TaskType.VISION,
            "You are a helpful assistant reading the user's phone screen. Answer questions about the screen.",
            listOf(ChatMessageTurn(true, prompt))
        )

        return if (reply is ProviderReply.Success) reply.text else "Screen par information nahi mil saki."
    }

    /**
     * Translates visible screen text into Hindi/English.
     */
    suspend fun translateScreen(context: Context, targetLanguage: String = "Hindi"): String {
        val service = LiaAccessibilityService.instance
            ?: return "Accessibility Service enable nahi hai."
        val nodes = service.readScreen()
        val texts = nodes.map { it.text }.filter { it.isNotBlank() }.take(25)
        if (texts.isEmpty()) return "Screen par translate karne ke liye koi text nahi mila."

        val prompt = "Translate the following phone screen texts into $targetLanguage:\n${texts.joinToString("\n")}"
        val reply = ModelRouter.executeWithFallback(
            context,
            TaskType.GENERAL_CHAT,
            "Translate screen texts accurately while preserving app UI layout meaning.",
            listOf(ChatMessageTurn(true, prompt))
        )
        return if (reply is ProviderReply.Success) reply.text else "Translation fail ho gaya."
    }

    /**
     * Copies all visible screen text to device clipboard.
     */
    fun copyScreenTextToClipboard(context: Context): String {
        val service = LiaAccessibilityService.instance ?: return "Accessibility connected nahi hai."
        val nodes = service.readScreen()
        val text = nodes.map { it.text }.filter { it.isNotBlank() }.joinToString("\n")
        if (text.isEmpty()) return "Screen par copy karne ke liye koi text nahi mila."

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Screen Text", text)
        clipboard?.setPrimaryClip(clip)
        return "Screen text successfully clipboard mein copy ho gaya! (${nodes.size} elements)"
    }
}
