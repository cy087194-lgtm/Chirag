package com.Lia.assistant.ui.screens.routines

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.agent.demonstration.DemonstrationRecorder
import com.Lia.assistant.agent.demonstration.LearnedRoutine
import com.Lia.assistant.agent.demonstration.LearnedRoutineStore
import com.Lia.assistant.agent.demonstration.RoutineCompiler
import com.Lia.assistant.agent.demonstration.RoutineExecutionEngine
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonType
import com.Lia.assistant.ui.components.NovaGlassCard
import com.Lia.assistant.ui.components.NovaSectionHeader
import com.Lia.assistant.ui.components.NovaTextField
import com.Lia.assistant.ui.theme.LocalNovaColors
import kotlinx.coroutines.launch

@Composable
fun LearnedRoutinesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        LearnedRoutineStore.initialize(context)
        onDispose {}
    }

    val routines by LearnedRoutineStore.routinesFlow.collectAsState()
    val isRecording by DemonstrationRecorder.isRecording.collectAsState()
    val recordedStepsCount by DemonstrationRecorder.recordedCount.collectAsState()

    var editingRoutine by remember { mutableStateOf<LearnedRoutine?>(null) }
    var editNameInput by remember { mutableStateOf("") }
    var editTriggerInput by remember { mutableStateOf("") }
    var editMessageInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceGlass)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Learned Routines",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Teach by Demonstration (PART Z)",
                    fontSize = 12.sp,
                    color = colors.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Teach Mode Control Card
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp,
            padding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRecording) "Learning Mode Active ($recordedStepsCount steps)" else "Teach a New Routine",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) colors.error else colors.textPrimary
                    )
                    Text(
                        text = if (isRecording) "Perform task on screen, then tap Stop" else "Say 'seekhna shuru karo' or tap Learn",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                NovaButton(
                    text = if (isRecording) "Stop Learning" else "Teach Task",
                    onClick = {
                        if (isRecording) {
                            val (actions, _) = DemonstrationRecorder.stopLearning(context)
                            if (actions.isNotEmpty()) {
                                try {
                                    val routine = RoutineCompiler.compile(context, actions)
                                    LearnedRoutineStore.saveRoutine(context, routine)
                                    Toast.makeText(context, "Routine saved: \"${routine.name}\"", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Compile error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            DemonstrationRecorder.startLearning(context)
                        }
                    },
                    type = if (isRecording) NovaButtonType.SECONDARY else NovaButtonType.PRIMARY
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: ROUTINES LIST
        NovaSectionHeader(title = "Saved Demonstrated Routines (${routines.size})")

        if (routines.isEmpty()) {
            NovaGlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                padding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No learned routines yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Say \"seekhna shuru karo\", do a task on screen, and say \"seekhna band karo\". LiaAI will repeat it anytime!",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            routines.forEach { routine ->
                RoutineItemCard(
                    routine = routine,
                    onToggle = { enabled ->
                        LearnedRoutineStore.updateRoutineEnabled(context, routine.id, enabled)
                    },
                    onRun = {
                        scope.launch {
                            Toast.makeText(context, "Executing \"${routine.name}\"...", Toast.LENGTH_SHORT).show()
                            val res = RoutineExecutionEngine.executeRoutine(context, routine)
                            Toast.makeText(context, res.spokenSummary, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEdit = {
                        editingRoutine = routine
                        editNameInput = routine.name
                        editTriggerInput = routine.trigger.phrase
                        editMessageInput = routine.variables["messageText"] ?: ""
                    },
                    onDelete = {
                        LearnedRoutineStore.deleteRoutine(context, routine.id)
                        Toast.makeText(context, "Routine deleted", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Card
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            padding = 12.dp
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = colors.lagoon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Local Privacy Guarantee",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.lagoon
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Demonstrations, recordings, and routines are stored strictly on this device in private storage. Password fields, OTPs, and banking screens are completely blocked from recording.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Honest Limits Card
        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            padding = 12.dp
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Demonstration Capabilities & Limits",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Apps that enforce FLAG_SECURE (banking/UPI) cannot be recorded.\n• Major UI updates in third-party apps may require relearning.\n• Demonstrations cannot bypass CAPTCHAs, two-factor logins, or OS permissions.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Edit Routine Dialog
    if (editingRoutine != null) {
        AlertDialog(
            onDismissRequest = { editingRoutine = null },
            title = { Text(text = "Edit Routine", color = colors.textPrimary) },
            text = {
                Column {
                    Text(text = "Routine Name", fontSize = 11.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    NovaTextField(value = editNameInput, onValueChange = { editNameInput = it })

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Spoken Trigger (say 'Jarvis, <phrase>')", fontSize = 11.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    NovaTextField(value = editTriggerInput, onValueChange = { editTriggerInput = it })

                    if (editingRoutine?.variables?.containsKey("messageText") == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Message Text Variable", fontSize = 11.sp, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        NovaTextField(value = editMessageInput, onValueChange = { editMessageInput = it })
                    }
                }
            },
            confirmButton = {
                NovaButton(
                    text = "Save",
                    onClick = {
                        val routine = editingRoutine!!
                        val newVars = routine.variables.toMutableMap()
                        if (editMessageInput.isNotBlank()) {
                            newVars["messageText"] = editMessageInput.trim()
                        }
                        val updated = routine.copy(
                            name = editNameInput.trim().ifEmpty { routine.name },
                            trigger = routine.trigger.copy(phrase = editTriggerInput.trim().ifEmpty { routine.trigger.phrase }),
                            variables = newVars
                        )
                        LearnedRoutineStore.saveRoutine(context, updated)
                        editingRoutine = null
                    }
                )
            },
            dismissButton = {
                NovaButton(
                    text = "Cancel",
                    onClick = { editingRoutine = null },
                    type = NovaButtonType.TEXT
                )
            },
            containerColor = colors.surfaceRaised
        )
    }
}

@Composable
private fun RoutineItemCard(
    routine: LearnedRoutine,
    onToggle: (Boolean) -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalNovaColors.current

    NovaGlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        padding = 12.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (routine.isEnabled) colors.textPrimary else colors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Trigger: \"Jarvis, ${routine.trigger.phrase}\"",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }

                Switch(
                    checked = routine.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.accent,
                        checkedTrackColor = colors.surfaceRaised
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (routine.directRouteAvailable) colors.lagoon.copy(alpha = 0.2f) else colors.surfaceRaised)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (routine.directRouteAvailable) "⚡ Direct Deep Link" else "📱 UI Accessibility (${routine.steps.size} steps)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (routine.directRouteAvailable) colors.lagoon else colors.textSecondary
                    )
                }

                if (routine.lastRunDurationMs > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceGlass)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Speed: ${routine.lastRunDurationMs}ms (${routine.lastRunStatus})",
                            fontSize = 10.sp,
                            color = if (routine.lastRunStatus == "SUCCESS") colors.success else colors.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Routine",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Routine",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                NovaButton(
                    text = "Run Now",
                    onClick = onRun,
                    type = NovaButtonType.SECONDARY
                )
            }
        }
    }
}
