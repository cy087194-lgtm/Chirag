package com.Lia.assistant.ui.screens.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.forge.ForgeIntent
import com.Lia.assistant.ui.components.ChatMessage
import com.Lia.assistant.ui.components.NovaMessageBubble
import com.Lia.assistant.ui.components.NovaTextField
import com.Lia.assistant.ui.components.NovaTypingDots
import com.Lia.assistant.ui.fx.LiaOrb3D
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.depthSurface
import com.Lia.assistant.ui.theme.nightSky
import com.Lia.assistant.voice.ChatPromptBuilder
import com.Lia.assistant.voice.ChatReplyResult
import com.Lia.assistant.voice.ChatTurn
import com.Lia.assistant.voice.GeminiTextClient
import com.Lia.assistant.voice.NovaOrbState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ChatScreen(
    navController: NavController,
    appState: NovaAppState,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Hello! I am ${appState.assistantName}. How can I assist you today?",
                isUser = false
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    val isListening by com.Lia.assistant.voice.DebouncedSpeechInputManager.isListening.collectAsState()
    val speechBuffer by com.Lia.assistant.voice.DebouncedSpeechInputManager.currentBuffer.collectAsState()

    LaunchedEffect(speechBuffer) {
        if (speechBuffer.isNotEmpty()) {
            inputText = speechBuffer
        }
    }

    // TTS lifecycle
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
            com.Lia.assistant.voice.DebouncedSpeechInputManager.stopListening()
        }
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        messages.add(ChatMessage(text = trimmed, isUser = true))
        inputText = ""

        // Check Website Forge intent
        val forgePrompt = ForgeIntent.extract(trimmed)
        if (forgePrompt != null) {
            messages.add(
                ChatMessage(
                    text = "Opening the Website Forge for: \"$forgePrompt\"...",
                    isUser = false
                )
            )
            ForgeController.start(context, forgePrompt)
            navController.navigate("forge")
            return
        }

        // Smart Intent Parsing (Negation, WhatsApp Multi-Step, Game Coach, Web Research)
        val parsedIntent = com.Lia.assistant.agent.SmartIntentParser.parse(trimmed)
        when (parsedIntent) {
            is com.Lia.assistant.agent.ParsedIntent.NegatedCommand -> {
                messages.add(
                    ChatMessage(
                        text = "Aapne kaha \"${parsedIntent.prohibitedAction}\" MAT karna, isliye maine ye action cancel kar diya hai.",
                        isUser = false
                    )
                )
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.WhatsAppMultiStep -> {
                messages.add(
                    ChatMessage(
                        text = "WhatsApp kholkar ${parsedIntent.contactName} ko \"${parsedIntent.messageText}\" bhej rahi hoon...",
                        isUser = false
                    )
                )
                scope.launch {
                    val res = com.Lia.assistant.agent.AutonomousAgentEngine.executeWhatsAppTask(
                        context,
                        parsedIntent.contactName,
                        parsedIntent.messageText
                    ) { _ -> }
                    withContext(Dispatchers.Main) {
                        messages.add(ChatMessage(text = res.summary, isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.UniversalMessageCommand -> {
                scope.launch {
                    val res = com.Lia.assistant.action.FastActionEngine.executeMessageAction(
                        context,
                        parsedIntent.contactName,
                        parsedIntent.messageText,
                        parsedIntent.app
                    )
                    withContext(Dispatchers.Main) {
                        messages.add(ChatMessage(text = res.spokenSummary, isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.DirectCallCommand -> {
                scope.launch {
                    val lookup = com.Lia.assistant.action.UniversalContactIndex.findContact(context, parsedIntent.contactName)
                    val reply = when (lookup) {
                        is com.Lia.assistant.action.UniversalContactResult.SingleMatch -> {
                            com.Lia.assistant.action.CommIntents.placeCall(context, lookup.chosenNumber)
                            "${lookup.contact.displayName} ko call lagaya."
                        }
                        is com.Lia.assistant.action.UniversalContactResult.AmbiguousMatch -> {
                            val names = lookup.candidates.map { it.displayName }.take(2)
                            "${names.getOrNull(0)} ya ${names.getOrNull(1)}?"
                        }
                        is com.Lia.assistant.action.UniversalContactResult.NoPermission -> {
                            "Contacts permission enable karein."
                        }
                        is com.Lia.assistant.action.UniversalContactResult.NotFound -> {
                            "${parsedIntent.contactName} contact nahi mila."
                        }
                    }
                    withContext(Dispatchers.Main) {
                        messages.add(ChatMessage(text = reply, isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.AppDownloadHelp -> {
                val guide = com.Lia.assistant.util.ApkExportHelper.getTroubleshootingGuide()
                messages.add(ChatMessage(text = guide, isUser = false))
                com.Lia.assistant.util.ApkExportHelper.shareApk(context)
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.TeachModeCommand -> {
                when (parsedIntent.action) {
                    com.Lia.assistant.agent.TeachAction.START_LEARNING -> {
                        val reply = com.Lia.assistant.agent.demonstration.DemonstrationRecorder.startLearning(context)
                        messages.add(ChatMessage(text = reply, isUser = false))
                    }
                    com.Lia.assistant.agent.TeachAction.STOP_LEARNING -> {
                        val (actions, msg) = com.Lia.assistant.agent.demonstration.DemonstrationRecorder.stopLearning(context)
                        if (actions.isNotEmpty()) {
                            try {
                                val routine = com.Lia.assistant.agent.demonstration.RoutineCompiler.compile(context, actions)
                                com.Lia.assistant.agent.demonstration.LearnedRoutineStore.saveRoutine(context, routine)
                                messages.add(
                                    ChatMessage(
                                        text = "Routine save ho gayi: \"${routine.name}\"!\nTrigger: \"Jarvis, ${routine.trigger.phrase}\"\nDirect Route: ${if (routine.directRouteAvailable) "Enabled (High-Speed)" else "UI Accessibility"}",
                                        isUser = false
                                    )
                                )
                            } catch (e: Exception) {
                                messages.add(ChatMessage(text = "Routine compile nahi ho saki: ${e.message}", isUser = false))
                            }
                        } else {
                            messages.add(ChatMessage(text = msg, isUser = false))
                        }
                    }
                    com.Lia.assistant.agent.TeachAction.LIST_ROUTINES -> {
                        val routines = com.Lia.assistant.agent.demonstration.LearnedRoutineStore.getAllRoutines(context)
                        if (routines.isEmpty()) {
                            messages.add(ChatMessage(text = "Abhi koi seekhi hui routine nahi hai. 'seekhna shuru karo' bolkar naya task sikha sakte hain.", isUser = false))
                        } else {
                            val sb = StringBuilder("Seekhi hui routines (${routines.size}):\n")
                            routines.forEachIndexed { idx, r ->
                                sb.append("${idx + 1}. ${r.name} (Trigger: \"Jarvis, ${r.trigger.phrase}\")\n")
                            }
                            messages.add(ChatMessage(text = sb.toString().trim(), isUser = false))
                        }
                        navController.navigate("learned_routines")
                    }
                    com.Lia.assistant.agent.TeachAction.DELETE_ROUTINE -> {
                        val routine = com.Lia.assistant.agent.demonstration.LearnedRoutineStore.findRoutineByTrigger(context, parsedIntent.query)
                        if (routine != null) {
                            com.Lia.assistant.agent.demonstration.LearnedRoutineStore.deleteRoutine(context, routine.id)
                            messages.add(ChatMessage(text = "\"${routine.name}\" delete kar diya gaya hai.", isUser = false))
                        } else {
                            messages.add(ChatMessage(text = "\"${parsedIntent.query}\" naam ki routine nahi mili.", isUser = false))
                        }
                    }
                    com.Lia.assistant.agent.TeachAction.TOGGLE_ROUTINE -> {
                        val routine = com.Lia.assistant.agent.demonstration.LearnedRoutineStore.findRoutineByTrigger(context, parsedIntent.query)
                        if (routine != null) {
                            val newState = !routine.isEnabled
                            com.Lia.assistant.agent.demonstration.LearnedRoutineStore.updateRoutineEnabled(context, routine.id, newState)
                            messages.add(ChatMessage(text = "\"${routine.name}\" ab ${if (newState) "Active" else "Disabled"} hai.", isUser = false))
                        } else {
                            messages.add(ChatMessage(text = "Routine nahi mili.", isUser = false))
                        }
                    }
                    com.Lia.assistant.agent.TeachAction.RELEARN -> {
                        val reply = com.Lia.assistant.agent.demonstration.DemonstrationRecorder.startLearning(context)
                        messages.add(ChatMessage(text = "Dobara seekh rahi hoon. Screen par action perform karein.", isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.ExecuteLearnedRoutineCommand -> {
                scope.launch {
                    val routine = com.Lia.assistant.agent.demonstration.LearnedRoutineStore.getRoutineById(context, parsedIntent.routineId)
                    if (routine != null) {
                        val res = com.Lia.assistant.agent.demonstration.RoutineExecutionEngine.executeRoutine(context, routine)
                        withContext(Dispatchers.Main) {
                            messages.add(ChatMessage(text = res.spokenSummary, isUser = false))
                        }
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.GameCoachCommand -> {
                val coach = com.Lia.assistant.agent.gaming.FreeFireGameCoach
                val coachScope = CoroutineScope(Dispatchers.Main)
                val reply = when (parsedIntent.action) {
                    com.Lia.assistant.agent.GameCoachAction.START -> coach.startCoaching(context, coachScope)
                    com.Lia.assistant.agent.GameCoachAction.STOP -> coach.stopCoaching()
                    com.Lia.assistant.agent.GameCoachAction.PAUSE -> coach.pauseCoaching()
                    com.Lia.assistant.agent.GameCoachAction.RESUME -> coach.resumeCoaching()
                    com.Lia.assistant.agent.GameCoachAction.STATUS -> "Free Fire Coach Status: ${coach.coachState.value}"
                }
                messages.add(ChatMessage(text = reply, isUser = false))
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.WebResearchCommand -> {
                isTyping = true
                scope.launch {
                    val report = com.Lia.assistant.research.WebResearchEngine.research(
                        parsedIntent.query,
                        parsedIntent.topicCategory
                    )
                    withContext(Dispatchers.Main) {
                        isTyping = false
                        val builder = StringBuilder()
                        builder.append(report.summary)
                        if (report.keyPoints.isNotEmpty()) {
                            builder.append("\n\nKey Highlights:\n")
                            report.keyPoints.forEach { builder.append("• $it\n") }
                        }
                        if (report.sources.isNotEmpty()) {
                            builder.append("\nSources: ${report.sources.joinToString(", ")}")
                        }
                        messages.add(ChatMessage(text = builder.toString().trim(), isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.OneShotCompositeCommand -> {
                messages.add(
                    ChatMessage(
                        text = "Full-Speed One-Shot Mode: Ek plan banakar phone par back-to-back execute kar rahi hoon...",
                        isUser = false
                    )
                )
                scope.launch {
                    val oneShotRes = com.Lia.assistant.agent.OneShotExecutionEngine.executeOneShotCommand(
                        context,
                        parsedIntent.rawCommand
                    )
                    withContext(Dispatchers.Main) {
                        messages.add(
                            ChatMessage(
                                text = "${oneShotRes.voiceSummary} (Completed ${oneShotRes.completedSteps}/${oneShotRes.totalSteps} steps)",
                                isUser = false
                            )
                        )
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.AlwaysWatchCommand -> {
                val reply = if (parsedIntent.isStart) {
                    com.Lia.assistant.agent.vision.AlwaysWatchManager.startWatching(context, parsedIntent.condition)
                } else {
                    com.Lia.assistant.agent.vision.AlwaysWatchManager.stopWatching()
                }
                messages.add(ChatMessage(text = reply, isUser = false))
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.ScreenIntelligenceCommand -> {
                isTyping = true
                scope.launch {
                    val reply = when (parsedIntent.actionType) {
                        "translate" -> com.Lia.assistant.agent.vision.ScreenIntelligenceEngine.translateScreen(context)
                        "question" -> com.Lia.assistant.agent.vision.ScreenIntelligenceEngine.answerScreenQuestion(context, parsedIntent.query)
                        "scam_check" -> {
                            val service = com.Lia.assistant.accessibility.LiaAccessibilityService.instance
                            val texts = service?.readScreen()?.map { it.text } ?: emptyList()
                            com.Lia.assistant.agent.vision.ScreenIntelligenceEngine.scanForScam(texts).warning
                        }
                        "copy_text" -> com.Lia.assistant.agent.vision.ScreenIntelligenceEngine.copyScreenTextToClipboard(context)
                        else -> "Screen command process ho gaya."
                    }
                    withContext(Dispatchers.Main) {
                        isTyping = false
                        messages.add(ChatMessage(text = reply, isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.FocusModeCommand -> {
                val reply = com.Lia.assistant.agent.system.SystemAndDailyEngine.toggleFocusMode(parsedIntent.enable)
                messages.add(ChatMessage(text = reply, isUser = false))
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.AutoClickerCommand -> {
                val reply = com.Lia.assistant.agent.control.MacroEngine.startAutoClicker(parsedIntent.target, parsedIntent.intervalSeconds)
                messages.add(ChatMessage(text = reply, isUser = false))
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.FeatureRegistryQuery -> {
                val summaryText = com.Lia.assistant.registry.FeatureRegistry.getSummaryVoiceText(parsedIntent.filter)
                messages.add(ChatMessage(text = summaryText, isUser = false))
                navController.navigate("features")
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.StudyAndBrainCommand -> {
                isTyping = true
                scope.launch {
                    val reply = when (parsedIntent.subType) {
                        "game_review" -> com.Lia.assistant.agent.gaming.GameCoachEngine.getPostMatchReview()
                        "game_guide" -> com.Lia.assistant.agent.gaming.GameCoachEngine.getGameGuide(parsedIntent.query)
                        "save_diary" -> com.Lia.assistant.agent.study.StudyBrainEngine.saveDiaryEntry(context, parsedIntent.query, "Reflective")
                        "second_brain" -> com.Lia.assistant.agent.study.StudyBrainEngine.searchSecondBrain(context, parsedIntent.query)
                        "english_practice" -> com.Lia.assistant.agent.study.StudyBrainEngine.practiceEnglishConversation(context, parsedIntent.query)
                        "interview_practice" -> com.Lia.assistant.agent.study.StudyBrainEngine.mockInterviewFeedback(context, "Software Engineer", parsedIntent.query)
                        "decision_helper" -> com.Lia.assistant.agent.study.StudyBrainEngine.compareDecisionOptions(context, parsedIntent.query)
                        "creative_writing" -> com.Lia.assistant.agent.study.StudyBrainEngine.writeCreativeContent(context, "Shayari/Poem", parsedIntent.query)
                        "math_solve" -> com.Lia.assistant.agent.study.StudyBrainEngine.solveMathOrScience(context, parsedIntent.query)
                        else -> com.Lia.assistant.agent.study.StudyBrainEngine.explainConceptOrQuestion(context, parsedIntent.query)
                    }
                    withContext(Dispatchers.Main) {
                        isTyping = false
                        messages.add(ChatMessage(text = reply, isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.PhotoFeatureCommand -> {
                isTyping = true
                scope.launch {
                    val reply = when (parsedIntent.subType) {
                        "identify" -> com.Lia.assistant.agent.photo.PhotoFeaturesEngine.identifyObjectOrLabel(context, parsedIntent.query)
                        "scan_pdf" -> com.Lia.assistant.agent.photo.PhotoFeaturesEngine.scanDocumentToPdf(context, "Document", parsedIntent.query)
                        "image_gen" -> com.Lia.assistant.agent.photo.PhotoFeaturesEngine.generateAiImage(context, parsedIntent.query)
                        else -> "Photo feature executed for: ${parsedIntent.query}"
                    }
                    withContext(Dispatchers.Main) {
                        isTyping = false
                        messages.add(ChatMessage(text = reply, isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.MoneyAndWorkCommand -> {
                isTyping = true
                scope.launch {
                    val reply = when (parsedIntent.subType) {
                        "expense_summary" -> com.Lia.assistant.agent.work.MoneyAndWorkEngine.getExpenseSummary(context)
                        "price_alert" -> com.Lia.assistant.agent.work.MoneyAndWorkEngine.getPriceAlert(context, parsedIntent.query)
                        "pro_writing" -> com.Lia.assistant.agent.work.MoneyAndWorkEngine.writeProfessionalDoc(context, "Formal Document", parsedIntent.query)
                        "meeting_summary" -> com.Lia.assistant.agent.work.MoneyAndWorkEngine.summarizeMeeting(context, parsedIntent.query)
                        else -> "Money & Work assistant ready."
                    }
                    withContext(Dispatchers.Main) {
                        isTyping = false
                        messages.add(ChatMessage(text = reply, isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.DailyLifeCommand -> {
                isTyping = true
                scope.launch {
                    val reply = when (parsedIntent.subType) {
                        "fridge_recipe" -> com.Lia.assistant.agent.daily.DailyLifeEngine.getRecipeFromFridge(context, parsedIntent.query)
                        "fitness" -> com.Lia.assistant.agent.daily.DailyLifeEngine.getFitnessPlanOrRepCount(context, parsedIntent.query)
                        "travel" -> com.Lia.assistant.agent.daily.DailyLifeEngine.getTravelOrTransitStatus(context, parsedIntent.query)
                        "shopping_compare" -> com.Lia.assistant.agent.daily.DailyLifeEngine.compareProductPrices(context, parsedIntent.query)
                        "morning_briefing" -> com.Lia.assistant.agent.daily.DailyLifeEngine.getMorningBriefingOrCricket(context, parsedIntent.query)
                        "panchang" -> com.Lia.assistant.agent.daily.DailyLifeEngine.getPanchangAndFestivals(context, parsedIntent.query)
                        "song_id" -> com.Lia.assistant.agent.daily.DailyLifeEngine.identifySong(context, parsedIntent.query)
                        else -> "Daily life assistance ready."
                    }
                    withContext(Dispatchers.Main) {
                        isTyping = false
                        messages.add(ChatMessage(text = reply, isUser = false))
                    }
                }
                return
            }

            is com.Lia.assistant.agent.ParsedIntent.SmartSystemCommand -> {
                val reply = when (parsedIntent.subType) {
                    "weekly_report" -> com.Lia.assistant.agent.system.SmartSystemEngine.getWeeklyReport(context)
                    "iron_man" -> {
                        com.Lia.assistant.agent.system.SmartSystemEngine.setPersonality(context, com.Lia.assistant.agent.system.PersonalityStyle.IRON_MAN_SIR)
                        "Certainly, Sir. Systems online and calibrated in JARVIS mode. How may I assist you today?"
                    }
                    "hands_free" -> {
                        com.Lia.assistant.agent.system.SmartSystemEngine.setWakeWordEnabled(context, true)
                        "Hands-free wake word mode active! Say 'Hey Lia' or 'LiaAI' anytime."
                    }
                    else -> "System command processed."
                }
                messages.add(ChatMessage(text = reply, isUser = false))
                return
            }

            else -> {
                // Continue to individual command checks and Multi-Model Router
            }
        }

        val lower = trimmed.lowercase()

        // PART Z: Check if speech matches any user-demonstrated learned routine trigger
        val matchedRoutine = com.Lia.assistant.agent.demonstration.RoutineExecutionEngine.findMatchingRoutine(
            context = context,
            rawSpeech = trimmed,
            isExplicitWakeWordPresent = true
        )
        if (matchedRoutine != null) {
            scope.launch {
                val res = com.Lia.assistant.agent.demonstration.RoutineExecutionEngine.executeRoutine(context, matchedRoutine)
                withContext(Dispatchers.Main) {
                    messages.add(ChatMessage(text = res.spokenSummary, isUser = false))
                }
            }
            return
        }

        // 1. Check Home command
        if (lower in listOf("home per ja", "home pe ja", "home jao", "home par jao", "go home", "home screen", "home screen kholo", "home")) {
            scope.launch {
                com.Lia.assistant.action.ActionExecutor.execute(context, "go_home", org.json.JSONObject())
            }
            messages.add(ChatMessage(text = "Home screen par bhej diya!", isUser = false))
            return
        }

        // 2. Check Settings command
        if (lower.contains("setting") && (lower.contains("khol") || lower.contains("open") || lower.contains("chalu"))) {
            scope.launch {
                com.Lia.assistant.action.ActionExecutor.execute(context, "open_settings", org.json.JSONObject())
            }
            messages.add(ChatMessage(text = "Settings khol diya!", isUser = false))
            return
        }

        // 3. Check Camera command
        if ((lower.contains("camera") || lower.contains("photo")) && (lower.contains("khol") || lower.contains("open") || lower.contains("chalu"))) {
            scope.launch {
                com.Lia.assistant.action.ActionExecutor.execute(context, "open_camera", org.json.JSONObject())
            }
            messages.add(ChatMessage(text = "Camera khol raha hoon...", isUser = false))
            return
        }

        // 4. Check Volume command
        if (lower.contains("volume") || lower.contains("aawaz") || lower.contains("awaz")) {
            val dir = if (lower.contains("badhao") || lower.contains("up") || lower.contains("tez") || lower.contains("increase")) "up"
                      else if (lower.contains("kam") || lower.contains("down") || lower.contains("dheere") || lower.contains("decrease")) "down"
                      else if (lower.contains("mute") || lower.contains("band")) "mute" else "up"
            scope.launch {
                val args = org.json.JSONObject().put("direction", dir)
                com.Lia.assistant.action.ActionExecutor.execute(context, "set_volume", args)
            }
            messages.add(ChatMessage(text = "Volume adjust kar diya!", isUser = false))
            return
        }

        // 5. Check App opening command (e.g. "whatsapp khol", "youtube chalao")
        val isOpenAppCmd = lower.startsWith("open ") || lower.contains("khol") || lower.contains("chalao") || lower.startsWith("launch ")
        if (isOpenAppCmd) {
            val appQuery = com.Lia.assistant.action.AppLauncher.cleanQuery(trimmed)
            if (appQuery.isNotBlank() && appQuery != lower) {
                scope.launch {
                    val args = org.json.JSONObject().put("app_name", appQuery)
                    val res = com.Lia.assistant.action.ActionExecutor.execute(context, "open_app", args)
                    withContext(Dispatchers.Main) {
                        val status = res.optString("status")
                        if (status == "success") {
                            messages.add(ChatMessage(text = "$appQuery khol raha hoon...", isUser = false))
                        } else {
                            messages.add(ChatMessage(text = "$appQuery app nahi mili. Kripya check karein ki app installed hai ya nahi.", isUser = false))
                        }
                    }
                }
                return
            }
        }

        // 6. Check Battery command
        if (lower.contains("battery") || lower.contains("charging")) {
            scope.launch {
                val res = com.Lia.assistant.action.ActionExecutor.execute(context, "get_battery_status", org.json.JSONObject())
                withContext(Dispatchers.Main) {
                    val summary = res.optString("summary", "Battery info unavailable")
                    messages.add(ChatMessage(text = summary, isUser = false))
                }
            }
            return
        }

        // 7. Check Torch / Flashlight command
        if (lower.contains("torch") || lower.contains("flashlight")) {
            val state = if (lower.contains("on") || lower.contains("chalu") || lower.contains("jalao")) "on"
                        else if (lower.contains("off") || lower.contains("band") || lower.contains("bujhao")) "off"
                        else "toggle"
            scope.launch {
                val args = org.json.JSONObject().put("state", state)
                val res = com.Lia.assistant.action.ActionExecutor.execute(context, "toggle_torch", args)
                withContext(Dispatchers.Main) {
                    val isOn = res.optBoolean("is_on", false)
                    messages.add(ChatMessage(text = if (isOn) "Flashlight chalu kar di!" else "Flashlight band kar di!", isUser = false))
                }
            }
            return
        }

        // 8. Check Media command (play, pause, next)
        if (lower in listOf("pause", "gana roko", "music pause", "stop music", "play", "play music", "gana chalao", "next song", "agla gana")) {
            val cmd = if (lower.contains("pause") || lower.contains("roko")) "pause"
                      else if (lower.contains("next") || lower.contains("agla")) "next"
                      else "play"
            scope.launch {
                val args = org.json.JSONObject().put("command", cmd)
                com.Lia.assistant.action.ActionExecutor.execute(context, "media_control", args)
            }
            messages.add(ChatMessage(text = "Media command sent: $cmd", isUser = false))
            return
        }

        // 9. Check Memory command ("yaad rakhna...", "kya yaad hai", "bhool jao")
        if (lower.startsWith("yaad rakh") || lower.contains("yaad rakhna") || lower.startsWith("remember ")) {
            val fact = trimmed.replace(Regex("^(yaad rakhna|yaad rakh|remember to|remember that|remember)\\s*[:,-]?\\s*", RegexOption.IGNORE_CASE), "")
            if (fact.isNotBlank()) {
                scope.launch {
                    val args = org.json.JSONObject().put("fact", fact)
                    val res = com.Lia.assistant.action.ActionExecutor.execute(context, "save_memory", args)
                    withContext(Dispatchers.Main) {
                        messages.add(ChatMessage(text = res.optString("message", "Maine yaad rakh liya!"), isUser = false))
                    }
                }
                return
            }
        }

        if (lower.contains("kya yaad hai") || lower.contains("kuch yaad hai") || lower.contains("what do you remember") || lower.contains("meri details")) {
            scope.launch {
                val res = com.Lia.assistant.action.ActionExecutor.execute(context, "get_memories", org.json.JSONObject())
                withContext(Dispatchers.Main) {
                    messages.add(ChatMessage(text = res.optString("message", "Mujhe abhi kuch yaad nahi hai."), isUser = false))
                }
            }
            return
        }

        if (lower.contains("bhool jao") || lower.startsWith("forget ") || lower == "clear memory" || lower == "sab delete kar do") {
            val query = if (lower.contains("sab") || lower.contains("all") || lower == "bhool jao") "all" else trimmed.replace(Regex("^(bhool jao|forget)\\s*", RegexOption.IGNORE_CASE), "")
            scope.launch {
                val args = org.json.JSONObject().put("query", query)
                val res = com.Lia.assistant.action.ActionExecutor.execute(context, "forget_memory", args)
                withContext(Dispatchers.Main) {
                    messages.add(ChatMessage(text = res.optString("message", "Memory update kar di!"), isUser = false))
                }
            }
            return
        }

        // 10. Check Screen reading and Gaming strategy commands
        val isScreenQuery = lower.contains("screen par kya") || lower.contains("screen padho") ||
                lower.contains("screen dekho") || lower.contains("screen dekh") ||
                lower.contains("screen samjhao") || lower.contains("read screen") ||
                lower.contains("kya likha hai") || lower.contains("game strategy") ||
                lower.contains("game mein kya strategy") || lower.contains("game tips")

        if (isScreenQuery) {
            val isGameContext = lower.contains("game") || lower.contains("strategy")
            scope.launch {
                val args = org.json.JSONObject().put("context", if (isGameContext) "game" else "general")
                val res = com.Lia.assistant.action.ActionExecutor.execute(context, "explain_screen", args)
                withContext(Dispatchers.Main) {
                    val msg = res.optString("message", "Screen analysis complete.")
                    messages.add(ChatMessage(text = msg, isUser = false))
                }
            }
            return
        }

        // Call Multi-Model Router (Gemini + GPT + Claude with automatic fallback)
        isTyping = true
        scope.launch(Dispatchers.IO) {
            val isCoding = lower.contains("code") || lower.contains("function") || lower.contains("bug") ||
                    lower.contains("kotlin") || lower.contains("python") || lower.contains("debug") || lower.contains("fix")
            val isImageGen = lower.contains("generate image") || lower.contains("image banao") || lower.contains("photo banao") || lower.contains("draw ")

            if (isImageGen) {
                val imagePrompt = trimmed.replace(Regex("(?i)^(generate image|image banao|photo banao|draw)\\s*"), "")
                val (ok, res) = com.Lia.assistant.brain.OpenAiProvider(context).generateImage(imagePrompt)
                withContext(Dispatchers.Main) {
                    isTyping = false
                    val replyText = if (ok) "Generated image URL: $res" else res
                    messages.add(ChatMessage(text = replyText, isUser = false))
                }
                return@launch
            }

            val memories = com.Lia.assistant.data.UserMemoryStore.get(context).getMemoriesSummary()
            val systemInstruction = ChatPromptBuilder.build(
                personality = appState.personality,
                language = appState.language,
                assistantName = appState.assistantName,
                userMemories = memories
            )

            val taskType = if (isCoding) com.Lia.assistant.brain.TaskType.CODING else com.Lia.assistant.brain.TaskType.GENERAL_CHAT
            val historyTurns = messages.map { com.Lia.assistant.brain.ChatMessageTurn(isUser = it.isUser, text = it.text) }

            val result = com.Lia.assistant.brain.ModelRouter.executeWithFallback(
                context,
                taskType,
                systemInstruction,
                historyTurns
            )

            withContext(Dispatchers.Main) {
                isTyping = false
                when (result) {
                    is com.Lia.assistant.brain.ProviderReply.Success -> {
                        // Word-by-word reveal (20ms per word)
                        val fullReply = result.text
                        val words = fullReply.split(" ")
                        val placeholderId = java.util.UUID.randomUUID().toString()
                        val msg = ChatMessage(id = placeholderId, text = "", isUser = false, isStreaming = true)
                        messages.add(msg)

                        scope.launch {
                            val builder = StringBuilder()
                            for (w in words) {
                                if (builder.isNotEmpty()) builder.append(" ")
                                builder.append(w)
                                val idx = messages.indexOfFirst { it.id == placeholderId }
                                if (idx != -1) {
                                    messages[idx] = messages[idx].copy(text = builder.toString())
                                }
                                delay(20)
                            }
                            val finalIdx = messages.indexOfFirst { it.id == placeholderId }
                            if (finalIdx != -1) {
                                messages[finalIdx] = messages[finalIdx].copy(isStreaming = false)
                            }
                        }
                    }
                    is com.Lia.assistant.brain.ProviderReply.Error -> {
                        messages.add(
                            ChatMessage(
                                text = "AI Provider Error: ${result.error}",
                                isUser = false,
                                isError = true
                            )
                        )
                    }
                }
            }
        }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                com.Lia.assistant.voice.DebouncedSpeechInputManager.startListening(context) { finalCmd ->
                    sendMessage(finalCmd)
                }
            } catch (e: Throwable) {
                Toast.makeText(context, "Voice input unavailable", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice command", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .imePadding()
    ) {
        // Top Header
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

            // Small 40dp 3D orb in header
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { navController.navigate("voice") },
                contentAlignment = Alignment.Center
            ) {
                LiaOrb3D(
                    state = if (isTyping) NovaOrbState.THINKING else NovaOrbState.IDLE,
                    size = 40.dp,
                    amplitude = if (isTyping) 0.3f else 0f,
                    assistantName = appState.assistantName
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appState.assistantName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = if (isTyping) "Typing..." else "Here to help",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                NovaMessageBubble(
                    message = msg,
                    onSpeakClick = {
                        tts?.speak(msg.text, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    onCopyClick = {
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clip?.setPrimaryClip(ClipData.newPlainText("Lia message", msg.text))
                    },
                    onRetryClick = {
                        sendMessage(messages.lastOrNull { it.isUser }?.text ?: "")
                    }
                )
            }

            if (isTyping) {
                item {
                    NovaTypingDots(modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
                }
            }
        }

        // Floating Glass Input Bar
        val inputShape = RoundedCornerShape(28.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .depthSurface(shape = inputShape, elevation = 4.dp)
                .clip(inputShape)
                .background(colors.surfaceGlass)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NovaTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = "Message ${appState.assistantName}...",
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0xFFE91E63) else colors.surfaceGlass)
                    .clickable {
                        if (isListening) {
                            com.Lia.assistant.voice.DebouncedSpeechInputManager.stopListening()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                try {
                                    com.Lia.assistant.voice.DebouncedSpeechInputManager.startListening(context) { finalCmd ->
                                        sendMessage(finalCmd)
                                    }
                                } catch (e: Throwable) {
                                    Toast.makeText(context, "Voice input unavailable", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Dictation with 3s silence debounce",
                    tint = if (isListening) Color.White else colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable { sendMessage(inputText) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = colors.onAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
