package com.Lia.assistant.agent.gaming

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.Lia.assistant.accessibility.LiaAccessibilityService
import com.Lia.assistant.agent.core.LiveStatusHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

object AllRounderCoachEngine {
    private const val TAG = "AllRounderCoachEngine"

    private val scope = CoroutineScope(Dispatchers.Default)
    private var coachJob: Job? = null

    private val _coachState = MutableStateFlow(CoachState.IDLE)
    val coachState: StateFlow<CoachState> = _coachState.asStateFlow()

    private val _latestAlert = MutableStateFlow<TacticalAlert?>(null)
    val latestAlert: StateFlow<TacticalAlert?> = _latestAlert.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isMuted: Boolean = false
    private var appContext: Context? = null

    // Lobby stability timer
    private var lastLobbyScreenHash: Int = 0
    private var lobbyScreenStableMs: Long = 0L
    private var lastLobbyAdviceGiven: String = ""

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("hi", "IN")
                    try {
                        val attrs = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        tts?.setAudioAttributes(attrs)
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    fun startCoaching(context: Context, callerScope: CoroutineScope? = null): String {
        appContext = context.applicationContext
        initTts(context)

        val service = LiaAccessibilityService.instance
        if (service == null) {
            return "Screen monitor karne ke liye LiaAI ki Accessibility permission on honi chahiye."
        }

        if (_coachState.value == CoachState.MONITORING) {
            return "Screen Coach already active hai aur screen watch kar raha hai!"
        }

        _coachState.value = CoachState.MONITORING
        CoachPreferences.setCoachPersistedActive(context, true)

        if (!ScreenCaptureManager.hasActiveMediaProjection()) {
            ScreenCapturePromptActivity.requestScreenCapture(context)
        } else {
            AllRounderCoachService.start(context)
        }

        CoachAntiRepeatEngine.clear()
        ScreenCaptureManager.resetHash()
        lastLobbyScreenHash = 0
        lobbyScreenStableMs = 0L
        lastLobbyAdviceGiven = ""

        CoachDebugHolder.setStatus("Coach active. Watching screen...")
        LiveStatusHolder.updatePermissions(
            accessibility = LiaAccessibilityService.isConnected.value,
            capture = ScreenCaptureManager.hasActiveMediaProjection(),
            contacts = true,
            overlay = true
        )

        // First run test: speak within 3-5 seconds so user knows it works
        scope.launch {
            delay(1500)
            if (_coachState.value == CoachState.MONITORING) {
                speakDirect("coach chalu hai, main screen dekh raha hun")
            }
        }

        // Start instant barge-in stop listener
        BargeInStopListener.startListening(
            context = context,
            onStopTriggered = {
                stopCoaching()
            },
            onCommandTriggered = { cmd ->
                // User interrupted with another voice command: cut speech, stop pending tips, run command
                stopSpeaking()
                CoachDebugHolder.setStatus("Voice command heard: \"$cmd\"")
                callerScope?.launch(Dispatchers.Main) {
                    try {
                        com.Lia.assistant.action.ActionExecutor.execute(context, "launch_app", org.json.JSONObject().put("app_name", cmd))
                    } catch (_: Throwable) {}
                }
            }
        )

        // Launch coaching loop
        coachJob?.cancel()
        coachJob = scope.launch {
            Log.i(TAG, "Real-time Coach loop started")

            while (isActive && _coachState.value == CoachState.MONITORING) {
                val cycleStartTime = System.currentTimeMillis()
                val currentPackage = LiaAccessibilityService.currentPackage.value.lowercase()
                val mode = detectScreenMode(context, currentPackage)
                CoachDebugHolder.updateModeAndPackage(mode, currentPackage)

                // 1. Capture a fresh frame
                val frameResult = ScreenCaptureManager.captureFrame(context)
                var nextIntervalMs = 2000L

                when (frameResult) {
                    is FrameResult.CaptureFailed -> {
                        LiveStatusHolder.updateFrame(0f, mode.name, currentPackage)
                        CoachDebugHolder.recordDecision(
                            DecisionRecord(
                                mode = mode,
                                foregroundPackage = currentPackage,
                                diffPercent = 0f,
                                outcome = "SKIPPED_CAPTURE_FAILED",
                                tip = null,
                                reason = "Screen frame capture failed"
                            )
                        )
                        notifyCaptureMissingOnce("screen capture band hai, permission do")
                    }

                    is FrameResult.BlockedOrEmpty -> {
                        LiveStatusHolder.updateFrame(0f, mode.name, currentPackage)
                        CoachDebugHolder.recordDecision(
                            DecisionRecord(
                                mode = mode,
                                foregroundPackage = currentPackage,
                                diffPercent = 0f,
                                outcome = "SKIPPED_BLOCKED_CAPTURE",
                                tip = null,
                                reason = "Screen capture blocked or failed (blank / FLAG_SECURE)"
                            )
                        )
                        notifyCaptureMissingOnce("screen capture band hai, permission do")
                    }

                    is FrameResult.Unchanged -> {
                        nextIntervalMs = frameResult.adaptiveIntervalMs
                        LiveStatusHolder.updateFrame(frameResult.diffPercent, mode.name, currentPackage)
                        // If in lobby/settings, track stability duration
                        if (mode == CoachScreenMode.GAME_LOBBY || mode == CoachScreenMode.GAME_SETTINGS) {
                            handleLobbyStabilityCheck(context, frameResult.diffPercent)
                        } else {
                            CoachDebugHolder.recordDecision(
                                DecisionRecord(
                                    mode = mode,
                                    foregroundPackage = currentPackage,
                                    diffPercent = frameResult.diffPercent,
                                    outcome = "SKIPPED_UNCHANGED",
                                    tip = null,
                                    reason = "Screen unchanged (diff ${"%.1f".format(frameResult.diffPercent)}% < 3.5%)"
                                )
                            )
                        }
                    }

                    is FrameResult.Fresh -> {
                        nextIntervalMs = frameResult.adaptiveIntervalMs
                        lobbyScreenStableMs = 0L // Reset lobby timer on screen change
                        hasAnnouncedCaptureMissing = false
                        LiveStatusHolder.updateFrame(frameResult.diffPercent, mode.name, currentPackage)

                        processFreshFrame(
                            context = context,
                            mode = mode,
                            currentPackage = currentPackage,
                            frame = frameResult,
                            cycleStartTime = cycleStartTime
                        )
                    }
                }

                // Adaptive delay
                val elapsed = System.currentTimeMillis() - cycleStartTime
                val delayTime = (nextIntervalMs - elapsed).coerceAtLeast(600L)
                delay(delayTime)
            }
        }

        val freq = CoachPreferences.getFrequency(context)
        return "LiaAI Screen Coach ON! (Frequency: ${freq.label}). Disclose: Vision has 1-3 sec delay. Direct speech 'chup' or 'ruko' stops it instantly."
    }

    private suspend fun processFreshFrame(
        context: Context,
        mode: CoachScreenMode,
        currentPackage: String,
        frame: FrameResult.Fresh,
        cycleStartTime: Long
    ) {
        val freq = CoachPreferences.getFrequency(context)

        // 1. In Match: Fast on-device HUD checks (HP bar & damage flash)
        if (mode == CoachScreenMode.GAME_IN_MATCH) {
            val fastHudEvents = FastHudDetector.detectFastHudEvents(frame.bitmap)
            for (hudEvent in fastHudEvents) {
                val decision = CoachAntiRepeatEngine.evaluateEvent(hudEvent, freq, cycleStartTime)
                if (decision is EvaluationDecision.Approved) {
                    speakAlert(decision.tipToSpeak, hudEvent.urgency, hudEvent.type)
                    CoachDebugHolder.recordDecision(
                        DecisionRecord(
                            mode = mode,
                            foregroundPackage = currentPackage,
                            diffPercent = frame.diffPercent,
                            outcome = "SPOKEN",
                            tip = decision.tipToSpeak,
                            reason = "Fast HUD event: ${hudEvent.type}"
                        )
                    )
                    return // Fast HUD handled this cycle
                }
            }
        }

        // 2. Multimodal Vision Analysis (only report what is visible, JSON only)
        val recentTips = CoachAntiRepeatEngine.getRecentSpokenTips()
        val analysis = CoachVisionAnalyzer.analyzeFrame(context, frame.jpegBytes, mode, recentTips)
        CoachDebugHolder.updateModelJson(analysis.rawJson)

        if (analysis.events.isEmpty()) {
            CoachDebugHolder.recordDecision(
                DecisionRecord(
                    mode = mode,
                    foregroundPackage = currentPackage,
                    diffPercent = frame.diffPercent,
                    outcome = "SKIPPED_EMPTY_EVENTS",
                    tip = null,
                    reason = "Model returned no events (screen is normal / quiet)"
                )
            )
            return
        }

        // 3. Process returned events through anti-repeat and urgency filters
        for (event in analysis.events) {
            // Guard: in match tips ONLY given in match
            if (mode != CoachScreenMode.GAME_IN_MATCH && event.type in listOf("enemy_near", "zone_shrinking", "low_health")) {
                continue
            }

            val decision = CoachAntiRepeatEngine.evaluateEvent(event, freq, cycleStartTime)
            when (decision) {
                is EvaluationDecision.Approved -> {
                    speakAlert(decision.tipToSpeak, event.urgency, event.type)
                    CoachDebugHolder.recordDecision(
                        DecisionRecord(
                            mode = mode,
                            foregroundPackage = currentPackage,
                            diffPercent = frame.diffPercent,
                            outcome = "SPOKEN",
                            tip = decision.tipToSpeak,
                            reason = "Event: ${event.type} (Urgency: ${event.urgency}, Dir: ${event.direction})"
                        )
                    )
                    return // One tip at a time
                }
                is EvaluationDecision.Rejected -> {
                    CoachDebugHolder.recordDecision(
                        DecisionRecord(
                            mode = mode,
                            foregroundPackage = currentPackage,
                            diffPercent = frame.diffPercent,
                            outcome = "SKIPPED_ANTI_REPEAT",
                            tip = event.say,
                            reason = decision.reason
                        )
                    )
                }
            }
        }
    }

    private suspend fun handleLobbyStabilityCheck(context: Context, diffPercent: Float) {
        val delaySettingSec = CoachPreferences.getLobbyDelaySeconds(context)
        lobbyScreenStableMs += 2000L

        if (lobbyScreenStableMs >= delaySettingSec * 1000L) {
            lobbyScreenStableMs = 0L
            val frameResult = ScreenCaptureManager.captureFrame(context)
            if (frameResult is FrameResult.Fresh) {
                val recentTips = CoachAntiRepeatEngine.getRecentSpokenTips()
                val analysis = CoachVisionAnalyzer.analyzeFrame(context, frameResult.jpegBytes, CoachScreenMode.GAME_SETTINGS, recentTips)
                val advice = analysis.events.firstOrNull()?.say ?: ""
                if (advice.isNotBlank() && advice != lastLobbyAdviceGiven) {
                    lastLobbyAdviceGiven = advice
                    speakAlert(advice, 2, "lobby_advice")
                    CoachDebugHolder.recordDecision(
                        DecisionRecord(
                            mode = CoachScreenMode.GAME_SETTINGS,
                            foregroundPackage = LiaAccessibilityService.currentPackage.value,
                            diffPercent = diffPercent,
                            outcome = "SPOKEN",
                            tip = advice,
                            reason = "Lobby/Settings stable for ${delaySettingSec}s"
                        )
                    )
                }
            }
        }
    }

    private fun detectScreenMode(context: Context, currentPackage: String): CoachScreenMode {
        val gamePackages = CoachPreferences.getGamePackages(context)
        val isGame = gamePackages.any { currentPackage.contains(it) }

        if (isGame) {
            // Check Accessibility node tree to classify screen state
            val nodes = LiaAccessibilityService.instance?.readScreen() ?: emptyList()
            val text = nodes.joinToString(" ") { it.text }.lowercase()

            return when {
                text.contains("sensitivity") || text.contains("graphics") || text.contains("controls") || text.contains("sound") ->
                    CoachScreenMode.GAME_SETTINGS
                text.contains("start") || text.contains("ready") || text.contains("invite") || text.contains("matchmaking") ->
                    CoachScreenMode.GAME_LOBBY
                text.contains("vault") || text.contains("store") || text.contains("weapon") || text.contains("character") ->
                    CoachScreenMode.GAME_OTHER
                else ->
                    CoachScreenMode.GAME_IN_MATCH
            }
        }

        val browserPackages = setOf("com.android.chrome", "org.mozilla.firefox", "com.opera.browser", "com.brave.browser")
        if (browserPackages.any { currentPackage.contains(it) }) {
            return CoachScreenMode.BROWSER
        }

        return CoachScreenMode.OTHER_APP
    }

    /**
     * T1-STOP: Instant stop execution in under 1 second.
     */
    fun stopCoaching(): String {
        _coachState.value = CoachState.IDLE
        appContext?.let { CoachPreferences.setCoachPersistedActive(it, false) }

        // 1. Cut speech immediately
        stopSpeaking()

        // 2. Cancel loop & model jobs
        coachJob?.cancel()
        coachJob = null

        // 3. Stop barge-in listener & release screen capture
        BargeInStopListener.stopListening()
        ScreenCaptureManager.releaseMediaProjection()

        // 4. Stop foreground service
        appContext?.let { AllRounderCoachService.stop(it) }

        // 5. Update debug telemetry
        CoachDebugHolder.setStatus("Coach band (Stopped)")
        CoachDebugHolder.recordDecision(
            DecisionRecord(
                mode = CoachScreenMode.OTHER_APP,
                foregroundPackage = "",
                diffPercent = 0f,
                outcome = "STOPPED",
                tip = null,
                reason = "Coach stopped by user voice or manual action"
            )
        )

        return "Coach band kar diya hai. Silent mode active."
    }

    fun pauseCoaching(): String {
        if (_coachState.value == CoachState.MONITORING) {
            _coachState.value = CoachState.PAUSED
            stopSpeaking()
            CoachDebugHolder.setStatus("Coach paused")
            return "Coach pause ho gaya hai."
        }
        return "Coach active nahi hai."
    }

    fun resumeCoaching(): String {
        if (_coachState.value == CoachState.PAUSED) {
            _coachState.value = CoachState.MONITORING
            CoachDebugHolder.setStatus("Coach resumed")
            return "Coach resume ho gaya hai."
        }
        return "Coach paused nahi tha."
    }

    private fun speakAlert(text: String, urgency: Int, type: String) {
        if (isMuted || _coachState.value != CoachState.MONITORING) return

        CoachAntiRepeatEngine.recordSpoken(text, type, urgency)
        val alert = TacticalAlert(
            text = text,
            priority = when (urgency) {
                3 -> AlertPriority.CRITICAL
                2 -> AlertPriority.TACTICAL
                else -> AlertPriority.INFO
            }
        )
        _latestAlert.value = alert
        BargeInStopListener.updateCurrentlySpeakingTip(text)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.45f) // Low volume over game audio
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, alert.id)
    }

    private fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Throwable) {}
        BargeInStopListener.updateCurrentlySpeakingTip("")
    }

    private var hasAnnouncedCaptureMissing: Boolean = false

    fun notifyCaptureMissing(context: Context) {
        initTts(context)
        notifyCaptureMissingOnce("screen capture band hai, permission do")
    }

    private fun notifyCaptureMissingOnce(message: String) {
        if (!hasAnnouncedCaptureMissing) {
            hasAnnouncedCaptureMissing = true
            speakDirect(message)
            LiveStatusHolder.updateSpokenTip(message)
        }
    }

    fun speakDirect(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "direct_announcement")
            LiveStatusHolder.updateSpokenTip(text)
        } catch (_: Throwable) {}
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) stopSpeaking()
        return isMuted
    }
}

