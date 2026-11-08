package com.Lia.assistant.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.LocalNovaReducedMotion
import com.Lia.assistant.ui.theme.depthSurface

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

enum class NovaButtonType {
    PRIMARY,
    SECONDARY,
    TEXT
}

@Composable
fun NovaGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 4.dp,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val colors = LocalNovaColors.current
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .depthSurface(shape = shape, elevation = elevation, rimColor = colors.rimLight)
            .clip(shape)
            .background(colors.surfaceGlass)
            .padding(padding)
    ) {
        content()
    }
}

@Composable
fun NovaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: NovaButtonType = NovaButtonType.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current
    val scale = remember { Animatable(1f) }

    val bgModifier = when (type) {
        NovaButtonType.PRIMARY -> Modifier
            .background(if (enabled) colors.accent else colors.surfaceBorder)
        NovaButtonType.SECONDARY -> Modifier
            .depthSurface(shape = RoundedCornerShape(50), elevation = 2.dp)
            .background(colors.surfaceGlass)
        NovaButtonType.TEXT -> Modifier
    }

    val contentColor = when (type) {
        NovaButtonType.PRIMARY -> colors.onAccent
        NovaButtonType.SECONDARY -> colors.textPrimary
        NovaButtonType.TEXT -> colors.accent
    }

    Box(
        modifier = modifier
            .scale(scale.value)
            .defaultMinSize(minWidth = 84.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(50))
            .then(bgModifier)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        if (!reducedMotion) {
                            scale.animateTo(0.96f, tween(80))
                            tryAwaitRelease()
                            scale.animateTo(1f, tween(100))
                        }
                        onClick()
                    }
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun NovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true
) {
    val colors = LocalNovaColors.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .depthSurface(shape = shape, elevation = 2.dp)
            .clip(shape)
            .background(colors.surfaceGlass)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = colors.textTertiary,
                fontSize = 15.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = colors.textPrimary,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NovaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null
) {
    val colors = LocalNovaColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null) {
            navigationIcon()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actions != null) {
            actions()
        }
    }
}

@Composable
fun NovaSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = colors.accent,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun NovaSettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: String? = null,
    switch: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 54.dp)
                .then(clickableModifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceRaised),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            if (value != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceRaised)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value,
                        fontSize = 12.sp,
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (switch != null && onCheckedChange != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = switch,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textTertiary,
                        uncheckedTrackColor = colors.surfaceRaised
                    )
                )
            }
        }

        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 66.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = colors.rimLight.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun NovaMessageBubble(
    message: ChatMessage,
    onSpeakClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val isUser = message.isUser

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleBg = if (isUser) colors.userBubble else colors.assistantBubble
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .depthSurface(shape = shape, elevation = if (isUser) 1.dp else 3.dp)
                .clip(shape)
                .background(bubbleBg)
                .padding(14.dp)
        ) {
            Text(
                text = message.text,
                color = colors.textPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun NovaEmptyState(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = colors.textSecondary
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            NovaButton(
                text = actionText,
                onClick = onActionClick,
                type = NovaButtonType.PRIMARY
            )
        }
    }
}

@Composable
fun NovaTypingDots(modifier: Modifier = Modifier) {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current
    val anim = remember { Animatable(0f) }

    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0..2) {
            val alpha = if (reducedMotion) 0.6f else {
                val phase = (anim.value + i * 0.33f) % 1f
                0.2f + 0.8f * kotlin.math.sin(phase * Math.PI).toFloat().coerceIn(0f, 1f)
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun NovaVoiceVisualizer(
    amplitude: Float,
    barCount: Int = 24,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val clamped = amplitude.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val barWidth = size.width / (barCount * 2f)
        val centerY = size.height / 2f
        val random = java.util.Random(1337)

        for (i in 0 until barCount) {
            val jitter = 0.4f + random.nextFloat() * 0.6f
            val barHeight = (size.height * 0.15f) + (size.height * 0.85f * clamped * jitter)
            val left = i * (barWidth * 2f) + barWidth / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.accent, colors.lagoon)
                ),
                topLeft = Offset(left, centerY - barHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
