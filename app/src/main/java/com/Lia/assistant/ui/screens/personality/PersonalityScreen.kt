package com.Lia.assistant.ui.screens.personality

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.data.Personality
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.nightSky

@Composable
fun PersonalityScreen(
    navController: NavController,
    appState: NovaAppState,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .padding(16.dp)
    ) {
        // Header
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
            Text(
                text = "Select Personality",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Changes take effect immediately in voice and chat.",
            fontSize = 13.sp,
            color = colors.accent,
            modifier = Modifier.padding(start = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of 10 personality glass cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(Personality.entries) { item ->
                val isSelected = item == appState.personality
                val cardShape = RoundedCornerShape(20.dp)

                val borderModifier = if (isSelected) {
                    Modifier.border(2.dp, colors.accent, cardShape)
                } else {
                    Modifier.border(1.dp, colors.surfaceBorder, cardShape)
                }

                Box(
                    modifier = Modifier
                        .scale(if (isSelected) 1.04f else 0.98f)
                        .clip(cardShape)
                        .then(borderModifier)
                        .background(if (isSelected) colors.surfaceRaised else colors.surfaceGlass)
                        .clickable { appState.applyPersonality(context, item) }
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = item.icon,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) colors.accent else colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp,
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}
