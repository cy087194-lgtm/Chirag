package com.Lia.assistant.ui.screens.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.ui.components.NovaGlassCard
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.nightSky

@Composable
fun PrivacyScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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
                text = "Privacy & Data Policy",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Local-First Architecture",
                    fontSize = 16.sp,
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Your Gemini API key is stored strictly on your local device in private application storage.\n" +
                            "• Audio streams directly between your phone and Google Gemini Live WebSocket servers over TLS. No intermediate relay servers store or inspect your voice.\n" +
                            "• Conversation memory is retained in volatile memory only for the duration of the active conversation session and cleared upon stopping.\n" +
                            "• Screen interactions in the direct build occur entirely on-device through Android's Accessibility APIs and are never recorded to persistent logs.",
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
