package com.Lia.assistant.ui.screens.forge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.forge.ForgePhase
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonType
import com.Lia.assistant.ui.components.NovaEmptyState
import com.Lia.assistant.ui.components.NovaTextField
import com.Lia.assistant.ui.theme.LocalNovaColors

@Composable
fun ForgeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current
    val state by ForgeController.state.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editInput by remember { mutableStateOf("") }
    var useFallbackScene by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when (state.phase) {
            ForgePhase.IDLE -> {
                var promptInput by remember { mutableStateOf("") }
                Box(modifier = Modifier.fillMaxSize()) {
                    NovaEmptyState(
                        title = "Website Forge",
                        subtitle = "Speak or describe the website you want to generate in 3D.",
                        actionText = "Forge Website",
                        onActionClick = {
                            if (promptInput.isNotBlank()) {
                                ForgeController.start(context, promptInput)
                            }
                        }
                    )
                }
            }

            ForgePhase.BUILDING -> {
                if (useFallbackScene) {
                    ForgeFallbackScene(state = state)
                } else {
                    ForgeWebGlScene(
                        state = state,
                        onReady = {},
                        onRevealFinished = {},
                        onFailed = { useFallbackScene = true }
                    )
                }

                ForgeHud(
                    state = state,
                    onCancel = { ForgeController.cancel() }
                )
            }

            ForgePhase.DONE -> {
                val file = state.file
                val html = file?.readText() ?: ForgeController.partialHtml()

                GeneratedSiteView(html = html)

                ResultToolbar(
                    file = file,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        editInput = ""
                        showEditDialog = true
                    },
                    onRebuild = { ForgeController.retry(context) },
                    onDelete = {
                        file?.delete()
                        ForgeController.reset()
                        navController.popBackStack()
                    }
                )
            }

            ForgePhase.FAILED -> {
                NovaEmptyState(
                    title = "Forge Interrupted",
                    subtitle = state.error ?: "Something went wrong while generating the website.",
                    actionText = "Try Again",
                    onActionClick = { ForgeController.retry(context) }
                )
            }

            ForgePhase.CANCELLED -> {
                NovaEmptyState(
                    title = "Forge Cancelled",
                    subtitle = "Website generation was cancelled by you.",
                    actionText = "Start New",
                    onActionClick = { ForgeController.reset() }
                )
            }
        }

        // Edit Dialog
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text(text = "Refine Website", color = colors.textPrimary) },
                text = {
                    NovaTextField(
                        value = editInput,
                        onValueChange = { editInput = it },
                        placeholder = "e.g. Change accent to emerald, add coffee pricing..."
                    )
                },
                confirmButton = {
                    NovaButton(
                        text = "Apply Changes",
                        onClick = {
                            showEditDialog = false
                            if (editInput.isNotBlank()) {
                                ForgeController.edit(context, editInput)
                            }
                        }
                    )
                },
                dismissButton = {
                    NovaButton(
                        text = "Cancel",
                        onClick = { showEditDialog = false },
                        type = NovaButtonType.TEXT
                    )
                },
                containerColor = colors.surfaceRaised
            )
        }
    }
}
