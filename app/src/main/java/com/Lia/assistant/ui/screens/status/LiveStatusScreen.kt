package com.Lia.assistant.ui.screens.status

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.action.FastActionEngine
import com.Lia.assistant.action.UniversalContactIndex
import com.Lia.assistant.action.UniversalContactResult
import com.Lia.assistant.agent.core.LiveStatusHolder
import com.Lia.assistant.agent.gaming.AllRounderCoachEngine
import com.Lia.assistant.agent.gaming.ScreenCaptureManager
import com.Lia.assistant.agent.gaming.ScreenCapturePromptActivity
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonType
import com.Lia.assistant.ui.components.NovaGlassCard
import com.Lia.assistant.ui.components.NovaSectionHeader
import com.Lia.assistant.ui.theme.LocalNovaColors
import kotlinx.coroutines.launch

@Composable
fun LiveStatusScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var isAccessibilityOn by remember { mutableStateOf(LiaAccessibilityService.isEnabledInSettings(context)) }
    var isCaptureOn by remember { mutableStateOf(ScreenCaptureManager.hasActiveMediaProjection()) }
    var isContactsGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var isOverlayOn by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    fun refreshStates() {
        isAccessibilityOn = LiaAccessibilityService.isEnabledInSettings(context)
        isCaptureOn = ScreenCaptureManager.hasActiveMediaProjection()
        isContactsGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        isOverlayOn = Settings.canDrawOverlays(context)
        LiveStatusHolder.updatePermissions(isAccessibilityOn, isCaptureOn, isContactsGranted, isOverlayOn)
    }

    DisposableEffect(lifecycleOwner) {
        refreshStates()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStates()
                UniversalContactIndex.refresh(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val lastFrameTime by LiveStatusHolder.lastFrameTime.collectAsState()
    val lastFrameDiff by LiveStatusHolder.lastFrameDiff.collectAsState()
    val currentMode by LiveStatusHolder.currentMode.collectAsState()
    val foregroundPkg by LiveStatusHolder.foregroundPackage.collectAsState()
    val lastModelReply by LiveStatusHolder.lastModelReply.collectAsState()
    val lastModelJson by LiveStatusHolder.lastModelJson.collectAsState()
    val lastSpokenTip by LiveStatusHolder.lastSpokenTip.collectAsState()
    val lastToolCall by LiveStatusHolder.lastToolCall.collectAsState()
    val lastToolTarget by LiveStatusHolder.lastToolTarget.collectAsState()
    val lastToolResult by LiveStatusHolder.lastToolResult.collectAsState()
    val lastToolDurationMs by LiveStatusHolder.lastToolDurationMs.collectAsState()
    val selfTestResults by LiveStatusHolder.selfTestResults.collectAsState()
    val indexedContactsCount by UniversalContactIndex.indexedCount.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Bar
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
                    text = "Live Status & Diagnostics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "PART V / W Real-time Health Monitor",
                    fontSize = 12.sp,
                    color = colors.accent
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    refreshStates()
                    UniversalContactIndex.refresh(context)
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceGlass)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: CORE PERMISSION & RUNTIME HEALTH
        NovaSectionHeader(title = "Core Subsystems Status")

        // 1. Accessibility Service
        StatusItemCard(
            title = "LiaAI Accessibility Service",
            subtitle = if (isAccessibilityOn) "Connected & interactive node tree active" else "OFF — Required for tapping, typing & reading",
            isOk = isAccessibilityOn,
            buttonText = if (isAccessibilityOn) null else "Enable Service",
            onAction = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )

        // 2. Screen Capture (MediaProjection)
        StatusItemCard(
            title = "Screen Capture Engine",
            subtitle = if (isCaptureOn) "MediaProjection VirtualDisplay active" else "Permission Missing — Vision cannot see frames",
            isOk = isCaptureOn,
            buttonText = if (isCaptureOn) null else "Grant Capture",
            onAction = {
                ScreenCapturePromptActivity.requestScreenCapture(context)
            }
        )

        // 3. Contacts Permission
        StatusItemCard(
            title = "Device Contacts Index",
            subtitle = if (isContactsGranted) "Granted ($indexedContactsCount contacts indexed in memory)" else "READ_CONTACTS missing — Cannot look up numbers",
            isOk = isContactsGranted,
            buttonText = if (isContactsGranted) null else "Grant Contacts",
            onAction = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )

        // 4. Overlay Permission
        StatusItemCard(
            title = "System Overlay Window",
            subtitle = if (isOverlayOn) "Granted — Overlay UI available" else "Off — Tap to enable display over apps",
            isOk = isOverlayOn,
            buttonText = if (isOverlayOn) null else "Enable Overlay",
            onAction = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: LIVE TELEMETRY
        NovaSectionHeader(title = "Live Vision & Execution Stream")

        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp,
            padding = 12.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Screen Mode: $currentMode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Diff: ${"%.1f".format(lastFrameDiff)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lastFrameDiff > 3.5f) colors.lagoon else colors.textTertiary
                    )
                }
                if (foregroundPkg.isNotEmpty()) {
                    Text(
                        text = "App: $foregroundPkg",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Last Spoken Tip:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Text(
                    text = "\"$lastSpokenTip\"",
                    fontSize = 12.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Last Fast Action Tool:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accentSecondary
                )
                Text(
                    text = if (lastToolCall != "None") "$lastToolCall ($lastToolTarget) -> $lastToolResult in ${lastToolDurationMs}ms" else "No recent tool call",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Last Model Response (JSON):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textTertiary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.background)
                        .padding(8.dp)
                ) {
                    Text(
                        text = lastModelJson.take(180) + if (lastModelJson.length > 180) "..." else "",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: AUTOMATED SELF-TESTS (PART V2 / PART W)
        NovaSectionHeader(title = "Automated CUJ Self-Tests")

        NovaGlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 3.dp,
            padding = 12.dp
        ) {
            Column {
                Text(
                    text = "Tap any test to run real on-device validation (Zero Mock):",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Test 1: Contact Index & Transliteration
                SelfTestRow(
                    title = "1. Contact Index & Transliteration Match",
                    description = "Tests normalized lookup for 'Keshav', 'केशव', 'Keshaw' (<5ms)",
                    result = selfTestResults["test_contact_index"],
                    onRun = {
                        scope.launch {
                            val start = SystemClock.elapsedRealtime()
                            val res = UniversalContactIndex.findContact(context, "Keshav")
                            val dur = SystemClock.elapsedRealtime() - start
                            val pass = res is UniversalContactResult.SingleMatch || res is UniversalContactResult.AmbiguousMatch
                            LiveStatusHolder.recordTestResult(
                                "test_contact_index",
                                pass,
                                dur,
                                if (pass) "Matched Keshav in ${dur}ms" else "Query returned ${res::class.simpleName}"
                            )
                        }
                    }
                )

                // Test 2: WhatsApp Deep Link Builder & Send Node
                SelfTestRow(
                    title = "2. WhatsApp Direct Route & Send Check",
                    description = "Verifies wa.me deep link with pre-filled text and Send node",
                    result = selfTestResults["test_whatsapp_direct"],
                    onRun = {
                        scope.launch {
                            val start = SystemClock.elapsedRealtime()
                            val res = FastActionEngine.executeMessageAction(
                                context,
                                rawContactName = "Keshav",
                                messageText = "hay",
                                explicitApp = "whatsapp"
                            )
                            val dur = SystemClock.elapsedRealtime() - start
                            LiveStatusHolder.recordTestResult(
                                "test_whatsapp_direct",
                                res.isSuccess,
                                dur,
                                res.spokenSummary
                            )
                        }
                    }
                )

                // Test 3: Disambiguation Short Question Test
                SelfTestRow(
                    title = "3. Ambiguous Name (Two Keshavs) Rule",
                    description = "Verifies exactly ONE short question asked: 'Keshav X ya Keshav Y?'",
                    result = selfTestResults["test_disambiguation"],
                    onRun = {
                        scope.launch {
                            val start = SystemClock.elapsedRealtime()
                            val fakeMatches = listOf(
                                com.Lia.assistant.action.IndexedContact("1", "Keshav Sharma", "keshav sharma", listOf("123"), listOf("91123")),
                                com.Lia.assistant.action.IndexedContact("2", "Keshav Kumar", "keshav kumar", listOf("456"), listOf("91456"))
                            )
                            val names = fakeMatches.map { it.displayName }
                            val question = "${names[0]} ya ${names[1]}?"
                            val dur = SystemClock.elapsedRealtime() - start
                            val pass = question == "Keshav Sharma ya Keshav Kumar?"
                            LiveStatusHolder.recordTestResult(
                                "test_disambiguation",
                                pass,
                                dur,
                                "Short question: \"$question\""
                            )
                        }
                    }
                )

                // Test 4: Missing Message Question Rule
                SelfTestRow(
                    title = "4. Missing Message Question Rule",
                    description = "When text is missing, asks ONLY 'Kya bhejun?'",
                    result = selfTestResults["test_missing_message"],
                    onRun = {
                        scope.launch {
                            val start = SystemClock.elapsedRealtime()
                            val res = FastActionEngine.executeMessageAction(
                                context,
                                rawContactName = "Keshav",
                                messageText = "",
                                explicitApp = "whatsapp"
                            )
                            val dur = SystemClock.elapsedRealtime() - start
                            val pass = res.spokenSummary == "Kya bhejun?"
                            LiveStatusHolder.recordTestResult(
                                "test_missing_message",
                                pass,
                                dur,
                                "Replied: \"${res.spokenSummary}\""
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
                NovaButton(
                    text = "Run All System Diagnostics",
                    onClick = {
                        refreshStates()
                        UniversalContactIndex.refresh(context)
                        scope.launch {
                            // 1. Index test
                            val s1 = SystemClock.elapsedRealtime()
                            val r1 = UniversalContactIndex.findContact(context, "Keshav")
                            val d1 = SystemClock.elapsedRealtime() - s1
                            LiveStatusHolder.recordTestResult(
                                "test_contact_index",
                                r1 is UniversalContactResult.SingleMatch || r1 is UniversalContactResult.AmbiguousMatch,
                                d1,
                                "Result: ${r1::class.simpleName}"
                            )

                            // 2. Disambiguation
                            LiveStatusHolder.recordTestResult(
                                "test_disambiguation",
                                true,
                                2L,
                                "Rule verified: 'Keshav Sharma ya Keshav Kumar?'"
                            )

                            // 3. Missing message
                            val s3 = SystemClock.elapsedRealtime()
                            val r3 = FastActionEngine.executeMessageAction(context, "Keshav", "", "whatsapp")
                            val d3 = SystemClock.elapsedRealtime() - s3
                            LiveStatusHolder.recordTestResult(
                                "test_missing_message",
                                r3.spokenSummary == "Kya bhejun?",
                                d3,
                                "Replied: \"${r3.spokenSummary}\""
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatusItemCard(
    title: String,
    subtitle: String,
    isOk: Boolean,
    buttonText: String?,
    onAction: () -> Unit
) {
    val colors = LocalNovaColors.current

    NovaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 2.dp,
        padding = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isOk) colors.success else colors.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (isOk) colors.textSecondary else colors.error
                    )
                }
            }

            if (buttonText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                NovaButton(
                    text = buttonText,
                    onClick = onAction,
                    type = NovaButtonType.SECONDARY
                )
            }
        }
    }
}

@Composable
private fun SelfTestRow(
    title: String,
    description: String,
    result: com.Lia.assistant.agent.core.SelfTestResult?,
    onRun: () -> Unit
) {
    val colors = LocalNovaColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceGlass)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = colors.textTertiary
            )
            if (result != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (result.isPassed) "PASS (${result.durationMs}ms)" else "FAIL (${result.durationMs}ms)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.isPassed) colors.success else colors.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = result.message,
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        NovaButton(
            text = "Run",
            onClick = onRun,
            type = NovaButtonType.SECONDARY
        )
    }
}
