package com.Lia.assistant.voice

enum class MemoryRole {
    USER,
    ASSISTANT
}

data class MemoryTurn(
    val role: MemoryRole,
    val text: String
)

object ConversationMemory {
    private const val MAX_TURNS = 12
    private const val MAX_RECAP_CHARS = 1600

    private val turns = mutableListOf<MemoryTurn>()
    private val lock = Any()

    fun recordTurn(role: MemoryRole, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        synchronized(lock) {
            turns.add(MemoryTurn(role, trimmed))
            while (turns.size > MAX_TURNS) {
                turns.removeAt(0)
            }
        }
    }

    fun recap(): String? {
        synchronized(lock) {
            if (turns.isEmpty()) return null

            val sb = StringBuilder()
            for (turn in turns) {
                val label = if (turn.role == MemoryRole.USER) "User" else "You"
                sb.append("$label: ${turn.text}\n")
            }

            val fullText = sb.toString().trim()
            if (fullText.isEmpty()) return null

            return if (fullText.length > MAX_RECAP_CHARS) {
                fullText.takeLast(MAX_RECAP_CHARS)
            } else {
                fullText
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            turns.clear()
        }
    }

    fun size(): Int {
        synchronized(lock) {
            return turns.size
        }
    }
}
