package com.Lia.assistant.ui.screens.forge

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Lia.assistant.forge.ForgeStage
import com.Lia.assistant.forge.ForgeUiState
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonType
import com.Lia.assistant.ui.components.NovaGlassCard
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.SerifFontFamily

@Composable
fun ForgeHud(
    state: ForgeUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top prompt chip
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(50))
                .background(colors.surfaceGlass)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Forging: ${state.prompt}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1
            )
        }

        // Bottom status card
        NovaGlassCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = state.stage,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                        },
                        label = "StageAnimation"
                    ) { stage ->
                        Text(
                            text = stage.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }

                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        fontFamily = SerifFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Six-segment stage track
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val currentIdx = state.stage.ordinal
                    for (i in 0 until 6) {
                        val isComplete = i <= currentIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isComplete) colors.accent else colors.surfaceBorder)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${state.metrics.chars / 1024} KB",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${state.sections.size} sections",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${state.metrics.tokensPerSec.toInt()} tok/s",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${state.metrics.elapsedSec}s",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                NovaButton(
                    text = "Cancel Forge",
                    onClick = onCancel,
                    type = NovaButtonType.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
