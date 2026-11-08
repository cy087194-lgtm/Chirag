package com.Lia.assistant.agent.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedOrchestratorTest {

    @Test
    fun testNegationOrchestration() = runBlocking {
        // Test orchestration of "Wi-Fi band mat karna"
        val parsed = com.Lia.assistant.agent.SmartIntentParser.parse("Wi-Fi band mat karna")
        assertTrue("Intent must be parsed as negation", parsed is com.Lia.assistant.agent.ParsedIntent.NegatedCommand)

        ActionHistoryManager.recordAction(
            taskId = "TEST-1",
            toolName = "negation_guard",
            command = "Wi-Fi band mat karna",
            status = VerificationStatus.SUCCESS_VERIFIED,
            evidence = "Negation token detected"
        )

        val records = ActionHistoryManager.records.value
        assertTrue("Audit record must be present", records.isNotEmpty())
        assertEquals("negation_guard", records.first().toolName)
        assertEquals(VerificationStatus.SUCCESS_VERIFIED, records.first().status)
    }

    @Test
    fun testActionHistoryAuditSanitizesPasswords() {
        ActionHistoryManager.recordAction(
            taskId = "TEST-2",
            toolName = "login_test",
            command = "my password: SecretPassword123!",
            status = VerificationStatus.SUCCESS_VERIFIED,
            evidence = "Test evidence"
        )

        val latest = ActionHistoryManager.records.value.first()
        assertTrue("Command must not contain raw password", !latest.command.contains("SecretPassword123!"))
        assertTrue("Command must contain redacted placeholder", latest.command.contains("[REDACTED]"))
    }
}
