package com.Lia.assistant.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Random

val LocalBottomBarInset = compositionLocalOf { 80.dp }

fun Modifier.nightSky(): Modifier = this.drawBehind {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF15113A),
            Color(0xFF0C0924),
            Color(0xFF06050E)
        )
    )
    drawRect(brush = gradient)

    // Deterministic sparse star field
    val random = Random(42)
    val width = size.width
    val height = size.height

    for (i in 0 until 40) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * height
        val radius = 0.8f + random.nextFloat() * 1.4f
        val alpha = 0.15f + random.nextFloat() * 0.45f
        drawCircle(
            color = Color(0xFFEEE9FF).copy(alpha = alpha),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

fun Modifier.depthSurface(
    shape: Shape,
    elevation: Dp = 4.dp,
    rimColor: Color = Color(0x26FFFFFF)
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x6606050E),
        spotColor = Color(0x8015113A)
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                rimColor,
                rimColor.copy(alpha = 0.05f)
            )
        ),
        shape = shape
    )
