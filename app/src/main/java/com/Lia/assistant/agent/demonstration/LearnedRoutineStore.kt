package com.Lia.assistant.agent.demonstration

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object LearnedRoutineStore {
    private const val TAG = "LearnedRoutineStore"
    private const val PREFS_NAME = "lia_learned_routines_prefs"
    private const val KEY_ROUTINES_JSON = "saved_routines_json"

    private val cachedRoutines = mutableListOf<LearnedRoutine>()
    private var isLoaded = false

    private val _routinesFlow = MutableStateFlow<List<LearnedRoutine>>(emptyList())
    val routinesFlow: StateFlow<List<LearnedRoutine>> = _routinesFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun initialize(context: Context) {
        if (isLoaded) return
        loadRoutines(context)
        isLoaded = true
    }

    fun get(context: Context): LearnedRoutineStore {
        initialize(context)
        return this
    }

    fun getAll(): List<LearnedRoutine> {
        return ArrayList(cachedRoutines)
    }

    @Synchronized
    fun loadRoutines(context: Context): List<LearnedRoutine> {
        cachedRoutines.clear()
        try {
            val jsonString = getPrefs(context).getString(KEY_ROUTINES_JSON, "[]") ?: "[]"
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                cachedRoutines.add(LearnedRoutine.fromJson(obj))
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load routines: ${e.message}", e)
        }
        _routinesFlow.value = ArrayList(cachedRoutines)
        return cachedRoutines
    }

    @Synchronized
    private fun persistRoutines(context: Context) {
        try {
            val array = JSONArray()
            cachedRoutines.forEach { array.put(it.toJson()) }
            getPrefs(context).edit().putString(KEY_ROUTINES_JSON, array.toString()).apply()
            _routinesFlow.value = ArrayList(cachedRoutines)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to persist routines: ${e.message}", e)
        }
    }

    @Synchronized
    fun saveRoutine(context: Context, routine: LearnedRoutine) {
        initialize(context)
        val idx = cachedRoutines.indexOfFirst { it.id == routine.id }
        if (idx >= 0) {
            cachedRoutines[idx] = routine
        } else {
            cachedRoutines.add(0, routine)
        }
        persistRoutines(context)
        Log.i(TAG, "Saved routine '${routine.name}' with trigger '${routine.trigger.phrase}'")
    }

    @Synchronized
    fun getAllRoutines(context: Context): List<LearnedRoutine> {
        initialize(context)
        return ArrayList(cachedRoutines)
    }

    @Synchronized
    fun getRoutineById(context: Context, id: String): LearnedRoutine? {
        initialize(context)
        return cachedRoutines.firstOrNull { it.id == id }
    }

    @Synchronized
    fun findRoutineByTrigger(context: Context, inputPhrase: String): LearnedRoutine? {
        initialize(context)
        val clean = inputPhrase.lowercase().trim()
        // 1. Direct match on trigger phrase
        val direct = cachedRoutines.firstOrNull { it.isEnabled && it.trigger.phrase.lowercase().trim() == clean }
        if (direct != null) return direct

        // 2. Contains match
        return cachedRoutines.firstOrNull {
            it.isEnabled && (clean.contains(it.trigger.phrase.lowercase().trim()) || it.name.lowercase().contains(clean))
        }
    }

    @Synchronized
    fun deleteRoutine(context: Context, id: String): Boolean {
        initialize(context)
        val removed = cachedRoutines.removeAll { it.id == id }
        if (removed) {
            persistRoutines(context)
        }
        return removed
    }

    @Synchronized
    fun updateRoutineEnabled(context: Context, id: String, enabled: Boolean) {
        initialize(context)
        val routine = cachedRoutines.firstOrNull { it.id == id }
        if (routine != null) {
            val updated = routine.copy(isEnabled = enabled)
            val idx = cachedRoutines.indexOf(routine)
            cachedRoutines[idx] = updated
            persistRoutines(context)
        }
    }

    @Synchronized
    fun updateRoutineVariables(context: Context, id: String, newVariables: Map<String, String>) {
        initialize(context)
        val routine = cachedRoutines.firstOrNull { it.id == id }
        if (routine != null) {
            val merged = routine.variables.toMutableMap().apply { putAll(newVariables) }
            val updated = routine.copy(variables = merged)
            val idx = cachedRoutines.indexOf(routine)
            cachedRoutines[idx] = updated
            persistRoutines(context)
        }
    }

    @Synchronized
    fun recordExecution(context: Context, id: String, success: Boolean, durationMs: Long) {
        initialize(context)
        val routine = cachedRoutines.firstOrNull { it.id == id }
        if (routine != null) {
            val updated = routine.copy(
                lastRunTime = System.currentTimeMillis(),
                lastRunDurationMs = durationMs,
                lastRunStatus = if (success) "SUCCESS" else "FAILED",
                successCount = if (success) routine.successCount + 1 else routine.successCount,
                failureCount = if (!success) routine.failureCount + 1 else routine.failureCount
            )
            val idx = cachedRoutines.indexOf(routine)
            cachedRoutines[idx] = updated
            persistRoutines(context)
        }
    }
}
