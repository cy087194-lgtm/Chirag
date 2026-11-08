package com.Lia.assistant.license

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessKeyManager {
    private val _info = MutableStateFlow(LicenseInfo(active = true, plan = "Play", statusText = "Active"))
    val info: StateFlow<LicenseInfo> = _info.asStateFlow()

    fun refreshInfo(context: Context) {
        // Safe no-op in Play store build
    }

    suspend fun activate(context: Context, key: String): Pair<Boolean, String> {
        return Pair(true, "Play edition does not require activation")
    }

    suspend fun liveCheck(context: Context): Boolean {
        return true
    }

    fun isAccessGranted(): Boolean {
        return true
    }
}
