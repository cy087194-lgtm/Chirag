package com.Lia.assistant.ui.screens.quickactions

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.action.AppLauncher
import com.Lia.assistant.action.InstalledAppLabelCache
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.nightSky
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class QuickActionTile(
    val title: String,
    val phrase: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun QuickActionsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current
    val scope = CoroutineScope(Dispatchers.Main)

    val tiles = listOf(
        QuickActionTile(
            title = "Open YouTube",
            phrase = "\"YouTube kholo\"",
            icon = Icons.Default.PlayArrow,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val apps = InstalledAppLabelCache.get(context)
                    AppLauncher.findApp("youtube", apps)?.let {
                        AppLauncher.launch(context, it)
                    }
                }
            }
        ),
        QuickActionTile(
            title = "Open Camera",
            phrase = "\"Camera kholo\"",
            icon = Icons.Default.CameraAlt,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val apps = InstalledAppLabelCache.get(context)
                    AppLauncher.findApp("camera", apps)?.let {
                        AppLauncher.launch(context, it)
                    }
                }
            }
        ),
        QuickActionTile(
            title = "Build a Website",
            phrase = "\"Ek cafe ki website banao\"",
            icon = Icons.Default.Web,
            onClick = {
                ForgeController.start(context, "Modern cafe with artisanal coffee menu and 3D cup")
                navController.navigate("forge")
            }
        ),
        QuickActionTile(
            title = "Call Contact",
            phrase = "\"Mummy ko call karo\"",
            icon = Icons.Default.Call,
            onClick = { navController.navigate("voice") }
        ),
        QuickActionTile(
            title = "Send Message",
            phrase = "\"Rahul ko message karo\"",
            icon = Icons.Default.Message,
            onClick = { navController.navigate("voice") }
        ),
        QuickActionTile(
            title = "Ask Anything",
            phrase = "\"Duniya ka sabse uncha pahad kaunsa hai\"",
            icon = Icons.Default.Language,
            onClick = { navController.navigate("chat") }
        )
    )

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
                text = "Quick Actions",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tiles) { tile ->
                val shape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(colors.surfaceGlass)
                        .clickable { tile.onClick() }
                        .padding(16.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceRaised),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tile.icon,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = tile.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tile.phrase,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
