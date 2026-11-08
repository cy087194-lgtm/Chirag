package com.Lia.assistant.ui.screens.forge

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.Lia.assistant.forge.ForgeUiState
import com.Lia.assistant.ui.theme.LocalNovaColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ForgeFallbackScene(
    state: ForgeUiState,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val transition = rememberInfiniteTransition(label = "ForgeFallback")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rot"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.28f

        // Central energy core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.accent, colors.lagoon, Color.Transparent),
                center = center,
                radius = radius * 1.5f
            ),
            radius = radius * 1.5f,
            center = center
        )

        // Gyro rings
        for (i in 0..2) {
            val ringRadius = radius * (1.1f + i * 0.25f)
            val rotAngle = Math.toRadians((rotation + i * 60f).toDouble())
            drawCircle(
                color = if (i == 0) colors.accent else colors.lotus,
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Six stage markers around core
        val stageRadius = radius * 1.8f
        for (i in 0 until 6) {
            val theta = Math.toRadians((i * 60.0))
            val px = center.x + (stageRadius * cos(theta)).toFloat()
            val py = center.y + (stageRadius * sin(theta)).toFloat()
            val isActive = i <= state.stage.ordinal
            drawCircle(
                color = if (isActive) colors.accent else colors.surfaceBorder,
                radius = if (isActive) 8.dp.toPx() else 5.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}
