package com.Lia.assistant.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val category: String = "note",
    val timestamp: Long = System.currentTimeMillis()
)

class UserMemoryStore private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun saveMemory(content: String, category: String = "note"): MemoryItem {
        val trimmed = content.trim()
        val memories = getAllMemories().toMutableList()
        // If an identical memory exists, update its timestamp
        val existingIndex = memories.indexOfFirst { it.content.equals(trimmed, ignoreCase = true) }
        val item = if (existingIndex >= 0) {
            memories[existingIndex].copy(timestamp = System.currentTimeMillis(), category = category)
        } else {
            MemoryItem(content = trimmed, category = category)
        }

        if (existingIndex >= 0) {
            memories[existingIndex] = item
        } else {
            memories.add(0, item)
        }

        persistMemories(memories)
        return item
    }

    @Synchronized
    fun getAllMemories(): List<MemoryItem> {
        val raw = prefs.getString(KEY_MEMORIES, "[]") ?: "[]"
        val list = mutableListOf<MemoryItem>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MemoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        content = obj.optString("content", ""),
                        category = obj.optString("category", "note"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    @Synchronized
    fun deleteMemory(id: String): Boolean {
        val memories = getAllMemories().toMutableList()
        val removed = memories.removeAll { it.id == id }
        if (removed) {
            persistMemories(memories)
        }
        return removed
    }

    @Synchronized
    fun forgetMatching(query: String): Int {
        val q = query.trim().lowercase()
        val memories = getAllMemories().toMutableList()
        val countBefore = memories.size
        memories.removeAll { it.content.lowercase().contains(q) }
        val countRemoved = countBefore - memories.size
        if (countRemoved > 0) {
            persistMemories(memories)
        }
        return countRemoved
    }

    @Synchronized
    fun clearAll() {
        prefs.edit().remove(KEY_MEMORIES).apply()
    }

    fun getMemoriesSummary(): String {
        val list = getAllMemories()
        if (list.isEmpty()) return "No memories saved yet."
        return list.mapIndexed { index, item -> "${index + 1}. ${item.content}" }.joinToString("\n")
    }

    private fun persistMemories(list: List<MemoryItem>) {
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("content", item.content)
                put("category", item.category)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_MEMORIES, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "lia_user_memories"
        private const val KEY_MEMORIES = "saved_memories_json"

        @Volatile
        private var INSTANCE: UserMemoryStore? = null

        fun get(context: Context): UserMemoryStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserMemoryStore(context).also { INSTANCE = it }
            }
        }
    }
}
