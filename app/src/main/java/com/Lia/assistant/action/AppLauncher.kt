package com.Lia.assistant.action

import android.content.Context
import android.content.Intent
import kotlin.math.abs

object AppLauncher {

    private val KNOWN_APP_ALIASES = mapOf(
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "youtube" to listOf("com.google.android.youtube"),
        "chrome" to listOf("com.android.chrome"),
        "google" to listOf("com.google.android.googlequicksearchbox"),
        "settings" to listOf("com.android.settings"),
        "setting" to listOf("com.android.settings"),
        "camera" to listOf("com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2"),
        "maps" to listOf("com.google.android.apps.maps"),
        "gallery" to listOf("com.google.android.apps.photos", "com.android.gallery3d", "com.android.gallery"),
        "photos" to listOf("com.google.android.apps.photos"),
        "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "alarm" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "gmail" to listOf("com.google.android.gm"),
        "mail" to listOf("com.google.android.gm"),
        "playstore" to listOf("com.android.vending"),
        "play store" to listOf("com.android.vending"),
        "phone" to listOf("com.google.android.dialer", "com.android.dialer"),
        "dialer" to listOf("com.google.android.dialer", "com.android.dialer"),
        "messages" to listOf("com.google.android.apps.messaging", "com.android.mms"),
        "message" to listOf("com.google.android.apps.messaging", "com.android.mms"),
        "sms" to listOf("com.google.android.apps.messaging", "com.android.mms"),
        "telegram" to listOf("org.telegram.messenger"),
        "instagram" to listOf("com.instagram.android"),
        "spotify" to listOf("com.spotify.music"),
        "files" to listOf("com.google.android.apps.nbu.files")
    )

    fun cleanQuery(rawQuery: String): String {
        var q = rawQuery.trim().lowercase()
        // Strip common Hindi & English action words
        val commandWords = listOf(
            "khol", "kholo", "khol do", "kholna", "chalao", "chala do",
            "open", "launch", "start", "pe jao", "par jao", "me jao", "chalu karo", "app"
        )
        for (w in commandWords) {
            q = q.replace(Regex("\\b${Regex.escape(w)}\\b"), "").trim()
        }
        return q.ifBlank { rawQuery.trim().lowercase() }
    }

    /**
     * Resolves an app query against a map of lowercase app labels to package names.
     */
    fun findApp(query: String, apps: Map<String, String>): String? {
        val q = cleanQuery(query)
        if (q.isEmpty()) return null

        // 1. Exact match in installed apps
        if (apps.containsKey(q)) {
            return apps[q]
        }

        val queryWords = q.split(Regex("\\s+")).filter { it.isNotBlank() }
        val candidates = mutableListOf<String>()

        for (label in apps.keys) {
            if (label.length < 3) continue

            val startsWith = label.startsWith(q) || q.startsWith(label)
            if (startsWith) {
                candidates.add(label)
                continue
            }

            // Word-by-word prefix match
            val labelWords = label.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (queryWords.isNotEmpty() && labelWords.isNotEmpty()) {
                val allQueryWordsMatch = queryWords.all { qw ->
                    labelWords.any { lw -> lw.startsWith(qw) }
                }
                if (allQueryWordsMatch) {
                    candidates.add(label)
                }
            }
        }

        if (candidates.isNotEmpty()) {
            val bestLabel = candidates.minByOrNull { abs(it.length - q.length) }
            if (bestLabel != null) return apps[bestLabel]
        }

        // 2. Fallback to known package aliases (exact or strong prefix match)
        for ((alias, packages) in KNOWN_APP_ALIASES) {
            if (q == alias || (q.length >= 4 && alias.startsWith(q)) || (alias.length >= 4 && q.startsWith(alias))) {
                return packages.firstOrNull()
            }
        }

        return null
    }

    fun launch(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }

            // Special fallback for Settings
            if (packageName == "com.android.settings") {
                val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return true
            }

            // Special fallback for Camera
            if (packageName.contains("camera")) {
                val cameraIntent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(cameraIntent)
                return true
            }

            false
        } catch (_: Exception) {
            false
        }
    }
}
