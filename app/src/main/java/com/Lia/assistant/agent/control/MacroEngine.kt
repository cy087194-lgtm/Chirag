package com.Lia.assistant.agent.control

import android.content.Context
import android.util.Log
import com.Lia.assistant.accessibility.LiaAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MacroAction(
    val type: String, // "tap", "type", "scroll"
    val target: String,
    val delayAfterMs: Long = 600L
)

object MacroEngine {

    private const val TAG = "MacroEngine"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var autoClickerJob: Job? = null

    private val recordedActions = mutableListOf<MacroAction>()
    private var isRecording = false

    // Text expander dictionary
    private val textExpanderMap = mutableMapOf(
        "addr" to "Flat 402, Green Valley Apartments, New Delhi 110001",
        "email" to "user@example.com",
        "omw" to "On my way! 5 minutes mein pahunch raha hoon.",
        "greetings" to "Namaste! Kaise hain aap?"
    )

    fun startRecording() {
        recordedActions.clear()
        isRecording = true
        Log.i(TAG, "Macro recording started")
    }

    fun recordAction(type: String, target: String) {
        if (isRecording) {
            recordedActions.add(MacroAction(type, target))
        }
    }

    fun stopRecording(): Int {
        isRecording = false
        Log.i(TAG, "Macro recording stopped: ${recordedActions.size} actions saved")
        return recordedActions.size
    }

    suspend fun replayMacro(): Boolean {
        val service = LiaAccessibilityService.instance ?: return false
        for (action in recordedActions) {
            when (action.type) {
                "tap" -> service.tapText(action.target)
                "type" -> service.typeText(action.target)
                "scroll" -> service.scroll(action.target)
            }
            delay(action.delayAfterMs)
        }
        return true
    }

    /**
     * Auto-Clicker: repeats tap at regular interval (e.g. "har 5 second mein is button par tap karo")
     */
    fun startAutoClicker(target: String, intervalSeconds: Int = 5): String {
        stopAutoClicker()
        val service = LiaAccessibilityService.instance ?: return "Accessibility Service enable nahi hai."

        autoClickerJob = scope.launch {
            Log.i(TAG, "Auto-clicker started for \"$target\" every $intervalSeconds seconds")
            while (isActive) {
                service.tapText(target)
                delay(intervalSeconds * 1000L)
            }
        }
        return "Auto-clicker start ho gaya: Har $intervalSeconds seconds mein \"$target\" par tap hoga."
    }

    fun stopAutoClicker(): String {
        autoClickerJob?.cancel()
        autoClickerJob = null
        return "Auto-clicker stop kar diya gaya hai."
    }

    fun expandText(shortcut: String): String? {
        return textExpanderMap[shortcut.trim().lowercase()]
    }

    fun addShortcut(shortcut: String, fullText: String) {
        textExpanderMap[shortcut.trim().lowercase()] = fullText.trim()
    }
}
