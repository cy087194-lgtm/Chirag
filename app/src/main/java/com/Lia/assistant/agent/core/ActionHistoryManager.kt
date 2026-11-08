package com.Lia.assistant.agent.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ActionAuditRecord(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val toolName: String,
    val command: String,
    val status: VerificationStatus,
    val evidence: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ActionHistoryManager {
    private const val MAX_RECORDS = 50

    private val _records = MutableStateFlow<List<ActionAuditRecord>>(emptyList())
    val records: StateFlow<List<ActionAuditRecord>> = _records.asStateFlow()

    @Synchronized
    fun recordAction(
        taskId: String,
        toolName: String,
        command: String,
        status: VerificationStatus,
        evidence: String
    ) {
        val newRecord = ActionAuditRecord(
            taskId = taskId,
            toolName = toolName,
            command = sanitizeCommand(command),
            status = status,
            evidence = evidence
        )
        val current = _records.value.toMutableList()
        current.add(0, newRecord)
        if (current.size > MAX_RECORDS) {
            _records.value = current.take(MAX_RECORDS)
        } else {
            _records.value = current
        }
    }

    @Synchronized
    fun clearHistory() {
        _records.value = emptyList()
    }

    /**
     * Strips any sensitive keywords or passwords from logged commands.
     */
    private fun sanitizeCommand(cmd: String): String {
        return cmd.replace(Regex("(?i)\\b(password|pin|otp|cvv|token)\\b[:=]?\\s*\\S+"), "[REDACTED]")
    }
}
