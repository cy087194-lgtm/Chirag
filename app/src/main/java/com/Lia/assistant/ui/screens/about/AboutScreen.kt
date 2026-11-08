package com.Lia.assistant.ui.screens.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.data.AssistantBrand
import com.Lia.assistant.ui.components.NovaGlassCard
import com.Lia.assistant.ui.fx.LiaOrb3D
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.SerifFontFamily
import com.Lia.assistant.ui.theme.nightSky
import com.Lia.assistant.voice.NovaOrbState

@Composable
fun AboutScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                text = "About",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 120 dp Orb on About screen
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            LiaOrb3D(
                state = NovaOrbState.IDLE,
                size = 120.dp,
                amplitude = 0f
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = AssistantBrand.FULL_NAME,
            fontFamily = SerifFontFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Text(
            text = "Version 1.0.0",
            fontSize = 13.sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "A Pearl of Light in the Night",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lia AI is a real-time full-duplex personal AI companion crafted in Kotlin & Jetpack Compose with custom AGSL 3D shaders, multi-lingual intelligence (Hindi, Hinglish, English), and the Website Forge.",
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                com.Lia.assistant.ui.components.NovaButton(
                    text = "Share / Export APK",
                    onClick = {
                        com.Lia.assistant.util.ApkExportHelper.shareApk(context)
                    }
                )
            }
        }
    }
}
