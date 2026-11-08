package com.Lia.assistant.voice

import org.json.JSONArray

object LiaToolCatalog {
    fun declarations(): JSONArray {
        val array = JSONArray()

        array.put(
            GeminiToolUtils.functionDecl(
                name = "open_app",
                description = "Opens an installed application on the phone by its name.",
                params = mapOf("app_name" to "The name of the app to launch (e.g. YouTube, WhatsApp, Camera)"),
                required = listOf("app_name")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "call_contact",
                description = "Places a voice call to a phone contact by name or direct number.",
                params = mapOf("contact_name" to "The contact name or phone number to call"),
                required = listOf("contact_name")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "message_contact",
                description = "Prepares a message to a contact in SMS or WhatsApp. The composer opens for user review.",
                params = mapOf(
                    "contact_name" to "Name or phone number of the recipient",
                    "message" to "Text body of the message",
                    "app" to "Optional target app: 'whatsapp' or 'sms' (defaults to 'sms')"
                ),
                required = listOf("contact_name", "message")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "build_website",
                description = "Launches the Website Forge to design, code, and animate a complete website based on a description.",
                params = mapOf("prompt" to "Detailed description of the website to build (e.g., 'Modern cafe landing page with menu and 3D coffee cup')"),
                required = listOf("prompt")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "go_home",
                description = "Navigates directly to the Android Home screen. Use when user says 'home jao', 'home per ja', 'home screen', 'go to home', or 'exit'.",
                params = emptyMap(),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "read_screen",
                description = "Reads all visible UI elements, buttons, text, and descriptions currently on the phone screen.",
                params = emptyMap(),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "tap_text",
                description = "Taps or clicks a button or element on the phone screen matching the specified text or description.",
                params = mapOf("text" to "Text or label of the button/element to tap"),
                required = listOf("text")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "type_text",
                description = "Types text into the active input or search field on the screen.",
                params = mapOf("text" to "Text to type into the input field"),
                required = listOf("text")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "scroll_screen",
                description = "Scrolls the current screen forward (down) or backward (up).",
                params = mapOf("direction" to "Scroll direction: 'forward' (down) or 'backward' (up)"),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "go_back",
                description = "Navigates back one screen on the phone (performs Back button action).",
                params = emptyMap(),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "recent_apps",
                description = "Opens the Android recent apps overview / app switcher.",
                params = emptyMap(),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "open_settings",
                description = "Opens device settings or specific panels: 'wifi', 'bluetooth', 'sound'/'volume', 'display', 'accessibility'.",
                params = mapOf("setting" to "Optional target setting panel (e.g. 'wifi', 'bluetooth', 'sound', 'display', 'accessibility')"),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "set_volume",
                description = "Adjusts device media volume: 'up', 'down', 'mute', or 'unmute'. Use when user asks to increase or decrease sound/volume.",
                params = mapOf("direction" to "Volume adjustment direction: 'up', 'down', 'mute', or 'unmute'"),
                required = listOf("direction")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "open_camera",
                description = "Opens the device camera to take photos or videos.",
                params = emptyMap(),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "search_web",
                description = "Searches the web via browser for a given search query.",
                params = mapOf("query" to "Search query to look up on the web"),
                required = listOf("query")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "media_control",
                description = "Controls media playback (YouTube/Spotify/music): 'play', 'pause', 'next', 'previous'.",
                params = mapOf("command" to "Media playback command: 'play', 'pause', 'next', or 'previous'"),
                required = listOf("command")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "toggle_torch",
                description = "Turns the phone flashlight (torch) on or off, or toggles it.",
                params = mapOf("state" to "Flashlight state: 'on', 'off', or 'toggle'"),
                required = listOf("state")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "get_battery_status",
                description = "Checks the phone's current battery percentage and charging status.",
                params = emptyMap(),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "set_timer_alarm",
                description = "Sets a timer or an alarm clock on the device.",
                params = mapOf(
                    "type" to "'timer' or 'alarm'",
                    "seconds" to "For timer: duration in seconds",
                    "hour" to "For alarm: hour (0-23)",
                    "minutes" to "For alarm: minute (0-59)",
                    "message" to "Label or note for the alarm/timer"
                ),
                required = listOf("type")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "save_memory",
                description = "Saves a user preference, note, or fact to remember across sessions (e.g. 'yaad rakhna...').",
                params = mapOf("fact" to "The detail, preference, or note to remember"),
                required = listOf("fact")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "get_memories",
                description = "Retrieves all saved memories, user notes, and stored preferences (e.g. 'kya yaad hai?').",
                params = emptyMap(),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "forget_memory",
                description = "Deletes a saved memory or clears all memories if query is 'all' (e.g. 'bhool jao').",
                params = mapOf("query" to "Keyword of the memory to remove, or 'all' to clear everything"),
                required = listOf("query")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "explain_screen",
                description = "Inspects the current screen content and explains it, or gives gaming tactics/strategy if a game is playing.",
                params = mapOf("context" to "Optional context: 'general', 'game', 'form', or 'article'"),
                required = emptyList()
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "autonomous_agent",
                description = "Executes multi-step complex phone tasks end-to-end (e.g. 'WhatsApp kholo aur Keshav ko Hi bhej do').",
                params = mapOf("command" to "The multi-step phone task instruction to execute"),
                required = listOf("command")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "web_research",
                description = "Performs real-time web research for breaking news, current events, Free Fire updates, or queries requiring fresh facts.",
                params = mapOf(
                    "query" to "Search topic or research question",
                    "category" to "Category: 'breaking_news', 'gaming_free_fire', or 'general_web'"
                ),
                required = listOf("query")
            )
        )

        array.put(
            GeminiToolUtils.functionDecl(
                name = "game_coach",
                description = "Starts, pauses, resumes, or stops the Free Fire persistent live coach that monitors gameplay screen and issues tactical alerts.",
                params = mapOf("action" to "Action: 'start', 'stop', 'pause', 'resume', or 'status'"),
                required = listOf("action")
            )
        )

        return array
    }
}
