package com.Lia.assistant.ui.fx

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Lia.assistant.agent.OneShotProgressHolder
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.LocalNovaReducedMotion
import com.Lia.assistant.voice.NovaOrbState
import kotlin.math.cos
import kotlin.math.sin

enum class NovaOrbStyle(val displayName: String) {
    NOVA("Nova Pearl"),
    AURORA("Aurora Borealis"),
    PLASMA("Plasma Core"),
    GLASS("Glass Sphere"),
    ENERGY("Energy Pulse"),
    MINIMAL("Minimal"),
    ARCHER("Golden Archer");

    companion object {
        fun fromString(name: String): NovaOrbStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NOVA
        }
    }
}

@Composable
fun NovaOrb(
    state: NovaOrbState,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    amplitude: Float = 0f,
    style: NovaOrbStyle = NovaOrbStyle.NOVA,
    assistantName: String = "Lia"
) {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current

    val transition = rememberInfiniteTransition(label = "OrbMotion")

    // Breathing: 4.2 s sine (+-3.5%)
    val breathProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Breath"
    )

    val liveProgress by OneShotProgressHolder.liveProgressText.collectAsState()
    val isExecutingOneShot by OneShotProgressHolder.isExecuting.collectAsState()

    val effectiveState = if (isExecutingOneShot) NovaOrbState.THINKING else state

    // Rotation: 9s default; 2.6s thinking; 1.1s connecting; 3s watching
    val rotDuration = when (effectiveState) {
        NovaOrbState.CONNECTING -> 1100
        NovaOrbState.THINKING -> 2600
        NovaOrbState.WATCHING -> 3200
        else -> 9000
    }

    val rotationProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Palette per state
    val (coreColor, rimColor, haloColor) = when (effectiveState) {
        NovaOrbState.IDLE -> Triple(Color(0xFF9C27B0), colors.lotus, Color(0x33FF6FAE))
        NovaOrbState.LISTENING -> Triple(Color(0xFF00838F), colors.lagoon, Color(0x403FE0D0))
        NovaOrbState.THINKING -> Triple(Color(0xFF5E35B1), Color(0xFF7C4DFF), Color(0x407C4DFF))
        NovaOrbState.SPEAKING -> Triple(colors.accentSecondary, colors.accent, colors.accentGlow)
        NovaOrbState.CONNECTING -> Triple(Color(0xFFE65100), colors.accent, Color(0x33FFB547))
        NovaOrbState.WATCHING -> Triple(Color(0xFF00838F), Color(0xFF00E5FF), Color(0x4000E5FF))
        NovaOrbState.ERROR -> Triple(Color(0xFFB71C1C), colors.error, Color(0x40FF5252))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .semantics {
                    contentDescription = "$assistantName status: ${effectiveState.name.lowercase()}"
                }
        ) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        val breathScale = if (reducedMotion) 1f else 1f + 0.035f * sin(breathProgress * 2 * Math.PI).toFloat()
        val ampDisplacement = (amplitude.coerceIn(0f, 1f) * 0.15f)
        val currentRadius = radius * 0.70f * (breathScale + ampDisplacement)

        // 1. Radial outer halo glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(haloColor, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // 2. State-specific background rings (Listening expanding rings)
        if (state == NovaOrbState.LISTENING && !reducedMotion) {
            val ringProgress = (breathProgress * 2f) % 1f
            drawCircle(
                color = colors.lagoon.copy(alpha = (1f - ringProgress) * 0.5f),
                radius = currentRadius + (radius - currentRadius) * ringProgress,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 3. Connecting 100-degree arc
        if (state == NovaOrbState.CONNECTING) {
            drawArc(
                color = colors.accent,
                startAngle = if (reducedMotion) 0f else rotationProgress,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - currentRadius * 1.15f, center.y - currentRadius * 1.15f),
                size = Size(currentRadius * 2.3f, currentRadius * 2.3f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // 4. ARCHER Style golden spiral particles
        if (style == NovaOrbStyle.ARCHER) {
            drawArcherSphere(center, currentRadius, rotationProgress, amplitude, colors)
        } else {
            // Radial gradient core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(rimColor, coreColor, coreColor.copy(alpha = 0.8f)),
                    center = center - Offset(currentRadius * 0.25f, currentRadius * 0.25f),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            // Orbiting particles
            val particleCount = when (style) {
                NovaOrbStyle.ENERGY -> 8
                NovaOrbStyle.PLASMA -> 6
                NovaOrbStyle.GLASS -> 3
                NovaOrbStyle.MINIMAL -> 0
                else -> if (state == NovaOrbState.ERROR) 0 else 5
            }

            if (particleCount > 0 && !reducedMotion) {
                for (i in 0 until particleCount) {
                    val angleDeg = (rotationProgress + (i * 360f / particleCount)) % 360f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val orbitDist = currentRadius * 0.85f
                    val px = center.x + (orbitDist * cos(angleRad)).toFloat()
                    val py = center.y + (orbitDist * sin(angleRad) * 0.45f).toFloat()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            // Glass highlight arc
            if (style == NovaOrbStyle.GLASS) {
                drawArc(
                    color = Color.White.copy(alpha = 0.65f),
                    startAngle = 200f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - currentRadius * 0.85f, center.y - currentRadius * 0.85f),
                    size = Size(currentRadius * 1.7f, currentRadius * 1.7f),
                    style = Stroke(width = 3.5.dp.toPx())
                )
            }
        }

        // Error pulse ring
        if (effectiveState == NovaOrbState.ERROR) {
            drawCircle(
                color = colors.error.copy(alpha = 0.7f),
                radius = currentRadius * 1.1f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }

    if (liveProgress != null) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xE60F0B26),
            border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.85f)),
            shadowElevation = 8.dp,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = liveProgress!!,
                color = Color.White,
                fontSize = if (size > 80.dp) 15.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
}

private fun DrawScope.drawArcherSphere(
    center: Offset,
    radius: Float,
    rotation: Float,
    amplitude: Float,
    colors: com.Lia.assistant.ui.theme.NovaColorScheme
) {
    val count = 280
    val phi = (1 + kotlin.math.sqrt(5.0)) / 2.0
    val rotRad = Math.toRadians(rotation.toDouble())

    for (i in 0 until count) {
        val y = 1.0 - (i.toDouble() / (count - 1)) * 2.0 // -1 to 1
        val radiusAtY = kotlin.math.sqrt(1.0 - y * y)
        val theta = 2.0 * Math.PI * i / phi + rotRad

        val x = kotlin.math.cos(theta) * radiusAtY
        val z = kotlin.math.sin(theta) * radiusAtY

        val depthAlpha = ((z + 1.0) / 2.0).coerceIn(0.15, 1.0).toFloat()
        val particleRadius = (1.5f + (z.toFloat() * 1.0f) + (amplitude * 1.2f)).coerceAtLeast(0.8f)

        val px = center.x + (x * radius).toFloat()
        val py = center.y + (y * radius).toFloat()

        drawCircle(
            color = if (z > 0) colors.accent.copy(alpha = depthAlpha) else colors.lotus.copy(alpha = depthAlpha),
            radius = particleRadius,
            center = Offset(px, py)
        )
    }
}
