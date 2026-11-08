package com.Lia.assistant

import android.app.Application
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
        try {
            com.Lia.assistant.action.UniversalContactIndex.initialize(this)
        } catch (_: Throwable) {}
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                val timestamp = sdf.format(Date())
                val crashFile = File(getExternalFilesDir(null), "crash_$timestamp.txt")
                crashFile.writeText(buildString {
                    appendLine("=== Lia AI Crash Report ===")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Exception: ${throwable::class.qualifiedName}")
                    appendLine("Message: ${throwable.message}")
                    appendLine("Stacktrace:")
                    appendLine(throwable.stackTraceToString())
                    throwable.cause?.let {
                        appendLine("Caused by: ${it::class.qualifiedName}: ${it.message}")
                        appendLine(it.stackTraceToString())
                    }
                })
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
