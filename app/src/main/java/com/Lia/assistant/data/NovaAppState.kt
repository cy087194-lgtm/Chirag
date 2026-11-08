package com.Lia.assistant.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ConversationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val preview: String,
    val timestamp: Long = System.currentTimeMillis()
)

class NovaAppState(context: Context) {
    private val ioScope = CoroutineScope(Dispatchers.IO)

    var themeMode by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.THEME_MODE, "DARK")
    )
        private set

    var reducedMotion by mutableStateOf(
        NovaPreferences.getBoolean(context, NovaPreferences.Keys.REDUCED_MOTION, false)
    )
        private set

    var orbStyle by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.ORB_STYLE, "NOVA")
    )
        private set

    var personality by mutableStateOf(
        Personality.fromString(
            NovaPreferences.getString(
                context,
                NovaPreferences.Keys.PERSONALITY,
                Personality.NORMAL.name
            )
        )
    )
        private set

    var assistantName by mutableStateOf(
        NovaPreferences.getString(
            context,
            NovaPreferences.Keys.ASSISTANT_NAME,
            AssistantBrand.NAME
        )
    )
        private set

    var userName by mutableStateOf(
        NovaPreferences.getString(
            context,
            NovaPreferences.Keys.USER_NAME,
            "Friend"
        )
    )
        private set

    var language by mutableStateOf(
        LanguagePreference.fromString(
            NovaPreferences.getString(
                context,
                NovaPreferences.Keys.LANGUAGE,
                LanguagePreference.AUTO.name
            )
        )
    )
        private set

    var voiceName by mutableStateOf(
        NovaPreferences.getString(
            context,
            NovaPreferences.Keys.VOICE_NAME,
            "Aoede"
        )
    )
        private set

    var speechSpeed by mutableFloatStateOf(
        NovaPreferences.getFloat(
            context,
            NovaPreferences.Keys.SPEECH_SPEED,
            1.0f
        )
    )
        private set

    var voiceVolume by mutableFloatStateOf(
        NovaPreferences.getFloat(
            context,
            NovaPreferences.Keys.VOICE_VOLUME,
            1.0f
        )
    )
        private set

    val conversations = mutableStateListOf<ConversationItem>()

    fun applyThemeMode(context: Context, mode: String) {
        themeMode = mode
        NovaPreferences.putString(context, NovaPreferences.Keys.THEME_MODE, mode)
    }

    fun applyReducedMotion(context: Context, enabled: Boolean) {
        reducedMotion = enabled
        NovaPreferences.putBoolean(context, NovaPreferences.Keys.REDUCED_MOTION, enabled)
    }

    fun applyOrbStyle(context: Context, style: String) {
        orbStyle = style
        NovaPreferences.putString(context, NovaPreferences.Keys.ORB_STYLE, style)
    }

    fun applyPersonality(context: Context, p: Personality) {
        personality = p
        NovaPreferences.putString(context, NovaPreferences.Keys.PERSONALITY, p.name)
        ioScope.launch {
            PersonalityRepository.setPersonality(context, p)
        }
    }

    fun applyAssistantName(context: Context, name: String) {
        val trimmed = name.trim().ifEmpty { AssistantBrand.NAME }
        assistantName = trimmed
        NovaPreferences.putString(context, NovaPreferences.Keys.ASSISTANT_NAME, trimmed)
        ioScope.launch {
            PersonalityRepository.setAssistantName(context, trimmed)
        }
    }

    fun applyUserName(context: Context, name: String) {
        val trimmed = name.trim().ifEmpty { "Friend" }
        userName = trimmed
        NovaPreferences.putString(context, NovaPreferences.Keys.USER_NAME, trimmed)
    }

    fun applyLanguage(context: Context, lang: LanguagePreference) {
        language = lang
        NovaPreferences.putString(context, NovaPreferences.Keys.LANGUAGE, lang.name)
        ioScope.launch {
            PersonalityRepository.setLanguage(context, lang)
        }
    }

    fun applyVoiceName(context: Context, voice: String) {
        val trimmed = voice.trim().ifEmpty { "Aoede" }
        voiceName = trimmed
        NovaPreferences.putString(context, NovaPreferences.Keys.VOICE_NAME, trimmed)
        ioScope.launch {
            PersonalityRepository.setVoiceName(context, trimmed)
        }
    }

    fun applySpeechSpeed(context: Context, speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        speechSpeed = clamped
        NovaPreferences.putFloat(context, NovaPreferences.Keys.SPEECH_SPEED, clamped)
    }

    fun applyVoiceVolume(context: Context, volume: Float) {
        val clamped = volume.coerceIn(0.1f, 1.0f)
        voiceVolume = clamped
        NovaPreferences.putFloat(context, NovaPreferences.Keys.VOICE_VOLUME, clamped)
    }

    fun addConversation(item: ConversationItem) {
        conversations.add(0, item)
    }

    fun clearConversations() {
        conversations.clear()
    }
}
