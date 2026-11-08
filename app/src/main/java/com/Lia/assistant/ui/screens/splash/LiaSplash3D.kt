package com.Lia.assistant.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Lia.assistant.data.AssistantBrand
import com.Lia.assistant.ui.fx.LiaOrb3D
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.LocalNovaReducedMotion
import com.Lia.assistant.ui.theme.SerifFontFamily
import com.Lia.assistant.voice.NovaOrbState
import kotlinx.coroutines.delay
import java.util.Random

@Composable
fun LiaSplash3D(onDismiss: () -> Unit) {
    if (SplashSession.played) {
        onDismiss()
        return
    }

    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current

    val orbScale = remember { Animatable(0.2f) }
    val textAlpha = remember { Animatable(0f) }
    val containerAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            delay(500)
            SplashSession.played = true
            onDismiss()
            return@LaunchedEffect
        }

        // Orb condenses from centre
        orbScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )

        // Text fades in
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )

        delay(700)

        // Dissolve into Home
        containerAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400, easing = LinearEasing)
        )

        SplashSession.played = true
        onDismiss()
    }

    AnimatedVisibility(
        visible = containerAlpha.value > 0f,
        exit = fadeOut(tween(400))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .alpha(containerAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            // Star streaks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rand = Random(99)
                val w = size.width
                val h = size.height
                for (i in 0 until 50) {
                    val sx = rand.nextFloat() * w
                    val sy = rand.nextFloat() * h
                    val len = 10f + rand.nextFloat() * 25f
                    drawLine(
                        color = Color(0xFFEEE9FF).copy(alpha = 0.25f),
                        start = Offset(sx, sy),
                        end = Offset(sx, sy + len),
                        strokeWidth = 1.2f
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LiaOrb3D(
                    state = NovaOrbState.CONNECTING,
                    size = (180 * orbScale.value).dp,
                    amplitude = 0.2f
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = AssistantBrand.FULL_NAME,
                    fontFamily = SerifFontFamily,
                    fontSize = 32.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.alpha(textAlpha.value)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = AssistantBrand.TAGLINE,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.alpha(textAlpha.value)
                )
            }
        }
    }
}
