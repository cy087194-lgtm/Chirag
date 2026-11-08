package com.Lia.assistant.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonType
import com.Lia.assistant.ui.components.NovaVoiceVisualizer
import com.Lia.assistant.ui.fx.LiaOrb3D
import com.Lia.assistant.ui.fx.ListeningEdgeGlow
import com.Lia.assistant.ui.fx.NovaOrbStyle
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.SerifFontFamily
import com.Lia.assistant.ui.theme.depthSurface
import com.Lia.assistant.ui.theme.nightSky
import com.Lia.assistant.voice.NovaOrbState
import com.Lia.assistant.voice.VoiceForegroundService
import com.Lia.assistant.voice.VoiceSessionManager

@Composable
fun VoiceScreen(
    navController: NavController,
    appState: NovaAppState,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current

    val state by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()
    val muted by VoiceSessionManager.muted.collectAsState()
    val statusText by VoiceSessionManager.statusText.collectAsState()

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val orbSize = screenWidth * 0.70f

    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    BackHandler {
        try {
            VoiceForegroundService.stop(context)
        } catch (_: Throwable) {}
        navController.popBackStack()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                VoiceForegroundService.stop(context)
            } catch (_: Throwable) {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordAudioPermission = isGranted
        if (isGranted) {
            try {
                VoiceForegroundService.start(context)
            } catch (e: Throwable) {
                Log.e("VoiceScreen", "Failed to start VoiceForegroundService: ${e.message}")
            }
        }
    }

    val hasKey = ApiKeyStore.hasKey(context)

    LaunchedEffect(hasRecordAudioPermission, hasKey) {
        if (!hasRecordAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (hasKey && !VoiceSessionManager.isActive.value) {
            try {
                VoiceForegroundService.start(context)
            } catch (e: Throwable) {
                Log.e("VoiceScreen", "Failed to start VoiceForegroundService: ${e.message}")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
    ) {
        ListeningEdgeGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Dismiss/Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceGlass)
                        .clickable {
                            try {
                                VoiceForegroundService.stop(context)
                            } catch (_: Throwable) {}
                            navController.popBackStack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textPrimary
                    )
                }
            }

            // Center: 70% width 3D Orb & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!hasKey) {
                    LiaOrb3D(
                        state = NovaOrbState.ERROR,
                        size = orbSize,
                        amplitude = 0f,
                        assistantName = appState.assistantName
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "API Key Required",
                        fontFamily = SerifFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 20.sp,
                        color = colors.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaButton(
                        text = "Open Settings",
                        onClick = { navController.navigate("settings") },
                        type = NovaButtonType.PRIMARY
                    )
                } else if (!hasRecordAudioPermission) {
                    LiaOrb3D(
                        state = NovaOrbState.ERROR,
                        size = orbSize,
                        amplitude = 0f,
                        assistantName = appState.assistantName
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Microphone Permission Required",
                        fontFamily = SerifFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 18.sp,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaButton(
                        text = "Grant Permission",
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                } else {
                    Box(
                        modifier = Modifier.size(orbSize),
                        contentAlignment = Alignment.Center
                    ) {
                        LiaOrb3D(
                            state = state,
                            size = orbSize,
                            amplitude = amplitude,
                            style = NovaOrbStyle.fromString(appState.orbStyle),
                            assistantName = appState.assistantName
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = statusText,
                        fontFamily = SerifFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 20.sp,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state == NovaOrbState.SPEAKING) {
                        NovaVoiceVisualizer(
                            amplitude = amplitude,
                            barCount = 48,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }

            // Bottom Frosted Controls: Mute, Stop, Close
            val frostedShape = RoundedCornerShape(50)
            Row(
                modifier = Modifier
                    .depthSurface(shape = frostedShape, elevation = 4.dp)
                    .clip(frostedShape)
                    .background(colors.surfaceGlass)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mute toggle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (muted) colors.surfaceBorder else colors.surfaceRaised)
                        .clickable { VoiceSessionManager.toggleMute() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (muted) colors.error else colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Stop button (large, red-tinted)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B2535))
                        .clickable {
                            VoiceForegroundService.stop(context)
                            navController.popBackStack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Voice Session",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Close / dismiss button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceRaised)
                        .clickable {
                            try {
                                VoiceForegroundService.stop(context)
                            } catch (_: Throwable) {}
                            navController.popBackStack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Screen",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
