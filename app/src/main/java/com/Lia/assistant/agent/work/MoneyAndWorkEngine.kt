package com.Lia.assistant.agent.work

import android.content.Context
import com.Lia.assistant.agent.photo.PhotoFeaturesEngine
import com.Lia.assistant.brain.ModelRouter
import com.Lia.assistant.brain.TaskType
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ExpenseRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val amount: Double,
    val merchantOrParty: String,
    val type: String, // "DEBIT" or "CREDIT"
    val date: String,
    val rawSnippet: String
)

object MoneyAndWorkEngine {
    private const val PREF_EXPENSES = "lia_local_expenses"
    private const val KEY_RECORDS = "expense_records"
    private const val KEY_BUDGET = "monthly_budget"

    /**
     * T4: Local Bank SMS Expense Tracker (100% On-Device Regex Parsing)
     */
    fun parseBankSms(context: Context, smsBody: String): ExpenseRecord? {
        val lower = smsBody.lowercase()
        val isDebit = lower.contains("debited") || lower.contains("spent") || lower.contains("paid") || lower.contains("dr")
        val isCredit = lower.contains("credited") || lower.contains("received") || lower.contains("cr")

        if (!isDebit && !isCredit) return null

        // Extract amount: e.g. Rs. 450.00, INR 1200, Rs 50
        val amountPattern = Pattern.compile("(?:rs\\.?|inr)\\s*([0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcher = amountPattern.matcher(smsBody)
        val amount = if (matcher.find()) {
            matcher.group(1)?.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        if (amount <= 0.0) return null

        // Extract merchant / party
        val merchantPattern = Pattern.compile("(?:at|to|vpa|info|for)\\s+([a-zA-Z0-9\\.\\-_@]+)", Pattern.CASE_INSENSITIVE)
        val merchantMatcher = merchantPattern.matcher(smsBody)
        val merchant = if (merchantMatcher.find()) {
            merchantMatcher.group(1) ?: "Unknown"
        } else {
            "General Transaction"
        }

        val record = ExpenseRecord(
            amount = amount,
            merchantOrParty = merchant,
            type = if (isDebit) "DEBIT" else "CREDIT",
            date = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date()),
            rawSnippet = smsBody.take(60)
        )

        saveExpenseRecord(context, record)
        return record
    }

    private fun saveExpenseRecord(context: Context, record: ExpenseRecord) {
        val prefs = context.getSharedPreferences(PREF_EXPENSES, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }

        val obj = JSONObject().apply {
            put("id", record.id)
            put("amount", record.amount)
            put("merchant", record.merchantOrParty)
            put("type", record.type)
            put("date", record.date)
            put("snippet", record.rawSnippet)
        }
        arr.put(obj)
        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply()
    }

    fun getExpenseSummary(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_EXPENSES, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
        var totalDebit = 0.0
        var totalCredit = 0.0
        var count = 0

        try {
            val arr = JSONArray(raw)
            count = arr.length()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val amt = obj.optDouble("amount", 0.0)
                if (obj.optString("type") == "DEBIT") totalDebit += amt else totalCredit += amt
            }
        } catch (_: Exception) {}

        val budget = prefs.getFloat(KEY_BUDGET, 20000f)
        val remaining = budget - totalDebit

        return "Local Expense Report ($count transactions tracked on-device):\n" +
                "• Total Spent: ₹${"%.2f".format(totalDebit)}\n" +
                "• Total Received: ₹${"%.2f".format(totalCredit)}\n" +
                "• Monthly Budget: ₹${"%.2f".format(budget.toDouble())} (Remaining: ₹${"%.2f".format(remaining.toDouble())})\n" +
                "All bank SMS are parsed locally; no data ever leaves your phone."
    }

    /**
     * T4: Share & Crypto Price Ticker (Never Trades)
     */
    suspend fun getPriceAlert(context: Context, assetName: String): String {
        val prompt = """
            Provide the latest market price ticker estimate and recent 24h trend for: "$assetName" (e.g. Bitcoin, Nifty 50, Reliance).
            Disclose clearly that this is informational ticker intelligence only and LiaAI does not execute financial trading.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.DEEP_RESEARCH, prompt)
    }

    /**
     * T4: Professional Resume, Application & Email Writer
     */
    suspend fun writeProfessionalDoc(context: Context, docType: String, details: String): String {
        val prompt = """
            Draft a professional, polished $docType with proper subject, greeting, clear body paragraphs, and professional sign-off.
            User details: $details
            Language: Professional English / Formal Hindi as appropriate.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T4: Meeting Recorder & Action-Item Summarizer
     */
    suspend fun summarizeMeeting(context: Context, meetingTranscript: String): String {
        val prompt = """
            Analyze this meeting notes transcript and extract:
            1. Meeting Title & Key Decisions Made.
            2. Bulleted Summary.
            3. Action Items with assigned owners and deadlines.
            
            Transcript:
            $meetingTranscript
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T4: Invoice to PDF
     */
    fun scanInvoiceToPdf(context: Context, invoiceText: String, vendorName: String): String {
        return PhotoFeaturesEngine.scanDocumentToPdf(context, "Invoice_$vendorName", invoiceText)
    }
}
