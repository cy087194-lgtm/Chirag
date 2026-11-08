package com.Lia.assistant.license

data class LicenseInfo(
    val active: Boolean = true,
    val plan: String = "Free",
    val expiresAt: Long = 0L,
    val statusText: String = "Active",
    val blocked: Boolean = false,
    val hasRememberedKey: Boolean = false
)
