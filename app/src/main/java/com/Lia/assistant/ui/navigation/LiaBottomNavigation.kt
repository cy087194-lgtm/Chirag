package com.Lia.assistant.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.ui.fx.LiaOrb3D
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.depthSurface
import com.Lia.assistant.voice.NovaOrbState

@Composable
fun LiaBottomNavigation(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val navShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating glass bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .depthSurface(shape = navShape, elevation = 6.dp)
                .clip(navShape)
                .background(colors.surfaceGlass)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentRoute == "home",
                onClick = { navController.navigate("home") { launchSingleTop = true } }
            )

            NavItem(
                icon = Icons.Default.Chat,
                label = "Chat",
                isSelected = currentRoute == "chat",
                onClick = { navController.navigate("chat") { launchSingleTop = true } }
            )

            // Center Spacer for raised talk orb
            Box(modifier = Modifier.size(54.dp))

            NavItem(
                icon = Icons.Default.History,
                label = "History",
                isSelected = currentRoute == "history",
                onClick = { navController.navigate("history") { launchSingleTop = true } }
            )

            NavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentRoute == "settings",
                onClick = { navController.navigate("settings") { launchSingleTop = true } }
            )
        }

        // Center raised Talk orb
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(58.dp)
                .clip(CircleShape)
                .clickable { navController.navigate("voice") },
            contentAlignment = Alignment.Center
        ) {
            LiaOrb3D(
                state = NovaOrbState.IDLE,
                size = 58.dp,
                amplitude = 0f
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalNovaColors.current
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) colors.accent else colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) colors.accent else colors.textSecondary
        )
    }
}
