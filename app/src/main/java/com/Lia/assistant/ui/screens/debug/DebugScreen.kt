package com.Lia.assistant.ui.screens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.ui.components.NovaGlassCard
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.nightSky
import com.Lia.assistant.voice.VoiceLatencyTracker
import com.Lia.assistant.voice.VoiceSessionManager

@Composable
fun DebugScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current

    val state by VoiceSessionManager.state.collectAsState()
    val personality by VoiceSessionManager.currentPersonality.collectAsState()
    val configuredLang by VoiceSessionManager.currentLanguage.collectAsState()
    val detectedLang by VoiceSessionManager.detectedLanguage.collectAsState()
    val vadActive by VoiceSessionManager.vadActive.collectAsState()
    val muted by VoiceSessionManager.muted.collectAsState()
    val reconnectCount by VoiceLatencyTracker.reconnectCount.collectAsState()
    val lastLatency by VoiceLatencyTracker.lastTurnLatencyMs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = "Voice Pipeline Debugger",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DebugRow("Live Connection State", state.name)
                DebugRow("Active Personality", personality.displayName)
                DebugRow("Configured Language", configuredLang.displayName)
                DebugRow("Detected Spoken Language", detectedLang)
                DebugRow("Voice Activity Detection (VAD)", if (vadActive) "ACTIVE" else "SILENT")
                DebugRow("Microphone State", if (muted) "MUTED" else "UNMUTED")
                DebugRow("Last Turn Latency", "$lastLatency ms")
                DebugRow("Session Reconnects", "$reconnectCount")
            }
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    val colors = LocalNovaColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = colors.textSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.accent)
    }
}
