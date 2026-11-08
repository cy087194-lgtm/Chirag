package com.Lia.assistant.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object InstalledAppLabelCache {
    private const val TTL_MS = 15 * 60 * 1000L // 15-minute TTL

    private val cache = mutableMapOf<String, String>() // lowercase label -> package
    private var lastUpdateTime = 0L
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun prewarm(context: Context) {
        scope.launch {
            get(context)
        }
    }

    suspend fun get(context: Context): Map<String, String> = mutex.withLock {
        val now = System.currentTimeMillis()
        if (cache.isNotEmpty() && (now - lastUpdateTime) < TTL_MS) {
            return HashMap(cache)
        }

        cache.clear()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        try {
            val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in activities) {
                val label = resolveInfo.loadLabel(pm)?.toString()?.trim() ?: continue
                val pkg = resolveInfo.activityInfo.packageName
                if (label.isNotEmpty() && pkg.isNotEmpty()) {
                    cache[label.lowercase()] = pkg
                }
            }
            lastUpdateTime = now
        } catch (_: Exception) {}

        return HashMap(cache)
    }

    // For testing and direct seeding
    fun setCacheForTest(testMap: Map<String, String>) {
        cache.clear()
        cache.putAll(testMap)
        lastUpdateTime = System.currentTimeMillis()
    }

    fun clear() {
        cache.clear()
        lastUpdateTime = 0L
    }
}
