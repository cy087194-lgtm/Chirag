package com.Lia.assistant.ui.screens.coach

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.agent.gaming.AllRounderCoachEngine
import com.Lia.assistant.agent.gaming.BargeInStopListener
import com.Lia.assistant.agent.gaming.CoachDebugHolder
import com.Lia.assistant.agent.gaming.CoachPreferences
import com.Lia.assistant.agent.gaming.CoachState
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonType
import com.Lia.assistant.ui.components.NovaGlassCard
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.nightSky
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CoachDebugScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val coachState by AllRounderCoachEngine.coachState.collectAsState()
    val mode by CoachDebugHolder.currentMode.collectAsState()
    val activePkg by CoachDebugHolder.activePackage.collectAsState()
    val lastFrame by CoachDebugHolder.lastFrame.collectAsState()
    val lastModelJson by CoachDebugHolder.lastModelJson.collectAsState()
    val lastDecision by CoachDebugHolder.lastDecision.collectAsState()
    val recentDecisions by CoachDebugHolder.recentDecisions.collectAsState()
    val lastTip by CoachDebugHolder.lastTip.collectAsState()
    val stopDebug by BargeInStopListener.lastDebugRecord.collectAsState()
    val freq = CoachPreferences.getFrequency(context)

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Coach Vision & Truth Debug",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State Card with Quick Controls
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Coach State: ${coachState.name}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (coachState == CoachState.MONITORING) colors.accent else colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mode: ${mode.label}",
                            fontSize = 13.sp,
                            color = colors.textPrimary
                        )
                        if (activePkg.isNotBlank()) {
                            Text(
                                text = "Package: $activePkg",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    NovaButton(
                        text = if (coachState == CoachState.MONITORING) "Stop Coach" else "Start Coach",
                        onClick = {
                            if (coachState == CoachState.MONITORING) {
                                AllRounderCoachEngine.stopCoaching()
                            } else {
                                AllRounderCoachEngine.startCoaching(context, scope)
                            }
                        },
                        type = if (coachState == CoachState.MONITORING) NovaButtonType.SECONDARY else NovaButtonType.PRIMARY,
                        icon = if (coachState == CoachState.MONITORING) Icons.Default.Stop else Icons.Default.PlayArrow
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Frequency setting: ${freq.label} (Min Urgency: ${freq.minUrgency})",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Decision Telemetry
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Latest Decision Analysis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (lastDecision != null) {
                    val dec = lastDecision!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (dec.outcome) {
                                        "SPOKEN" -> Color(0xFF2E7D32)
                                        "SKIPPED_UNCHANGED" -> Color(0xFF1565C0)
                                        "SKIPPED_EMPTY_EVENTS" -> Color(0xFF5E35B1)
                                        else -> Color(0xFFC2185B)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = dec.outcome,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Diff: ${"%.1f".format(dec.diffPercent)}%",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reason: ${dec.reason}",
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )

                    if (!dec.tip.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tip: \"${dec.tip}\"",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                } else {
                    Text(
                        text = "Waiting for coach cycle...",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instant Voice Stop (Barge-In) Debug
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Instant Stop Listener (Barge-In)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active Stop Words: stop, ruk, ruko, bas, chup, coach off, cancel, enough",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (stopDebug.lastHeard.isNotBlank()) {
                    Text(
                        text = "Last Heard: \"${stopDebug.lastHeard}\"",
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Matched Stop Word: ${stopDebug.matchedWord ?: "None"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stopDebug.matchedWord != null) Color(0xFFE91E63) else colors.textSecondary
                    )
                    Text(
                        text = "Stop Latency: ${stopDebug.latencyMs} ms",
                        fontSize = 12.sp,
                        color = colors.accent
                    )
                } else {
                    Text(
                        text = "Listening for stop command while coach is active...",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fresh Frame Preview
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Fresh Captured Frame Preview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (lastFrame != null) {
                    Image(
                        bitmap = lastFrame!!.asImageBitmap(),
                        contentDescription = "Coach Frame Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, colors.surfaceBorder, RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceRaised),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No frame captured yet\n(Starts when coach is active & screen unblocked)",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Raw Vision Model JSON
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Model Response JSON",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E1E24))
                        .padding(12.dp)
                ) {
                    Text(
                        text = lastModelJson,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF81D4FA)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Decision History Log
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Decision Log (Last 25 Cycles)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (recentDecisions.isEmpty()) {
                    Text(
                        text = "No history yet. Start coach to see why tips are spoken or skipped.",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                } else {
                    recentDecisions.take(15).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = timeFormat.format(Date(item.timestamp)),
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.outcome,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.outcome == "SPOKEN") Color(0xFF4CAF50) else colors.textPrimary
                                    )
                                }
                                Text(
                                    text = item.reason,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = "${"%.1f".format(item.diffPercent)}%",
                                fontSize = 11.sp,
                                color = colors.accent
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Honesty & Policy Disclaimer
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = CoachPreferences.DISCLAIMER_TEXT,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
