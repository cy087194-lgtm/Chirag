package com.Lia.assistant.agent.core

enum class VerificationStatus {
    SUCCESS_VERIFIED,
    SUCCESS_UNVERIFIED,
    PARTIAL_SUCCESS,
    FAILED,
    BLOCKED_BY_PERMISSION,
    UNSUPPORTED,
    NEEDS_USER_ACTION
}

enum class MessageDeliveryStatus {
    DRAFTED,
    SUBMITTED,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    UNVERIFIED
}

data class VerificationResult(
    val status: VerificationStatus,
    val evidence: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class MessageVerificationResult(
    val deliveryStatus: MessageDeliveryStatus,
    val evidence: String,
    val isConfirmed: Boolean
)

object ActionVerificationEngine {

    /**
     * Verifies whether an app is genuinely running in the foreground.
     */
    fun verifyAppForeground(currentPkg: String, expectedPkg: String): VerificationResult {
        return if (currentPkg.equals(expectedPkg, ignoreCase = true) || currentPkg.contains(expectedPkg, ignoreCase = true)) {
            VerificationResult(
                status = VerificationStatus.SUCCESS_VERIFIED,
                evidence = "Foreground package verified: $currentPkg matches $expectedPkg"
            )
        } else if (currentPkg.isNotBlank()) {
            VerificationResult(
                status = VerificationStatus.FAILED,
                evidence = "Foreground package is $currentPkg, expected $expectedPkg"
            )
        } else {
            VerificationResult(
                status = VerificationStatus.SUCCESS_UNVERIFIED,
                evidence = "Package launch intent was dispatched, but active window package could not be verified by accessibility"
            )
        }
    }

    /**
     * Verifies message submission and delivery on screen without conflating states.
     * Distinct states: DRAFTED, SUBMITTED, SENT, DELIVERED, READ, FAILED, UNVERIFIED.
     */
    fun verifyMessageState(
        screenTexts: List<String>,
        sentText: String,
        sendButtonTapped: Boolean
    ): MessageVerificationResult {
        val fullText = screenTexts.joinToString(" ")
        val textFound = fullText.contains(sentText, ignoreCase = true)

        return when {
            textFound && sendButtonTapped -> {
                MessageVerificationResult(
                    deliveryStatus = MessageDeliveryStatus.SENT,
                    evidence = "Message text \"${sentText.take(20)}\" verified present in conversation stream after send tap",
                    isConfirmed = true
                )
            }
            sendButtonTapped -> {
                MessageVerificationResult(
                    deliveryStatus = MessageDeliveryStatus.SUBMITTED,
                    evidence = "Send action was performed, but message receipt could not be read from screen nodes",
                    isConfirmed = false
                )
            }
            textFound -> {
                MessageVerificationResult(
                    deliveryStatus = MessageDeliveryStatus.DRAFTED,
                    evidence = "Text was entered into input field, but send button was not tapped",
                    isConfirmed = false
                )
            }
            else -> {
                MessageVerificationResult(
                    deliveryStatus = MessageDeliveryStatus.UNVERIFIED,
                    evidence = "Neither input field nor sent bubble verified on current screen",
                    isConfirmed = false
                )
            }
        }
    }

    /**
     * Verifies hardware torch state against expected outcome.
     */
    fun verifyTorch(isExpectedOn: Boolean, isReportedOn: Boolean): VerificationResult {
        return if (isExpectedOn == isReportedOn) {
            VerificationResult(
                status = VerificationStatus.SUCCESS_VERIFIED,
                evidence = "Torch CameraManager reports state = ${if (isReportedOn) "ON" else "OFF"}"
            )
        } else {
            VerificationResult(
                status = VerificationStatus.FAILED,
                evidence = "Torch state mismatch: expected $isExpectedOn, actual $isReportedOn"
            )
        }
    }

    /**
     * Verifies volume change by comparing before and after values.
     */
    fun verifyVolume(before: Int, after: Int, direction: String): VerificationResult {
        val expectedChange = when (direction.lowercase()) {
            "up" -> after > before || after == 15 // max
            "down" -> after < before || after == 0 // min
            "mute" -> after == 0
            else -> after != before
        }

        return if (expectedChange) {
            VerificationResult(
                status = VerificationStatus.SUCCESS_VERIFIED,
                evidence = "Audio stream index changed from $before to $after"
            )
        } else {
            VerificationResult(
                status = VerificationStatus.SUCCESS_UNVERIFIED,
                evidence = "Audio stream index unchanged ($after); could be at limit"
            )
        }
    }
}
