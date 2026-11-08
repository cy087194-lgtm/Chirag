package com.Lia.assistant.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.data.NovaPreferences
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.ui.screens.about.AboutScreen
import com.Lia.assistant.ui.screens.chat.ChatScreen
import com.Lia.assistant.ui.screens.debug.DebugScreen
import com.Lia.assistant.ui.screens.forge.ForgeScreen
import com.Lia.assistant.ui.screens.history.HistoryScreen
import com.Lia.assistant.ui.screens.home.HomeScreen
import com.Lia.assistant.ui.screens.jarvis.JarvisHQScreen
import com.Lia.assistant.ui.screens.onboarding.OnboardingScreen
import com.Lia.assistant.ui.screens.orb.OrbStyleScreen
import com.Lia.assistant.ui.screens.permissions.PermissionsScreen
import com.Lia.assistant.ui.screens.personality.PersonalityScreen
import com.Lia.assistant.ui.screens.privacy.PrivacyScreen
import com.Lia.assistant.ui.screens.profile.ProfileScreen
import com.Lia.assistant.ui.screens.quickactions.QuickActionsScreen
import com.Lia.assistant.ui.screens.settings.SettingsScreen
import com.Lia.assistant.ui.screens.voice.VoiceScreen
import com.Lia.assistant.ui.theme.LocalBottomBarInset
import com.Lia.assistant.ui.theme.nightSky

@Composable
fun NovaApp(appState: NovaAppState) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hasOnboarded = NovaPreferences.getBoolean(context, NovaPreferences.Keys.HAS_ONBOARDED, false)
    val startDestination = if (hasOnboarded) "home" else "onboarding"

    val bottomBarRoutes = setOf("home", "chat", "history", "settings")
    val showBottomBar = currentRoute in bottomBarRoutes

    // Reactively observe ForgeController.pendingOpen
    LaunchedEffect(ForgeController.pendingOpen) {
        if (ForgeController.consumePendingOpen()) {
            navController.navigate("forge") {
                launchSingleTop = true
            }
        }
    }

    CompositionLocalProvider(LocalBottomBarInset provides if (showBottomBar) 80.dp else 0.dp) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nightSky(),
            bottomBar = {
                if (showBottomBar) {
                    LiaBottomNavigation(
                        navController = navController,
                        currentRoute = currentRoute
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("onboarding") { OnboardingScreen(navController) }
                    composable("home") { HomeScreen(navController, appState) }
                    composable("chat") { ChatScreen(navController, appState) }
                    composable("history") { HistoryScreen(navController, appState) }
                    composable("settings") { SettingsScreen(navController, appState) }
                    composable("voice") { VoiceScreen(navController, appState) }
                    composable("quick_actions") { QuickActionsScreen(navController) }
                    composable("profile") { ProfileScreen(navController, appState) }
                    composable("personality") { PersonalityScreen(navController, appState) }
                    composable("orb_style") { OrbStyleScreen(navController, appState) }
                    composable("privacy") { PrivacyScreen(navController) }
                    composable("permissions") { PermissionsScreen(navController) }
                    composable("about") { AboutScreen(navController) }
                    composable("debug") { DebugScreen(navController) }
                    composable("forge") { ForgeScreen(navController) }
                    composable("jarvis_hq") { JarvisHQScreen(navController, appState) }
                    composable("features") { com.Lia.assistant.ui.screens.features.FeaturesScreen(navController) }
                    composable("coach_debug") { com.Lia.assistant.ui.screens.coach.CoachDebugScreen(navController) }
                    composable("live_status") { com.Lia.assistant.ui.screens.status.LiveStatusScreen(navController) }
                    composable("learned_routines") { com.Lia.assistant.ui.screens.routines.LearnedRoutinesScreen(navController) }

                    // Flavor-specific routes (e.g. accessibility disclosure in direct)
                    registerAccessibilityDisclosureRoute(navController)
                }
            }
        }
    }
}
