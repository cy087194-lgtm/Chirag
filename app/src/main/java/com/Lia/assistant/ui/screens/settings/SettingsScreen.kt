package com.Lia.assistant.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.action.ActionExecutor
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.data.UserMemoryStore
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonType
import com.Lia.assistant.ui.components.NovaGlassCard
import com.Lia.assistant.ui.components.NovaSectionHeader
import com.Lia.assistant.ui.components.NovaSettingsRow
import com.Lia.assistant.ui.components.NovaTextField
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.nightSky
import org.json.JSONObject
import com.Lia.assistant.agent.gaming.CoachPreferences
import com.Lia.assistant.agent.gaming.CoachFrequency

@Composable
fun SettingsScreen(
    navController: NavController,
    appState: NovaAppState,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(ApiKeyStore.getKey(context)) }

    var showOpenAiDialog by remember { mutableStateOf(false) }
    var openAiInput by remember { mutableStateOf(ApiKeyStore.getOpenAiKey(context)) }

    var showClaudeDialog by remember { mutableStateOf(false) }
    var claudeInput by remember { mutableStateOf(ApiKeyStore.getClaudeKey(context)) }

    var showSearchKeyDialog by remember { mutableStateOf(false) }
    var searchKeyInput by remember { mutableStateOf(ApiKeyStore.getSearchApiKey(context)) }

    var routingMode by remember { mutableStateOf(ApiKeyStore.getModelRoutingPreference(context)) }
    var dailyTokens by remember { mutableStateOf(ApiKeyStore.getDailyTokensUsed(context)) }
    var dailyLimit by remember { mutableStateOf(ApiKeyStore.getDailyCostLimit(context)) }

    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(appState.assistantName) }

    var showMemoryDialog by remember { mutableStateOf(false) }
    var memoriesList by remember { mutableStateOf(UserMemoryStore.get(context).getAllMemories()) }
    var learnedRoutinesCount by remember { mutableStateOf<Int>(com.Lia.assistant.agent.demonstration.LearnedRoutineStore.getAllRoutines(context).size) }

    var isAccessibilityEnabled by remember { mutableStateOf(LiaAccessibilityService.isEnabledInSettings(context)) }
    var isOverlayAllowed by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    fun refreshSystemStatus() {
        isAccessibilityEnabled = LiaAccessibilityService.isEnabledInSettings(context)
        isOverlayAllowed = Settings.canDrawOverlays(context)
        memoriesList = UserMemoryStore.get(context).getAllMemories()
        learnedRoutinesCount = com.Lia.assistant.agent.demonstration.LearnedRoutineStore.getAllRoutines(context).size
        routingMode = ApiKeyStore.getModelRoutingPreference(context)
        dailyTokens = ApiKeyStore.getDailyTokensUsed(context)
        dailyLimit = ApiKeyStore.getDailyCostLimit(context)
    }

    var coachFrequency by remember { mutableStateOf(CoachPreferences.getFrequency(context)) }
    var lobbyDelaySec by remember { mutableStateOf(CoachPreferences.getLobbyDelaySeconds(context)) }
    var autoResumeGame by remember { mutableStateOf(CoachPreferences.isAutoResumeOnGameOpen(context)) }
    var showAddPackageDialog by remember { mutableStateOf(false) }
    var newPackageInput by remember { mutableStateOf("") }
    var gamePackages by remember { mutableStateOf(CoachPreferences.getGamePackages(context)) }

    DisposableEffect(lifecycleOwner) {
        refreshSystemStatus()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshSystemStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Profile Hero Card
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { navController.navigate("profile") },
            elevation = 6.dp,
            padding = 16.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.accentGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = colors.accent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appState.userName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Assistant: ${appState.assistantName} (${appState.personality.displayName})",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SECTION 1: AI & VOICE INTELLIGENCE
        NovaSectionHeader(title = "AI & Voice Intelligence")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 4.dp,
            padding = 0.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "Gemini API Key",
                    subtitle = if (ApiKeyStore.hasKey(context)) "Stored securely on device" else "Tap to configure API key",
                    icon = Icons.Default.Key,
                    value = if (ApiKeyStore.hasKey(context)) "Saved" else "Setup",
                    onClick = {
                        apiKeyInput = ApiKeyStore.getKey(context)
                        showApiKeyDialog = true
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "OpenAI (GPT) API Key",
                    subtitle = if (ApiKeyStore.getOpenAiKey(context).isNotEmpty()) "Configured for GPT-4o" else "Tap to configure OpenAI key",
                    icon = Icons.Default.Key,
                    value = if (ApiKeyStore.getOpenAiKey(context).isNotEmpty()) "Saved" else "Setup",
                    onClick = {
                        openAiInput = ApiKeyStore.getOpenAiKey(context)
                        showOpenAiDialog = true
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Anthropic Claude Key",
                    subtitle = if (ApiKeyStore.getClaudeKey(context).isNotEmpty()) "Configured for Claude 3.5 Sonnet" else "Tap to configure Claude key",
                    icon = Icons.Default.Key,
                    value = if (ApiKeyStore.getClaudeKey(context).isNotEmpty()) "Saved" else "Setup",
                    onClick = {
                        claudeInput = ApiKeyStore.getClaudeKey(context)
                        showClaudeDialog = true
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Model Router Engine",
                    subtitle = when (routingMode) {
                        "AUTO" -> "Auto-route based on task + auto-fallback"
                        "GEMINI" -> "Force Gemini 2.5 Flash as primary"
                        "OPENAI" -> "Force OpenAI GPT-4o as primary"
                        "CLAUDE" -> "Force Claude 3.5 Sonnet as primary"
                        else -> "Automatic intelligent routing"
                    },
                    icon = Icons.Default.Psychology,
                    value = routingMode,
                    onClick = {
                        val nextMode = when (routingMode) {
                            "AUTO" -> "GEMINI"
                            "GEMINI" -> "OPENAI"
                            "OPENAI" -> "CLAUDE"
                            else -> "AUTO"
                        }
                        ApiKeyStore.saveModelRoutingPreference(context, nextMode)
                        routingMode = nextMode
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Live Web Search Key",
                    subtitle = if (ApiKeyStore.getSearchApiKey(context).isNotEmpty()) "Search provider key active" else "Optional: Tavily / Serp / Brave key",
                    icon = Icons.Default.Language,
                    value = if (ApiKeyStore.getSearchApiKey(context).isNotEmpty()) "Saved" else "Setup",
                    onClick = {
                        searchKeyInput = ApiKeyStore.getSearchApiKey(context)
                        showSearchKeyDialog = true
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Daily Usage & Cost Limit",
                    subtitle = "Used today: $dailyTokens tokens (Limit: $$dailyLimit USD)",
                    icon = Icons.Default.Speed,
                    value = "$${"%.2f".format(dailyTokens * 0.000002)}",
                    onClick = {
                        Toast.makeText(context, "Today: $dailyTokens tokens consumed. Budget: $$dailyLimit/day", Toast.LENGTH_SHORT).show()
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Assistant Name",
                    subtitle = appState.assistantName,
                    icon = Icons.Default.Mic,
                    value = "Edit",
                    onClick = {
                        nameInput = appState.assistantName
                        showNameDialog = true
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Personality",
                    subtitle = appState.personality.displayName,
                    icon = Icons.Default.Mood,
                    value = appState.personality.icon,
                    onClick = { navController.navigate("personality") },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Spoken Language",
                    subtitle = appState.language.displayName,
                    icon = Icons.Default.Language,
                    value = "Change",
                    onClick = {
                        val nextLang = when (appState.language) {
                            LanguagePreference.AUTO -> LanguagePreference.HINDI
                            LanguagePreference.HINDI -> LanguagePreference.HINGLISH
                            LanguagePreference.HINGLISH -> LanguagePreference.ENGLISH
                            LanguagePreference.ENGLISH -> LanguagePreference.AUTO
                        }
                        appState.applyLanguage(context, nextLang)
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Gemini Voice Tone",
                    subtitle = appState.voiceName,
                    icon = Icons.Default.RecordVoiceOver,
                    value = "Switch",
                    onClick = {
                        val nextVoice = when (appState.voiceName) {
                            "Aoede" -> "Puck"
                            "Puck" -> "Charon"
                            "Charon" -> "Kore"
                            "Kore" -> "Fenrir"
                            else -> "Aoede"
                        }
                        appState.applyVoiceName(context, nextVoice)
                    },
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2: MOBILE & DEVICE CONTROL
        NovaSectionHeader(title = "Mobile & Device Control")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 4.dp,
            padding = 0.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "JARVIS Headquarters & Diagnostics",
                    subtitle = "Subsystem health, action audit log & test bench",
                    icon = Icons.Default.SmartToy,
                    value = "Dashboard",
                    onClick = { navController.navigate("jarvis_hq") },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Mobile Control Permissions",
                    subtitle = "Central Permission Registry & System Status",
                    icon = Icons.Default.Security,
                    value = "Review",
                    onClick = { navController.navigate("permissions") },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "System Accessibility Service",
                    subtitle = if (isAccessibilityEnabled) "Service active for phone control" else "Enable in Downloaded/Installed Apps",
                    icon = Icons.Default.Settings,
                    value = if (isAccessibilityEnabled) "Active" else "Setup",
                    onClick = { LiaAccessibilityService.openAccessibilitySettings(context) },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Display Over Other Apps",
                    subtitle = if (isOverlayAllowed) "Floating overlay assistant allowed" else "Allow overlay over other apps",
                    icon = Icons.Default.Layers,
                    value = if (isOverlayAllowed) "Allowed" else "Setup",
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Sound & Volume Settings",
                    subtitle = "Adjust media volume by voice ('volume badhao')",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    value = "Open",
                    onClick = {
                        val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Installed Applications",
                    subtitle = "Open WhatsApp, YouTube, Camera by voice",
                    icon = Icons.Default.PhoneAndroid,
                    value = "Manage",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 3: QUICK VOICE ACTION TRIGGERS
        NovaSectionHeader(title = "Quick Voice Action Triggers")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 4.dp,
            padding = 0.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "Home Screen Navigation",
                    subtitle = "Instantly exit to Home Screen ('home per ja')",
                    icon = Icons.Default.Home,
                    value = "Exit",
                    onClick = {
                        scope.launch {
                            ActionExecutor.execute(context, "go_home", JSONObject())
                        }
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Flashlight / Torch Action",
                    subtitle = "Toggle flashlight ('torch on / off')",
                    icon = Icons.Default.FlashlightOn,
                    value = "Toggle",
                    onClick = {
                        scope.launch {
                            ActionExecutor.execute(context, "toggle_torch", JSONObject())
                        }
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Battery Status Telemetry",
                    subtitle = "Voice check battery status ('battery kitni hai')",
                    icon = Icons.Default.BatteryChargingFull,
                    value = "Check",
                    onClick = {
                        scope.launch {
                            val res = ActionExecutor.execute(context, "get_battery_status", JSONObject())
                            val summary = res.optString("summary", "Battery ready")
                            Toast.makeText(context, summary, Toast.LENGTH_SHORT).show()
                        }
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Media Playback Control",
                    subtitle = "Control music playback ('gana roko / chalao')",
                    icon = Icons.Default.PlayArrow,
                    value = "Play/Pause",
                    onClick = {
                        scope.launch {
                            ActionExecutor.execute(context, "media_control", JSONObject())
                        }
                    },
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 3B: AUTOMATION & LEARNED ROUTINES (PART Z)
        NovaSectionHeader(title = "Learned Routines & Automation")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 4.dp,
            padding = 0.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "Learned Routines (Teach by Demo)",
                    subtitle = "Automate tasks recorded from your actions on screen",
                    icon = Icons.Default.Psychology,
                    value = "$learnedRoutinesCount Routines",
                    onClick = { navController.navigate("learned_routines") },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Teach Mode Commands",
                    subtitle = "'seekhna shuru karo', 'ye seekho', 'seekhna band karo'",
                    icon = Icons.Default.SmartToy,
                    value = "Help",
                    onClick = { navController.navigate("learned_routines") },
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 4: AI MEMORY & PERSONAL CONTEXT
        NovaSectionHeader(title = "AI Memory & Personal Context")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 4.dp,
            padding = 0.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "Saved Personal Memories",
                    subtitle = "Facts and preferences LiaAI remembers about you",
                    icon = Icons.Default.Psychology,
                    value = "${memoriesList.size} Saved",
                    onClick = {
                        memoriesList = UserMemoryStore.get(context).getAllMemories()
                        showMemoryDialog = true
                    },
                    showDivider = memoriesList.isNotEmpty()
                )
                if (memoriesList.isNotEmpty()) {
                    NovaSettingsRow(
                        title = "Clear All Memories",
                        subtitle = "Erase all stored notes and preferences",
                        icon = Icons.Default.Delete,
                        value = "Clear",
                        onClick = {
                            UserMemoryStore.get(context).clearAll()
                            memoriesList = emptyList()
                            Toast.makeText(context, "All memories cleared", Toast.LENGTH_SHORT).show()
                        },
                        showDivider = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 5: LOOK & VISUALS
        NovaSectionHeader(title = "Look & Visuals")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 4.dp,
            padding = 0.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "3D Orb Style",
                    subtitle = appState.orbStyle,
                    icon = Icons.Default.ColorLens,
                    value = "Select",
                    onClick = { navController.navigate("orb_style") },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Theme Mode",
                    subtitle = appState.themeMode,
                    icon = Icons.Default.DarkMode,
                    value = "Cycle",
                    onClick = {
                        val nextMode = when (appState.themeMode) {
                            "DARK" -> "LIGHT"
                            "LIGHT" -> "SYSTEM"
                            else -> "DARK"
                        }
                        appState.applyThemeMode(context, nextMode)
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Reduced Motion",
                    subtitle = "Minimize particle and parallax motion",
                    icon = Icons.Default.Animation,
                    switch = appState.reducedMotion,
                    onCheckedChange = { appState.applyReducedMotion(context, it) },
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 6: REAL-TIME GAME & SCREEN COACH
        NovaSectionHeader(title = "Screen & Game Coach (Real-Time)")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 4.dp,
            padding = 0.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "Tip Frequency",
                    subtitle = when (coachFrequency) {
                        CoachFrequency.KAM -> "Kam (Critical cues only: HP <30%, Danger)"
                        CoachFrequency.NORMAL -> "Normal (Tactical advice & health)"
                        CoachFrequency.ZYADA -> "Zyada (All tactical and loot insights)"
                    },
                    icon = Icons.Default.Speed,
                    value = coachFrequency.label,
                    onClick = {
                        val next = when (coachFrequency) {
                            CoachFrequency.KAM -> CoachFrequency.NORMAL
                            CoachFrequency.NORMAL -> CoachFrequency.ZYADA
                            CoachFrequency.ZYADA -> CoachFrequency.KAM
                        }
                        coachFrequency = next
                        CoachPreferences.setFrequency(context, next)
                    },
                    showDivider = true
                )

                NovaSettingsRow(
                    title = "Lobby & Menu Stability Delay",
                    subtitle = "Wait $lobbyDelaySec seconds before giving loadout advice",
                    icon = Icons.Default.Layers,
                    value = "${lobbyDelaySec}s",
                    onClick = {
                        val next = when (lobbyDelaySec) {
                            3 -> 5
                            5 -> 10
                            else -> 3
                        }
                        lobbyDelaySec = next
                        CoachPreferences.setLobbyDelaySeconds(context, next)
                    },
                    showDivider = true
                )

                NovaSettingsRow(
                    title = "Auto-Resume When Game Opens",
                    subtitle = "Resume watch automatically if previously active",
                    icon = Icons.Default.PlayArrow,
                    switch = autoResumeGame,
                    onCheckedChange = {
                        autoResumeGame = it
                        CoachPreferences.setAutoResumeOnGameOpen(context, it)
                    },
                    showDivider = true
                )

                NovaSettingsRow(
                    title = "Monitored Game Packages",
                    subtitle = "${gamePackages.size} packages configured (Tap to add/manage)",
                    icon = Icons.Default.SmartToy,
                    value = "+ Add",
                    onClick = { showAddPackageDialog = true },
                    showDivider = true
                )

                NovaSettingsRow(
                    title = "Live System Status & Diagnostics",
                    subtitle = "Accessibility ON/OFF, Screen Capture, Subsystem Health & CUJ Tests",
                    icon = Icons.Default.Security,
                    value = "Status",
                    onClick = { navController.navigate("live_status") },
                    showDivider = true
                )

                NovaSettingsRow(
                    title = "Live Coach Debug & Camera HUD",
                    subtitle = "View real-time frame diffs, Gemini JSON & stop latency",
                    icon = Icons.Default.Info,
                    value = "Open",
                    onClick = { navController.navigate("coach_debug") },
                    showDivider = false
                )
            }
        }

        // Coach Honesty & Rules Disclaimer
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                        text = "Fair Play & Performance Policy",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.lagoon
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CoachPreferences.DISCLAIMER_TEXT,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 7: ABOUT & SYSTEM INFO
        NovaSectionHeader(title = "About & Device Info")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 4.dp,
            padding = 0.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "LiaAI Assistant",
                    subtitle = "Full Phone Control & Voice Automation Agent",
                    icon = Icons.Default.Info,
                    value = "v2.0",
                    onClick = { navController.navigate("about") },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Download & Share APK",
                    subtitle = "Export APK installer or view installation fix guide",
                    icon = Icons.Default.Download,
                    value = "Share",
                    onClick = {
                        com.Lia.assistant.util.ApkExportHelper.shareApk(context)
                    },
                    showDivider = true
                )
                NovaSettingsRow(
                    title = "Device Platform",
                    subtitle = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                    icon = Icons.Default.PhoneAndroid,
                    value = "Android ${Build.VERSION.RELEASE}",
                    onClick = { navController.navigate("debug") },
                    showDivider = false
                )
            }
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(text = "Gemini API Key", color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        text = "Enter your Google Gemini API key. It is stored securely on your device only.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = "AIzaSy..."
                    )
                }
            },
            confirmButton = {
                NovaButton(
                    text = "Save Key",
                    onClick = {
                        ApiKeyStore.saveKey(context, apiKeyInput.trim())
                        showApiKeyDialog = false
                    }
                )
            },
            dismissButton = {
                NovaButton(
                    text = "Cancel",
                    onClick = { showApiKeyDialog = false },
                    type = NovaButtonType.TEXT
                )
            },
            containerColor = colors.surfaceRaised
        )
    }

    // OpenAI Key Dialog
    if (showOpenAiDialog) {
        AlertDialog(
            onDismissRequest = { showOpenAiDialog = false },
            title = { Text(text = "OpenAI (GPT) API Key", color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        text = "Enter your OpenAI API key for GPT-4o intelligence and fallback routing. Stored on device only.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = openAiInput,
                        onValueChange = { openAiInput = it },
                        placeholder = "sk-proj-..."
                    )
                }
            },
            confirmButton = {
                NovaButton(
                    text = "Save Key",
                    onClick = {
                        ApiKeyStore.saveOpenAiKey(context, openAiInput.trim())
                        showOpenAiDialog = false
                    }
                )
            },
            dismissButton = {
                NovaButton(
                    text = "Cancel",
                    onClick = { showOpenAiDialog = false },
                    type = NovaButtonType.TEXT
                )
            },
            containerColor = colors.surfaceRaised
        )
    }

    // Anthropic Claude Key Dialog
    if (showClaudeDialog) {
        AlertDialog(
            onDismissRequest = { showClaudeDialog = false },
            title = { Text(text = "Anthropic Claude Key", color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        text = "Enter your Anthropic Claude API key for Claude 3.5 Sonnet coding and complex logic.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = claudeInput,
                        onValueChange = { claudeInput = it },
                        placeholder = "sk-ant-..."
                    )
                }
            },
            confirmButton = {
                NovaButton(
                    text = "Save Key",
                    onClick = {
                        ApiKeyStore.saveClaudeKey(context, claudeInput.trim())
                        showClaudeDialog = false
                    }
                )
            },
            dismissButton = {
                NovaButton(
                    text = "Cancel",
                    onClick = { showClaudeDialog = false },
                    type = NovaButtonType.TEXT
                )
            },
            containerColor = colors.surfaceRaised
        )
    }

    // Web Search API Key Dialog
    if (showSearchKeyDialog) {
        AlertDialog(
            onDismissRequest = { showSearchKeyDialog = false },
            title = { Text(text = "Web Search Provider Key", color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        text = "Enter your search provider key (Tavily, SerpApi, or Brave Search) for grounded web fact-checking.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = searchKeyInput,
                        onValueChange = { searchKeyInput = it },
                        placeholder = "tvly-... or BSA..."
                    )
                }
            },
            confirmButton = {
                NovaButton(
                    text = "Save Key",
                    onClick = {
                        ApiKeyStore.saveSearchApiKey(context, searchKeyInput.trim())
                        showSearchKeyDialog = false
                    }
                )
            },
            dismissButton = {
                NovaButton(
                    text = "Cancel",
                    onClick = { showSearchKeyDialog = false },
                    type = NovaButtonType.TEXT
                )
            },
            containerColor = colors.surfaceRaised
        )
    }

    // Assistant Name Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(text = "Rename Assistant", color = colors.textPrimary) },
            text = {
                NovaTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = "Lia"
                )
            },
            confirmButton = {
                NovaButton(
                    text = "Save",
                    onClick = {
                        appState.applyAssistantName(context, nameInput)
                        showNameDialog = false
                    }
                )
            },
            dismissButton = {
                NovaButton(
                    text = "Cancel",
                    onClick = { showNameDialog = false },
                    type = NovaButtonType.TEXT
                )
            },
            containerColor = colors.surfaceRaised
        )
    }

    // Memory Management Dialog
    if (showMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Personal Memories", color = colors.textPrimary)
                    if (memoriesList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                UserMemoryStore.get(context).clearAll()
                                memoriesList = emptyList()
                            }
                        ) {
                            Text(
                                "Clear All",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            text = {
                if (memoriesList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No saved memories yet.",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Say \"yaad rakhna...\" to save preferences by voice or text.",
                            fontSize = 12.sp,
                            color = colors.textTertiary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        memoriesList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceGlass)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.content,
                                    fontSize = 13.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        UserMemoryStore.get(context).deleteMemory(item.id)
                                        memoriesList = UserMemoryStore.get(context).getAllMemories()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Memory",
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                NovaButton(
                    text = "Close",
                    onClick = { showMemoryDialog = false }
                )
            },
            containerColor = colors.surfaceRaised
        )
    }

    // Add Monitored Game Package Dialog
    if (showAddPackageDialog) {
        AlertDialog(
            onDismissRequest = { showAddPackageDialog = false },
            title = { Text(text = "Monitored Game Packages", color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        text = "The coach only analyzes screens while one of these packages is in the foreground:",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    gamePackages.forEach { pkg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceGlass)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pkg,
                                fontSize = 12.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (pkg !in CoachPreferences.DEFAULT_GAME_PACKAGES) {
                                IconButton(
                                    onClick = {
                                        CoachPreferences.removeGamePackage(context, pkg)
                                        gamePackages = CoachPreferences.getGamePackages(context)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Package",
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = newPackageInput,
                        onValueChange = { newPackageInput = it },
                        placeholder = "e.g. com.tencent.ig"
                    )
                }
            },
            confirmButton = {
                NovaButton(
                    text = "Add Game",
                    onClick = {
                        val trimmed = newPackageInput.trim().lowercase()
                        if (trimmed.isNotBlank() && trimmed.contains(".")) {
                            CoachPreferences.addGamePackage(context, trimmed)
                            gamePackages = CoachPreferences.getGamePackages(context)
                            newPackageInput = ""
                        }
                        showAddPackageDialog = false
                    }
                )
            },
            dismissButton = {
                NovaButton(
                    text = "Close",
                    onClick = { showAddPackageDialog = false },
                    type = NovaButtonType.TEXT
                )
            },
            containerColor = colors.surfaceRaised
        )
    }
}
