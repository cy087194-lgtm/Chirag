package com.Lia.assistant.agent.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionVerificationEngineTest {

    @Test
    fun testTorchVerificationMatches() {
        val resultOn = ActionVerificationEngine.verifyTorch(isExpectedOn = true, isReportedOn = true)
        assertEquals(VerificationStatus.SUCCESS_VERIFIED, resultOn.status)

        val resultMismatch = ActionVerificationEngine.verifyTorch(isExpectedOn = true, isReportedOn = false)
        assertEquals(VerificationStatus.FAILED, resultMismatch.status)
    }

    @Test
    fun testVolumeVerification() {
        val resUp = ActionVerificationEngine.verifyVolume(before = 5, after = 7, direction = "up")
        assertEquals(VerificationStatus.SUCCESS_VERIFIED, resUp.status)

        val resDown = ActionVerificationEngine.verifyVolume(before = 5, after = 3, direction = "down")
        assertEquals(VerificationStatus.SUCCESS_VERIFIED, resDown.status)
    }

    @Test
    fun testAppForegroundVerification() {
        val match = ActionVerificationEngine.verifyAppForeground(
            currentPkg = "com.whatsapp",
            expectedPkg = "com.whatsapp"
        )
        assertEquals(VerificationStatus.SUCCESS_VERIFIED, match.status)

        val mismatch = ActionVerificationEngine.verifyAppForeground(
            currentPkg = "com.android.settings",
            expectedPkg = "com.whatsapp"
        )
        assertEquals(VerificationStatus.FAILED, mismatch.status)
    }

    @Test
    fun testMessageDeliveryStatesNeverInterchangeable() {
        // State 1: Both sent and found on screen -> SENT
        val sentRes = ActionVerificationEngine.verifyMessageState(
            screenTexts = listOf("Keshav", "Hi", "Online"),
            sentText = "Hi",
            sendButtonTapped = true
        )
        assertEquals(MessageDeliveryStatus.SENT, sentRes.deliveryStatus)
        assertTrue(sentRes.isConfirmed)

        // State 2: Send tapped but not yet read in bubble -> SUBMITTED
        val submittedRes = ActionVerificationEngine.verifyMessageState(
            screenTexts = listOf("Keshav", "Online"),
            sentText = "Hi",
            sendButtonTapped = true
        )
        assertEquals(MessageDeliveryStatus.SUBMITTED, submittedRes.deliveryStatus)
        assertFalse(submittedRes.isConfirmed)

        // State 3: Text typed in input field but send was not tapped -> DRAFTED
        val draftedRes = ActionVerificationEngine.verifyMessageState(
            screenTexts = listOf("Keshav", "Hi", "Type a message"),
            sentText = "Hi",
            sendButtonTapped = false
        )
        assertEquals(MessageDeliveryStatus.DRAFTED, draftedRes.deliveryStatus)
        assertFalse(draftedRes.isConfirmed)

        // State 4: Neither text nor send button found -> UNVERIFIED
        val unverifiedRes = ActionVerificationEngine.verifyMessageState(
            screenTexts = listOf("Chat list", "Status", "Calls"),
            sentText = "Hi",
            sendButtonTapped = false
        )
        assertEquals(MessageDeliveryStatus.UNVERIFIED, unverifiedRes.deliveryStatus)
        assertFalse(unverifiedRes.isConfirmed)
    }
}
