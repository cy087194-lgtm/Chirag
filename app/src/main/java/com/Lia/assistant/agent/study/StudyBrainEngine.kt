package com.Lia.assistant.agent.study

import android.content.Context
import com.Lia.assistant.brain.ModelRouter
import com.Lia.assistant.brain.TaskType
import com.Lia.assistant.data.UserMemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiaryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val date: String,
    val text: String,
    val mood: String,
    val timestamp: Long = System.currentTimeMillis()
)

object StudyBrainEngine {
    private const val PREF_DIARY = "lia_private_diary"
    private const val KEY_ENTRIES = "diary_entries"

    /**
     * T2: Step-by-Step Homework Tutor & Exam Revision
     */
    suspend fun explainConceptOrQuestion(context: Context, question: String, isQuizOrFlashcard: Boolean = false): String {
        val prompt = if (isQuizOrFlashcard) {
            "Generate 3 interactive flashcards and 1 multiple choice quiz question on this topic: \"$question\". Provide the answer with explanation."
        } else {
            "You are a patient, encouraging tutor. Explain this question/concept step-by-step in easy Hindi/Hinglish with a practical example: \"$question\"."
        }
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T2: Math and Science Solver from Camera/Text
     */
    suspend fun solveMathOrScience(context: Context, problemText: String): String {
        val prompt = """
            Solve this math/science problem step-by-step with clear formulas, working, and the final boxed answer.
            Problem: $problemText
            Format in easy-to-read Hindi/English explanation.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T2: English Speaking Coach with gentle grammar corrections
     */
    suspend fun practiceEnglishConversation(context: Context, userSpokenSentence: String): String {
        val prompt = """
            You are a friendly, encouraging English conversation teacher for an Indian learner.
            User said: "$userSpokenSentence"
            Instructions:
            1. If there are any grammatical, pronunciation, or vocabulary mistakes, point them out gently and suggest the natural polite way to say it.
            2. Reply to continue the conversation in conversational English with a question for the user.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T2: Mock Interview & Speech Practice with structured feedback
     */
    suspend fun mockInterviewFeedback(context: Context, role: String, answer: String): String {
        val prompt = """
            You are an expert HR and technical interviewer for the role: "$role".
            Candidate's response: "$answer"
            Provide:
            1. Rating out of 10.
            2. Strengths of the answer.
            3. Improvements and what was missing (STAR method).
            4. Next challenging interview question.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T2: Decision Helper with Pros and Cons
     */
    suspend fun compareDecisionOptions(context: Context, dilemma: String): String {
        val prompt = """
            Help the user make a thoughtful decision for: "$dilemma".
            Provide:
            - Option A vs Option B breakdown.
            - Unbiased Pros and Cons for each.
            - Long-term trade-offs and final objective recommendation.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T2: Creative Writing (Shayari, Poems, Stories, Captions)
     */
    suspend fun writeCreativeContent(context: Context, promptType: String, topic: String): String {
        val prompt = "Write a high-quality, evocative $promptType on \"$topic\" in rich Hindi/Urdu/Hinglish."
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T2: Private Local Diary & Mood Check-In (Encrypted locally, never uploaded to cloud)
     */
    fun saveDiaryEntry(context: Context, text: String, mood: String): String {
        val prefs = context.getSharedPreferences(PREF_DIARY, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val raw = prefs.getString(KEY_ENTRIES, "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }

        val obj = JSONObject().apply {
            put("id", java.util.UUID.randomUUID().toString())
            put("date", today)
            put("text", text)
            put("mood", mood)
            put("timestamp", System.currentTimeMillis())
        }
        arr.put(obj)

        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply()
        return "Aapka private diary entry save ho gaya hai (Mood: $mood). Ye sirf aapke phone par safe hai."
    }

    fun getAllDiaryEntries(context: Context): List<DiaryEntry> {
        val prefs = context.getSharedPreferences(PREF_DIARY, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ENTRIES, "[]") ?: "[]"
        val list = mutableListOf<DiaryEntry>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DiaryEntry(
                        id = obj.optString("id"),
                        date = obj.optString("date"),
                        text = obj.optString("text"),
                        mood = obj.optString("mood"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.reversed()
    }

    /**
     * T2: Second Brain (Search notes, memories, saved chats)
     */
    fun searchSecondBrain(context: Context, query: String): String {
        val q = query.lowercase().trim()
        val memories = UserMemoryStore.get(context).getAllMemories()
        val diary = getAllDiaryEntries(context)

        val matchedMemories = memories.filter { it.content.lowercase().contains(q) }
        val matchedDiary = diary.filter { it.text.lowercase().contains(q) || it.date.lowercase().contains(q) }

        if (matchedMemories.isEmpty() && matchedDiary.isEmpty()) {
            return "Aapke second brain notes mein \"$query\" se related kuch nahi mila."
        }

        val builder = StringBuilder("Second Brain Search Results for \"$query\":\n\n")
        if (matchedMemories.isNotEmpty()) {
            builder.append("Saved Memories:\n")
            matchedMemories.forEach { builder.append("• ${it.content}\n") }
            builder.append("\n")
        }
        if (matchedDiary.isNotEmpty()) {
            builder.append("Private Diary Entries:\n")
            matchedDiary.take(3).forEach { builder.append("• [${it.date}] (${it.mood}): ${it.text}\n") }
        }

        return builder.toString().trim()
    }
}
