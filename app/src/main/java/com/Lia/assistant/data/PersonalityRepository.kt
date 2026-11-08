package com.Lia.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.Lia.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.assistantDataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_settings")

object PersonalityRepository {
    private val KEY_PERSONALITY = stringPreferencesKey("assistant_personality")
    private val KEY_LANGUAGE = stringPreferencesKey("assistant_language")
    private val KEY_NAME = stringPreferencesKey("assistant_name")
    private val KEY_VOICE = stringPreferencesKey("assistant_voice")

    fun getPersonalityFlow(context: Context): Flow<Personality> {
        val fallback = NovaPreferences.getString(
            context,
            NovaPreferences.Keys.PERSONALITY,
            Personality.NORMAL.name
        )
        return context.assistantDataStore.data.map { prefs ->
            val stored = prefs[KEY_PERSONALITY] ?: fallback
            Personality.fromString(stored)
        }
    }

    suspend fun getPersonality(context: Context): Personality {
        return getPersonalityFlow(context).first()
    }

    suspend fun setPersonality(context: Context, personality: Personality) {
        NovaPreferences.putString(context, NovaPreferences.Keys.PERSONALITY, personality.name)
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_PERSONALITY] = personality.name
        }
        VoiceSessionManager.refreshInstructions(context)
    }

    fun getLanguageFlow(context: Context): Flow<LanguagePreference> {
        val fallback = NovaPreferences.getString(
            context,
            NovaPreferences.Keys.LANGUAGE,
            LanguagePreference.AUTO.name
        )
        return context.assistantDataStore.data.map { prefs ->
            val stored = prefs[KEY_LANGUAGE] ?: fallback
            LanguagePreference.fromString(stored)
        }
    }

    suspend fun getLanguage(context: Context): LanguagePreference {
        return getLanguageFlow(context).first()
    }

    suspend fun setLanguage(context: Context, language: LanguagePreference) {
        NovaPreferences.putString(context, NovaPreferences.Keys.LANGUAGE, language.name)
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.name
        }
        VoiceSessionManager.refreshInstructions(context)
    }

    fun getAssistantNameFlow(context: Context): Flow<String> {
        val fallback = NovaPreferences.getString(
            context,
            NovaPreferences.Keys.ASSISTANT_NAME,
            AssistantBrand.NAME
        )
        return context.assistantDataStore.data.map { prefs ->
            prefs[KEY_NAME] ?: fallback
        }
    }

    suspend fun getAssistantName(context: Context): String {
        return getAssistantNameFlow(context).first()
    }

    suspend fun setAssistantName(context: Context, name: String) {
        val trimmed = name.trim().ifEmpty { AssistantBrand.NAME }
        NovaPreferences.putString(context, NovaPreferences.Keys.ASSISTANT_NAME, trimmed)
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_NAME] = trimmed
        }
        VoiceSessionManager.refreshInstructions(context)
    }

    fun getVoiceNameFlow(context: Context): Flow<String> {
        val fallback = NovaPreferences.getString(
            context,
            NovaPreferences.Keys.VOICE_NAME,
            "Aoede"
        )
        return context.assistantDataStore.data.map { prefs ->
            prefs[KEY_VOICE] ?: fallback
        }
    }

    suspend fun getVoiceName(context: Context): String {
        return getVoiceNameFlow(context).first()
    }

    suspend fun setVoiceName(context: Context, voiceName: String) {
        val trimmed = voiceName.trim().ifEmpty { "Aoede" }
        NovaPreferences.putString(context, NovaPreferences.Keys.VOICE_NAME, trimmed)
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_VOICE] = trimmed
        }
        VoiceSessionManager.refreshInstructions(context)
    }
}
