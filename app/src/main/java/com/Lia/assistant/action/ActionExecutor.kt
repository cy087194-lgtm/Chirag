package com.Lia.assistant.action

import android.content.Context
import org.json.JSONObject

object ActionExecutor {
    private var isTorchOn = false

    suspend fun execute(context: Context, toolName: String, args: JSONObject): JSONObject {
        val result = JSONObject()

        when (toolName) {
            "open_app" -> {
                val appName = args.optString("app_name", "").trim()
                if (appName.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "App name cannot be empty")
                    return result
                }

                val lowerApp = appName.lowercase()
                if (lowerApp in listOf("home", "home screen", "homepage", "home page", "homescreen")) {
                    return execute(context, "go_home", JSONObject())
                }
                if (lowerApp in listOf("settings", "setting", "phone settings", "device settings")) {
                    return execute(context, "open_settings", JSONObject())
                }
                if (lowerApp in listOf("camera", "cam", "photo", "selfie")) {
                    return execute(context, "open_camera", JSONObject())
                }

                val apps = InstalledAppLabelCache.get(context)
                val packageName = AppLauncher.findApp(appName, apps)

                if (packageName != null) {
                    val launched = AppLauncher.launch(context, packageName)
                    if (launched) {
                        result.put("status", "success")
                        result.put("action", "app_opened")
                        result.put("package", packageName)
                    } else {
                        result.put("status", "error")
                        result.put("action", "launch_failed")
                    }
                } else {
                    result.put("status", "error")
                    result.put("action", "app_not_found")
                    result.put("query", appName)
                }
            }

            "call_contact" -> {
                val query = args.optString("contact_name", "").trim()
                if (query.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "Contact query is empty")
                    return result
                }

                when (val lookup = ContactResolver.lookup(context, query)) {
                    is ContactLookupResult.Found -> {
                        val callStatus = CommIntents.placeCall(context, lookup.phoneNumber)
                        result.put("status", "success")
                        result.put("action", callStatus)
                        result.put("contact", lookup.displayName)
                        result.put("number", lookup.phoneNumber)
                    }
                    is ContactLookupResult.NoPermission -> {
                        result.put("status", "error")
                        result.put("action", "no_contacts_permission")
                        result.put("message", "Please grant Contacts permission in Settings.")
                    }
                    is ContactLookupResult.NotFound -> {
                        result.put("status", "error")
                        result.put("action", "contact_not_found")
                        result.put("query", query)
                    }
                }
            }

            "message_contact" -> {
                val contactName = args.optString("contact_name", "").trim()
                val messageText = args.optString("message", "").trim()
                val targetApp = args.optString("app", "sms").lowercase().trim()

                if (contactName.isEmpty() || messageText.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "Recipient and message body are required")
                    return result
                }

                val lookup = ContactResolver.lookup(context, contactName)
                val phoneNumber = when (lookup) {
                    is ContactLookupResult.Found -> lookup.phoneNumber
                    else -> contactName
                }

                val status = if (targetApp == "whatsapp") {
                    CommIntents.openWhatsAppComposer(context, phoneNumber, messageText)
                } else {
                    CommIntents.openSmsComposer(context, phoneNumber, messageText)
                }

                result.put("status", "success")
                result.put("action", status)
                result.put("recipient", phoneNumber)
            }

            "go_home" -> {
                try {
                    val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        addCategory(android.content.Intent.CATEGORY_HOME)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                    result.put("status", "success")
                    result.put("action", "go_home")
                    result.put("message", "Navigated to home screen")
                } catch (e: Exception) {
                    result.put("status", "error")
                    result.put("message", e.localizedMessage ?: "Failed to go home")
                }
            }

            "read_screen" -> {
                val service = com.Lia.assistant.accessibility.LiaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "error")
                    result.put("action", "accessibility_not_enabled")
                    result.put("message", "LiaAI Accessibility Service is not enabled. Please enable LiaAI in Settings -> Accessibility -> Downloaded Apps.")
                } else {
                    val elements = service.readScreen()
                    val array = org.json.JSONArray()
                    for (el in elements) {
                        val obj = JSONObject()
                        obj.put("text", el.text)
                        obj.put("desc", el.contentDescription)
                        obj.put("clickable", el.isClickable)
                        obj.put("editable", el.isEditable)
                        obj.put("viewId", el.viewId)
                        array.put(obj)
                    }
                    result.put("status", "success")
                    result.put("action", "screen_read")
                    result.put("count", elements.size)
                    result.put("elements", array)
                }
            }

            "tap_text" -> {
                val service = com.Lia.assistant.accessibility.LiaAccessibilityService.instance
                val text = args.optString("text", "").trim()
                if (service == null) {
                    result.put("status", "error")
                    result.put("action", "accessibility_not_enabled")
                    result.put("message", "LiaAI Accessibility Service is not enabled.")
                } else if (text.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "Target text is empty")
                } else {
                    val tapResult = service.tapText(text)
                    result.put("status", if (tapResult.startsWith("tapped")) "success" else "error")
                    result.put("action", tapResult)
                    result.put("target", text)
                }
            }

            "type_text" -> {
                val service = com.Lia.assistant.accessibility.LiaAccessibilityService.instance
                val text = args.optString("text", "")
                if (service == null) {
                    result.put("status", "error")
                    result.put("action", "accessibility_not_enabled")
                } else {
                    val typeResult = service.typeText(text)
                    result.put("status", if (typeResult == "typed") "success" else "error")
                    result.put("action", typeResult)
                }
            }

            "scroll_screen" -> {
                val service = com.Lia.assistant.accessibility.LiaAccessibilityService.instance
                val direction = args.optString("direction", "forward")
                if (service == null) {
                    result.put("status", "error")
                    result.put("action", "accessibility_not_enabled")
                } else {
                    val scrollResult = service.scroll(direction)
                    result.put("status", if (scrollResult == "scrolled") "success" else "error")
                    result.put("action", scrollResult)
                }
            }

            "go_back" -> {
                val service = com.Lia.assistant.accessibility.LiaAccessibilityService.instance
                if (service != null && service.performBack()) {
                    result.put("status", "success")
                    result.put("action", "go_back")
                } else {
                    result.put("status", "error")
                    result.put("message", "Could not perform back action")
                }
            }

            "recent_apps" -> {
                val service = com.Lia.assistant.accessibility.LiaAccessibilityService.instance
                if (service != null && service.performRecents()) {
                    result.put("status", "success")
                    result.put("action", "recent_apps")
                } else {
                    result.put("status", "error")
                    result.put("message", "Could not open recent apps")
                }
            }

            "open_settings" -> {
                try {
                    val target = args.optString("setting", "").lowercase().trim()
                    val action = when {
                        target.contains("wifi") || target.contains("wi-fi") -> android.provider.Settings.ACTION_WIFI_SETTINGS
                        target.contains("bluetooth") -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
                        target.contains("sound") || target.contains("volume") || target.contains("aawaz") -> android.provider.Settings.ACTION_SOUND_SETTINGS
                        target.contains("display") || target.contains("brightness") -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
                        target.contains("accessibility") -> android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                        target.contains("app") -> android.provider.Settings.ACTION_APPLICATION_SETTINGS
                        else -> android.provider.Settings.ACTION_SETTINGS
                    }
                    val intent = android.content.Intent(action).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    result.put("status", "success")
                    result.put("action", "settings_opened")
                    result.put("setting", target.ifBlank { "main" })
                } catch (e: Exception) {
                    result.put("status", "error")
                    result.put("message", e.localizedMessage ?: "Failed to open settings")
                }
            }

            "set_volume" -> {
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    val direction = args.optString("direction", "up").lowercase().trim()
                    if (audioManager != null) {
                        when {
                            direction.contains("up") || direction.contains("badhao") || direction.contains("raise") || direction.contains("increase") -> {
                                audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
                            }
                            direction.contains("down") || direction.contains("kam") || direction.contains("lower") || direction.contains("decrease") -> {
                                audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
                            }
                            direction.contains("mute") || direction.contains("band") -> {
                                audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_MUTE, android.media.AudioManager.FLAG_SHOW_UI)
                            }
                            else -> {
                                audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_SAME, android.media.AudioManager.FLAG_SHOW_UI)
                            }
                        }
                        result.put("status", "success")
                        result.put("action", "volume_adjusted")
                        result.put("direction", direction)
                    } else {
                        result.put("status", "error")
                        result.put("message", "AudioManager not available")
                    }
                } catch (e: Exception) {
                    result.put("status", "error")
                    result.put("message", e.localizedMessage ?: "Failed to adjust volume")
                }
            }

            "open_camera" -> {
                try {
                    val intent = android.content.Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    result.put("status", "success")
                    result.put("action", "camera_opened")
                } catch (e: Exception) {
                    result.put("status", "error")
                    result.put("message", e.localizedMessage ?: "Failed to open camera")
                }
            }

            "search_web" -> {
                try {
                    val q = args.optString("query", "").trim()
                    val uri = android.net.Uri.parse("https://www.google.com/search?q=" + android.net.Uri.encode(q))
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    result.put("status", "success")
                    result.put("action", "search_opened")
                    result.put("query", q)
                } catch (e: Exception) {
                    result.put("status", "error")
                    result.put("message", e.localizedMessage ?: "Failed to search web")
                }
            }

            "media_control" -> {
                try {
                    val cmd = args.optString("command", "play_pause").lowercase().trim()
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    val keyEvent = when {
                        cmd.contains("pause") || cmd.contains("stop") || cmd.contains("roko") -> android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
                        cmd.contains("play") || cmd.contains("resume") || cmd.contains("chalao") -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                        cmd.contains("next") || cmd.contains("aage") || cmd.contains("skip") -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                        cmd.contains("prev") || cmd.contains("peechhe") -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                        else -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    }
                    audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyEvent))
                    audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyEvent))
                    result.put("status", "success")
                    result.put("action", "media_command_sent")
                    result.put("command", cmd)
                } catch (e: Exception) {
                    result.put("status", "error")
                    result.put("message", e.localizedMessage ?: "Failed to dispatch media command")
                }
            }

            "toggle_torch" -> {
                try {
                    val state = args.optString("state", "toggle").lowercase().trim()
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                    val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                    if (cameraManager != null && cameraId != null) {
                        val turnOn = when {
                            state.contains("on") || state.contains("chalu") || state.contains("enable") -> true
                            state.contains("off") || state.contains("band") || state.contains("disable") -> false
                            else -> !isTorchOn
                        }
                        cameraManager.setTorchMode(cameraId, turnOn)
                        isTorchOn = turnOn
                        result.put("status", "success")
                        result.put("action", "torch_toggled")
                        result.put("is_on", turnOn)
                    } else {
                        result.put("status", "error")
                        result.put("message", "Flashlight hardware not available")
                    }
                } catch (e: Exception) {
                    result.put("status", "error")
                    result.put("message", e.localizedMessage ?: "Failed to toggle flashlight")
                }
            }

            "get_battery_status" -> {
                try {
                    val ifilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                    val batteryStatus = context.registerReceiver(null, ifilter)
                    val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else -1
                    val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                    val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                                    status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    result.put("status", "success")
                    result.put("action", "battery_status_retrieved")
                    result.put("percentage", batteryPct)
                    result.put("is_charging", isCharging)
                    result.put("summary", "Battery is at $batteryPct%${if (isCharging) " (charging)" else ""}")
                } catch (e: Exception) {
                    result.put("status", "error")
                    result.put("message", e.localizedMessage ?: "Failed to get battery info")
                }
            }

            "set_timer_alarm" -> {
                try {
                    val type = args.optString("type", "alarm").lowercase().trim()
                    val message = args.optString("message", "Lia AI Alarm")
                    if (type == "timer") {
                        val seconds = args.optInt("seconds", 60)
                        val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
                            putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds)
                            putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, message)
                            putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        result.put("status", "success")
                        result.put("action", "timer_set")
                        result.put("seconds", seconds)
                    } else {
                        val hour = args.optInt("hour", 7)
                        val minutes = args.optInt("minutes", 0)
                        val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                            putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minutes)
                            putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, message)
                            putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        result.put("status", "success")
                        result.put("action", "alarm_set")
                        result.put("time", String.format("%02d:%02d", hour, minutes))
                    }
                } catch (e: Exception) {
                    result.put("status", "error")
                    result.put("message", e.localizedMessage ?: "Failed to set timer or alarm")
                }
            }

            "save_memory" -> {
                val fact = args.optString("fact", "").trim()
                if (fact.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "Fact to remember cannot be empty")
                } else {
                    val item = com.Lia.assistant.data.UserMemoryStore.get(context).saveMemory(fact)
                    result.put("status", "success")
                    result.put("action", "memory_saved")
                    result.put("id", item.id)
                    result.put("content", item.content)
                    result.put("message", "Maine yaad rakh liya: \"${item.content}\"")
                }
            }

            "get_memories" -> {
                val store = com.Lia.assistant.data.UserMemoryStore.get(context)
                val memories = store.getAllMemories()
                result.put("status", "success")
                result.put("action", "memories_retrieved")
                result.put("count", memories.size)
                val array = org.json.JSONArray()
                memories.forEach { array.put(it.content) }
                result.put("memories", array)
                result.put("summary", store.getMemoriesSummary())
                result.put("message", if (memories.isEmpty()) "Mujhe abhi koi memory yaad nahi hai." else "Mujhe yeh sab yaad hai:\n" + store.getMemoriesSummary())
            }

            "forget_memory" -> {
                val query = args.optString("query", "").trim()
                val store = com.Lia.assistant.data.UserMemoryStore.get(context)
                if (query.equals("all", ignoreCase = true) || query.equals("sab", ignoreCase = true)) {
                    store.clearAll()
                    result.put("status", "success")
                    result.put("action", "all_memories_cleared")
                    result.put("message", "Saari saved memories delete kar di gayi hain.")
                } else if (query.isNotEmpty()) {
                    val removed = store.forgetMatching(query)
                    result.put("status", "success")
                    result.put("action", "memory_forgotten")
                    result.put("removed_count", removed)
                    result.put("message", if (removed > 0) "\"$query\" se judi memories bhula di gayi hain." else "\"$query\" se related koi memory nahi mili.")
                } else {
                    result.put("status", "error")
                    result.put("message", "Specify what to forget, or 'all'.")
                }
            }

            "explain_screen" -> {
                val service = com.Lia.assistant.accessibility.LiaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "error")
                    result.put("action", "service_not_connected")
                    result.put("message", "Screen padhne ke liye LiaAI ki Accessibility permission enable honi chahiye.")
                } else {
                    val nodes = service.readScreen()
                    if (nodes.isEmpty()) {
                        result.put("status", "success")
                        result.put("action", "empty_screen")
                        result.put("summary", "Screen par koi readable text ya elements nahi mile.")
                    } else {
                        val texts = nodes.map { it.text }.filter { it.isNotBlank() }
                        val fullText = texts.joinToString(" | ")
                        val requestedContext = args.optString("context", "general").lowercase()
                        val isGaming = requestedContext == "game" ||
                                fullText.contains("level", ignoreCase = true) ||
                                fullText.contains("score", ignoreCase = true) ||
                                fullText.contains("kill", ignoreCase = true) ||
                                fullText.contains("zone", ignoreCase = true) ||
                                fullText.contains("hp", ignoreCase = true)

                        result.put("status", "success")
                        result.put("action", "screen_analyzed")
                        result.put("elements_count", nodes.size)
                        result.put("extracted_text", fullText.take(1000))

                        if (isGaming) {
                            val advice = "Gaming Mode Strategy: 1. Mini-map par safe zone aur blue zone timer notice karein. 2. Cover ke paas rahein aur reload karte waqt crouch karein. 3. Sound clues aur enemy footsteps par focus karein. 4. Health/Shield low hone par pehle heal karein phir combat engage karein."
                            result.put("is_gaming", true)
                            result.put("strategy", advice)
                            result.put("message", "Screen analysis: Game active lag raha hai. Strategy: $advice")
                        } else {
                            result.put("is_gaming", false)
                            result.put("message", "Screen content padh liya gaya hai: ${fullText.take(300)}...")
                        }
                    }
                }
            }

            "autonomous_agent" -> {
                val command = args.optString("command", "").trim()
                if (command.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "Task command cannot be empty")
                } else {
                    val taskResult = com.Lia.assistant.agent.AutonomousAgentEngine.executeGeneralTask(context, command) { _ -> }
                    result.put("status", if (taskResult.isSuccess) "success" else "error")
                    result.put("action", "autonomous_task_executed")
                    result.put("goal", taskResult.goal)
                    result.put("summary", taskResult.summary)
                    val stepsArray = org.json.JSONArray()
                    taskResult.completedSteps.forEach { step ->
                        val stepObj = org.json.JSONObject()
                            .put("step", step.stepIndex)
                            .put("description", step.description)
                            .put("completed", step.isCompleted)
                        stepsArray.put(stepObj)
                    }
                    result.put("steps", stepsArray)
                    result.put("message", taskResult.summary)
                }
            }

            "web_research" -> {
                val query = args.optString("query", "").trim()
                val category = args.optString("category", "general_web").trim()
                if (query.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "Search query cannot be empty")
                } else {
                    val report = com.Lia.assistant.research.WebResearchEngine.research(query, category)
                    result.put("status", "success")
                    result.put("action", "web_research_completed")
                    result.put("summary", report.summary)
                    val pointsArray = org.json.JSONArray()
                    report.keyPoints.forEach { pointsArray.put(it) }
                    result.put("key_points", pointsArray)
                    val sourcesArray = org.json.JSONArray()
                    report.sources.forEach { sourcesArray.put(it) }
                    result.put("sources", sourcesArray)

                    val replyBuilder = StringBuilder()
                    replyBuilder.append(report.summary)
                    if (report.keyPoints.isNotEmpty()) {
                        replyBuilder.append("\n\nKey Highlights:\n")
                        report.keyPoints.forEach { p -> replyBuilder.append("• $p\n") }
                    }
                    if (report.sources.isNotEmpty()) {
                        replyBuilder.append("\nSources: ${report.sources.joinToString(", ")}")
                    }
                    result.put("message", replyBuilder.toString().trim())
                }
            }

            "game_coach" -> {
                val action = args.optString("action", "status").lowercase().trim()
                val coach = com.Lia.assistant.agent.gaming.FreeFireGameCoach
                val coachScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                val msg = when (action) {
                    "start" -> coach.startCoaching(context, coachScope)
                    "stop" -> coach.stopCoaching()
                    "pause" -> coach.pauseCoaching()
                    "resume" -> coach.resumeCoaching()
                    else -> "Game Coach State: ${coach.coachState.value}"
                }
                result.put("status", "success")
                result.put("action", "game_coach_$action")
                result.put("current_state", coach.coachState.value.name)
                result.put("message", msg)
            }

            else -> {
                result.put("error", "Unknown tool: $toolName")
            }
        }

        val actionStatus = if (result.optString("status") == "success") {
            com.Lia.assistant.agent.core.VerificationStatus.SUCCESS_VERIFIED
        } else if (result.has("error")) {
            com.Lia.assistant.agent.core.VerificationStatus.FAILED
        } else {
            com.Lia.assistant.agent.core.VerificationStatus.SUCCESS_UNVERIFIED
        }
        val evidence = result.optString("message", result.optString("summary", result.optString("action", "Executed")))
        com.Lia.assistant.agent.core.ActionHistoryManager.recordAction(
            taskId = "ACT-${System.currentTimeMillis() % 10000}",
            toolName = toolName,
            command = args.toString(),
            status = actionStatus,
            evidence = evidence
        )

        return result
    }
}
