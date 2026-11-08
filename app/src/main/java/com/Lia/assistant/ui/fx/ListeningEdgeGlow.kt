package com.Lia.assistant.ui.fx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.Lia.assistant.voice.NovaOrbState
import com.Lia.assistant.voice.VoiceSessionManager

@Composable
fun ListeningEdgeGlow(modifier: Modifier = Modifier) {
    val isActive by VoiceSessionManager.isActive.collectAsState()
    val state by VoiceSessionManager.state.collectAsState()

    AnimatedVisibility(
        visible = isActive && state != NovaOrbState.IDLE,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val glowColor = when (state) {
            NovaOrbState.SPEAKING -> Color(0xFFFF6FAE) // Pink
            NovaOrbState.ERROR -> Color(0xFFFF7A85) // Red
            else -> Color(0xFF8B5CF6) // Violet resting/listening
        }

        Canvas(modifier = modifier.fillMaxSize()) {
            val thickness = 26.dp.toPx()
            val w = size.width
            val h = size.height

            // Top edge
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(glowColor.copy(alpha = 0.6f), Color.Transparent),
                    startY = 0f,
                    endY = thickness
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, thickness)
            )

            // Bottom edge
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, glowColor.copy(alpha = 0.6f)),
                    startY = h - thickness,
                    endY = h
                ),
                topLeft = Offset(0f, h - thickness),
                size = Size(w, thickness)
            )

            // Left edge
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(glowColor.copy(alpha = 0.6f), Color.Transparent),
                    startX = 0f,
                    endX = thickness
                ),
                topLeft = Offset(0f, 0f),
                size = Size(thickness, h)
            )

            // Right edge
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, glowColor.copy(alpha = 0.6f)),
                    startX = w - thickness,
                    endX = w
                ),
                topLeft = Offset(w - thickness, 0f),
                size = Size(thickness, h)
            )
        }
    }
}
