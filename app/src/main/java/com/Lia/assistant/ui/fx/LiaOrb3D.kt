package com.Lia.assistant.ui.fx

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.LocalNovaReducedMotion
import com.Lia.assistant.voice.NovaOrbState

// Oppo/MediaTek GPU drivers crash with native SIGSEGV on RuntimeShader — keep disabled.
private val agslAvailable: Boolean = false

private const val AGSL_ORB_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iAmplitude;
    uniform float2 iTilt;
    uniform float3 iCoreColor;
    uniform float3 iRimColor;
    uniform float3 iHaloColor;

    float hash(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }

    float fbm(float2 p) {
        float v = 0.0;
        float a = 0.5;
        float2 shift = float2(100.0, 100.0);
        float2x2 rot = float2x2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
        for (int i = 0; i < 4; ++i) {
            v += a * noise(p);
            p = rot * p * 2.0 + shift;
            a *= 0.5;
        }
        return v;
    }

    half4 main(float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution) / min(iResolution.x, iResolution.y);
        float dist = length(uv);

        float sphereRadius = 0.35 + (iAmplitude * 0.08);

        // Outside sphere -> soft halo
        if (dist > sphereRadius) {
            float halo = exp(-6.0 * (dist - sphereRadius));
            return half4(iHaloColor * halo, halo * 0.5);
        }

        // Inside sphere -> compute normal
        float z = sqrt(max(0.0, sphereRadius * sphereRadius - dist * dist)) / sphereRadius;
        float3 normal = normalize(float3(uv / sphereRadius, z));

        // Light direction with tilt
        float3 lightDir = normalize(float3(0.5 + iTilt.x * 0.3, -0.6 + iTilt.y * 0.3, 0.8));

        // Diffuse and specular lighting
        float diff = max(dot(normal, lightDir), 0.0);
        float3 halfVec = normalize(lightDir + float3(0.0, 0.0, 1.0));
        float spec = pow(max(dot(normal, halfVec), 0.0), 32.0);

        // Rotating fbm noise interior energy
        float2 flowUv = uv * 3.0 + float2(iTime * 0.4, iTime * 0.3);
        float energy = fbm(flowUv + normal.xy);

        // Fresnel rim
        float fresnel = pow(1.0 - z, 2.8);

        float3 col = mix(iCoreColor * (0.6 + 0.4 * energy + iAmplitude * 0.5), iRimColor, fresnel);
        col += float3(spec * 0.8);
        col *= (0.4 + 0.6 * diff);

        return half4(col, 1.0);
    }
"""

@Composable
fun LiaOrb3D(
    state: NovaOrbState,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    amplitude: Float = 0f,
    style: NovaOrbStyle = NovaOrbStyle.NOVA,
    assistantName: String = "Lia"
) {
    if (!agslAvailable || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        NovaOrb(
            state = state,
            modifier = modifier,
            size = size,
            amplitude = amplitude,
            style = style,
            assistantName = assistantName
        )
        return
    }

    LiaOrb3DRuntimeShader(
        state = state,
        modifier = modifier,
        size = size,
        amplitude = amplitude,
        style = style,
        assistantName = assistantName
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LiaOrb3DRuntimeShader(
    state: NovaOrbState,
    modifier: Modifier,
    size: Dp,
    amplitude: Float,
    style: NovaOrbStyle,
    assistantName: String
) {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current
    val context = LocalContext.current
    val density = LocalDensity.current

    val shader = remember { RuntimeShader(AGSL_ORB_SHADER) }

    // Accelerometer tilt tracking with low-pass filter
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(reducedMotion) {
        if (reducedMotion) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val ax = -event.values[0] / 9.81f
                val ay = event.values[1] / 9.81f
                // Low-pass filter (alpha = 0.1)
                tiltX += 0.1f * (ax - tiltX)
                tiltY += 0.1f * (ay - tiltY)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val transition = rememberInfiniteTransition(label = "OrbShaderTransition")
    val timeDuration = when (state) {
        NovaOrbState.THINKING -> 2000
        NovaOrbState.CONNECTING -> 1500
        else -> 6000
    }

    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(timeDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShaderTime"
    )

    val (coreCol, rimCol, haloCol) = when (state) {
        NovaOrbState.IDLE -> Triple(Color(0xFF7B1FA2), colors.lotus, Color(0xFFFF6FAE))
        NovaOrbState.LISTENING -> Triple(Color(0xFF00838F), colors.lagoon, Color(0xFF3FE0D0))
        NovaOrbState.THINKING -> Triple(Color(0xFF4527A0), Color(0xFF7C4DFF), Color(0xFF7C4DFF))
        NovaOrbState.SPEAKING -> Triple(colors.accentSecondary, colors.accent, colors.accentGlow)
        NovaOrbState.CONNECTING -> Triple(Color(0xFFE65100), colors.accent, Color(0xFFFFB547))
        NovaOrbState.WATCHING -> Triple(Color(0xFF004D40), Color(0xFF00BFA5), Color(0xFF64FFDA))
        NovaOrbState.ERROR -> Triple(Color(0xFFB71C1C), colors.error, Color(0xFFFF5252))
    }

    val pxSize = with(density) { size.toPx() }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                shader.setFloatUniform("iResolution", pxSize, pxSize)
                shader.setFloatUniform("iTime", if (reducedMotion) 0f else time)
                shader.setFloatUniform("iAmplitude", amplitude.coerceIn(0f, 1f))
                shader.setFloatUniform("iTilt", if (reducedMotion) 0f else tiltX, if (reducedMotion) 0f else tiltY)
                shader.setFloatUniform("iCoreColor", coreCol.red, coreCol.green, coreCol.blue)
                shader.setFloatUniform("iRimColor", rimCol.red, rimCol.green, rimCol.blue)
                shader.setFloatUniform("iHaloColor", haloCol.red, haloCol.green, haloCol.blue)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "contents")
                    .asComposeRenderEffect()
            }
    ) {
        drawRect(Color.White)
    }
}
