package com.Lia.assistant.registry

import android.content.Context

enum class FeatureStatus {
    WORKING,
    PARTIAL,
    BROKEN,
    UI_ONLY,
    BLOCKED_BY_ANDROID,
    NEEDS_API_KEY,
    NOT_VERIFIED
}

data class FeatureEntry(
    val id: String,
    val name: String,
    val part: String, // "PART Q2", "PART R", "PART S", "PART T1", etc.
    val category: String, // "Automation", "AI Brain", "Gaming", "Study", "Photo", "Finance", "Daily", "System"
    val shortDescription: String,
    val activation: String,
    val stopping: String,
    val status: FeatureStatus,
    val statusDetails: String = ""
)

object FeatureRegistry {

    private val allFeatures = listOf(
        // === PART Q2: FULL-SPEED ONE-SHOT EXECUTION ===
        FeatureEntry(
            id = "q2_one_shot_planner",
            name = "One-Shot Command Planner",
            part = "PART Q2",
            category = "Automation",
            shortDescription = "Single Gemini call turns multi-intent spoken command into complete ordered JSON plan",
            activation = "Voice/Text: 'YouTube par Pro Shivay gamer search karo, pehli video par click karke like aur subscribe karo'",
            stopping = "Runs to completion or stops on fatal node failure",
            status = FeatureStatus.WORKING,
            statusDetails = "Deterministic local fallback included for instant offline plan decomposition"
        ),
        FeatureEntry(
            id = "q2_back_to_back_exec",
            name = "Back-to-Back Local Execution",
            part = "PART Q2",
            category = "Automation",
            shortDescription = "Executes all steps on-device without calling Gemini or speaking between steps",
            activation = "Automatic when executing any composite command plan",
            stopping = "Completes last step automatically",
            status = FeatureStatus.WORKING,
            statusDetails = "Never prompts 'ab kya karun?' between intermediate steps"
        ),
        FeatureEntry(
            id = "q2_dynamic_node_polling",
            name = "Fast Dynamic Node Polling",
            part = "PART Q2",
            category = "Automation",
            shortDescription = "Polls accessibility node tree every 80ms (max 3s) and proceeds immediately when target appears",
            activation = "Built into OneShotExecutionEngine for every action",
            stopping = "Advances immediately or times out at 3s",
            status = FeatureStatus.WORKING,
            statusDetails = "Zero fixed sleep delays; operates at maximum UI response speed"
        ),
        FeatureEntry(
            id = "q2_failure_recovery",
            name = "Gemini Step Recovery",
            part = "PART Q2",
            category = "Automation",
            shortDescription = "Calls Gemini only when a step fails to re-plan by ID, text, desc, or coordinates",
            activation = "Triggered automatically when node search fails after 3s",
            stopping = "Recovers and continues remaining plan",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "q2_popup_ad_autoclose",
            name = "Popup, Cookie & Ad Auto-Dismiss",
            part = "PART Q2",
            category = "Automation",
            shortDescription = "Auto-detects and clicks Skip, Skip Ad, Close, X, Allow, Got it, OK without breaking tasks",
            activation = "Automated watchdog before and during any phone control step",
            stopping = "Continuous background check",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "q2_skip_already_done",
            name = "Skip Already Done Actions",
            part = "PART Q2",
            category = "Automation",
            shortDescription = "Skips 'like' or 'subscribe' automatically if button is already marked Liked/Subscribed",
            activation = "Evaluated before executing like or subscribe steps",
            stopping = "Advances immediately to next step",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "q2_speech_silence_debounce",
            name = "3-Second Speech Silence Debounce",
            part = "PART Q2",
            category = "Voice",
            shortDescription = "Buffers user speech across natural pauses and executes only after 3 seconds of silence",
            activation = "Mic dictation in ChatScreen or VoiceScreen",
            stopping = "Sends combined command when 3000ms silence detected",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "q2_single_final_tts",
            name = "Single Final Spoken Report",
            part = "PART Q2",
            category = "Voice",
            shortDescription = "Speaks once at the very end ('ho gaya' or exact step that failed)",
            activation = "Spoken completion signal of OneShotExecutionEngine",
            stopping = "TTS completes speaking",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "q2_orb_progress_overlay",
            name = "Overlay Orb Live Progress Badge",
            part = "PART Q2",
            category = "UI",
            shortDescription = "Displays real-time step counter like (3/6) directly over the floating Nova orb",
            activation = "Floating overlay active during one-shot tasks",
            stopping = "Disappears when task completes",
            status = FeatureStatus.WORKING
        ),

        // === PART R: MULTI-MODEL BRAIN ===
        FeatureEntry(
            id = "r_gemini_provider",
            name = "Google Gemini Provider",
            part = "PART R",
            category = "AI Brain",
            shortDescription = "Gemini 2.5 Flash for ultra-fast reasoning, multimodal vision, and one-shot planning",
            activation = "Default primary provider in ModelRouter",
            stopping = "Per-call execution",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "r_openai_provider",
            name = "OpenAI GPT-4o Provider",
            part = "PART R",
            category = "AI Brain",
            shortDescription = "GPT-4o chat completions and DALL-E 3 image generation",
            activation = "Set OpenAI Key in Settings or automatic fallback",
            stopping = "Per-call execution",
            status = FeatureStatus.NEEDS_API_KEY,
            statusDetails = "Add OpenAI API key in Settings -> AI & Voice Intelligence"
        ),
        FeatureEntry(
            id = "r_claude_provider",
            name = "Anthropic Claude 3.5 Provider",
            part = "PART R",
            category = "AI Brain",
            shortDescription = "Claude 3.5 Sonnet for advanced coding, complex logic, and deep analysis",
            activation = "Set Claude Key in Settings or automatic fallback",
            stopping = "Per-call execution",
            status = FeatureStatus.NEEDS_API_KEY,
            statusDetails = "Add Anthropic API key in Settings -> AI & Voice Intelligence"
        ),
        FeatureEntry(
            id = "r_model_router_fallback",
            name = "Model Router & Fallback Ladder",
            part = "PART R",
            category = "AI Brain",
            shortDescription = "Routes tasks by specialty with single-retry and automatic ladder failover across providers",
            activation = "Automatic on all chat, coding, and planning queries",
            stopping = "Falls back to local parser if all endpoints fail",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "r_token_cost_limiter",
            name = "Token & Daily Cost Limiter",
            part = "PART R",
            category = "AI Brain",
            shortDescription = "Tracks daily tokens and blocks calls when daily budget limit is reached",
            activation = "Configured in Settings -> Daily Usage & Cost Limit",
            stopping = "Resets at midnight UTC",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "r_deep_web_research",
            name = "Deep Web Research Engine",
            part = "PART R",
            category = "AI Brain",
            shortDescription = "Decomposes complex questions into 3-6 searches with source comparison and uncertainty notes",
            activation = "Voice/Text: 'Research karo...', 'Deep search...', 'Kya latest updates hain...'",
            stopping = "Generates comprehensive research synthesis",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "r_model_diagnostics",
            name = "Multi-Model Self-Test Bench",
            part = "PART R",
            category = "AI Brain",
            shortDescription = "Interactive dashboard testing pings, failover ladder, search, and one-shot planner",
            activation = "Tap 'Run Test' in JARVIS Headquarters",
            stopping = "Generates latency and status report",
            status = FeatureStatus.WORKING
        ),

        // === PART S: SCREEN SHARE, TOUCH & SYSTEM CONTROL ===
        FeatureEntry(
            id = "s0_always_watch",
            name = "Always-Watch Mode",
            part = "PART S",
            category = "Screen Vision",
            shortDescription = "Continuous screen watcher using node hash diffing until 'bas karo' command",
            activation = "Voice/Text: 'screen dekhte raho', 'always watch on', 'nazar rakho'",
            stopping = "Voice/Text: 'bas karo', 'stop watching', 'chup'",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s0_flag_secure_guard",
            name = "Banking FLAG_SECURE Protection",
            part = "PART S",
            category = "Screen Vision",
            shortDescription = "Honestly discloses and pauses screen capture on banking/UPI apps (GPay, PhonePe, Paytm, SBI)",
            activation = "Automatic when protected finance apps enter foreground",
            stopping = "Resumes when leaving protected app",
            status = FeatureStatus.WORKING,
            statusDetails = "Complies strictly with Android security boundaries"
        ),
        FeatureEntry(
            id = "s1_scam_shield",
            name = "Scam & Phishing Screen Detector",
            part = "PART S",
            category = "Screen Vision",
            shortDescription = "Scans screen for fake bank domains, urgent lottery claims, and fraudulent OTP demands",
            activation = "Voice/Text: 'kya ye scam hai?', 'screen scan karo scam ke liye'",
            stopping = "Immediate alert report",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s1_screen_qa",
            name = "Screen Q&A & Bill Calculator",
            part = "PART S",
            category = "Screen Vision",
            shortDescription = "Answers questions about current screen content: bill totals, order items, and summaries",
            activation = "Voice/Text: 'is screen par bill kitna hai?', 'is screen par kya likha hai?'",
            stopping = "Spoken & text response",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s1_screen_translate",
            name = "Live Screen Translation",
            part = "PART S",
            category = "Screen Vision",
            shortDescription = "Translates foreign text on the screen to Hindi/English",
            activation = "Voice/Text: 'is screen ko Hindi mein translate karo'",
            stopping = "Displays translated text block",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s1_ocr_copy",
            name = "Screen OCR Clipboard Copy",
            part = "PART S",
            category = "Screen Vision",
            shortDescription = "Extracts all readable text from the screen and copies it to the device clipboard",
            activation = "Voice/Text: 'screen ka text copy kar lo', 'copy screen text'",
            stopping = "Copies to clipboard and confirms",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s2_macro_engine",
            name = "Macro Recorder & Replay",
            part = "PART S",
            category = "Automation",
            shortDescription = "Records sequence of tap/swipe gestures and replays them on command",
            activation = "Voice/Text: 'macro record shuru karo' / 'macro chalao'",
            stopping = "Voice/Text: 'macro record band karo'",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s2_auto_clicker",
            name = "Accessibility Auto-Clicker",
            part = "PART S",
            category = "Automation",
            shortDescription = "Repeatedly clicks a button or text on screen at a configured interval",
            activation = "Voice/Text: 'is button ko har 2 second mein click karo', 'auto clicker on'",
            stopping = "Voice/Text: 'auto clicker band karo', 'stop clicker'",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s2_text_expander",
            name = "Text Expander Dictionary",
            part = "PART S",
            category = "Automation",
            shortDescription = "Expands short abbreviations like #email or #addr into full pre-configured text",
            activation = "Type abbreviation or voice: 'expand text #email'",
            stopping = "Instant replacement",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s3_focus_mode",
            name = "Padhai / Focus Mode",
            part = "PART S",
            category = "System",
            shortDescription = "Silences distractions, enables DND, and alerts if Instagram/YouTube are opened",
            activation = "Voice/Text: 'padhai mode on karo', 'focus mode chalu karo'",
            stopping = "Voice/Text: 'padhai mode band karo', 'focus off'",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s3_storage_cleaner",
            name = "Storage & Junk Cleaner",
            part = "PART S",
            category = "System",
            shortDescription = "Scans cache directories and app temporary files to recover device storage",
            activation = "Voice/Text: 'storage saaf karo', 'clean cache', 'junk delete karo'",
            stopping = "Generates cleanup summary",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s3_smart_alarm",
            name = "Smart Voice Alarm",
            part = "PART S",
            category = "Daily",
            shortDescription = "Sets Android system alarms using native AlarmManager intents with label support",
            activation = "Voice/Text: 'subah 6 baje ka alarm laga do', 'set alarm 7am'",
            stopping = "Cancels alarm via Clock app",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "s4_cab_booking_flow",
            name = "Cab Booking Flow Guide",
            part = "PART S",
            category = "Automation",
            shortDescription = "Opens Ola or Uber and guides navigation to ride confirmation",
            activation = "Voice/Text: 'cab book karo airport ke liye', 'Uber bulao'",
            stopping = "Requires manual payment confirmation by user",
            status = FeatureStatus.PARTIAL,
            statusDetails = "Navigation automated; final OTP/Payment gated for user safety"
        ),
        FeatureEntry(
            id = "s4_cart_order_flow",
            name = "Cart Checkout Assistance",
            part = "PART S",
            category = "Automation",
            shortDescription = "Opens shopping app, navigates to cart, and prepares checkout screen",
            activation = "Voice/Text: 'cart check out karo', 'order place karo'",
            stopping = "Stops at final payment screen for user authorization",
            status = FeatureStatus.PARTIAL,
            statusDetails = "Payment authorization strictly reserved for user"
        ),
        FeatureEntry(
            id = "s4_emergency_alert",
            name = "Emergency SOS Alert",
            part = "PART S",
            category = "System",
            shortDescription = "Prepares emergency distress message with coordinates to designated emergency contact",
            activation = "Voice/Text: 'emergency help', 'bachao', 'send SOS'",
            stopping = "Sends SMS or opens dialer",
            status = FeatureStatus.WORKING
        ),

        // === PART T1: PROACTIVE GAME COACH ===
        FeatureEntry(
            id = "t1_game_coach",
            name = "Proactive Game Coach",
            part = "PART T1",
            category = "Gaming",
            shortDescription = "Continuous game vision coach speaking tactical alerts: zone, health, enemies, and weapon tips",
            activation = "Voice: 'game coach on' (stays completely silent in games until explicitly enabled)",
            stopping = "Voice: 'chup', 'bas karo', 'game coach off'",
            status = FeatureStatus.WORKING,
            statusDetails = "Coaching only (NO auto-aim, NO aim-assist, NO memory cheats; 1-3s vision delay disclosed)"
        ),
        FeatureEntry(
            id = "t1_tip_frequency",
            name = "Coach Tip Frequency Setting",
            part = "PART T1",
            category = "Gaming",
            shortDescription = "Customizable alert density: Kam (30s), Normal (15s), Zyada (6s)",
            activation = "Settings or Voice: 'coach tip frequency kam/normal/zyada karo'",
            stopping = "Saves user preference",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t1_post_match_review",
            name = "Post-Match Performance Review",
            part = "PART T1",
            category = "Gaming",
            shortDescription = "Analyzes end-of-game summary screen and provides actionable tactical mistakes and improvements",
            activation = "Voice: 'aaj ka game review karo', or automatic on match finish",
            stopping = "Speaks bulleted review",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t1_game_guide_loadout",
            name = "Game Guide & Loadout Advisor",
            part = "PART T1",
            category = "Gaming",
            shortDescription = "Recommends best weapon attachments, sensitivities, landing spots, and level solutions",
            activation = "Voice: 'Free Fire best sensitivity kya hai?', 'is level mein atka hun'",
            stopping = "Provides tactical guide",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t1_proactive_study_hint",
            name = "Proactive Study & Work Hint",
            part = "PART T1",
            category = "Study",
            shortDescription = "Detects if user has been stuck on the same question or document for 10 minutes and offers a hint",
            activation = "Enabled in Settings -> Proactive Study Watcher",
            stopping = "User says 'nahi chahiye' or solves problem",
            status = FeatureStatus.WORKING
        ),

        // === PART T2: STUDY AND BRAIN ===
        FeatureEntry(
            id = "t2_step_by_step_tutor",
            name = "Photo Homework Tutor",
            part = "PART T2",
            category = "Study",
            shortDescription = "Explains photo questions step by step, generates quizzes, flashcards, and exam revisions",
            activation = "Camera/Chat: 'ye sawal samjhao', 'quiz lo is topic par', 'flashcard banao'",
            stopping = "Interactive explanation",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t2_math_science_solver",
            name = "Camera Math & Science Solver",
            part = "PART T2",
            category = "Study",
            shortDescription = "Solves mathematical equations, chemistry balances, and physics problems with full working",
            activation = "Photo + Voice: 'ye math problem solve karo'",
            stopping = "Step-by-step breakdown",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t2_english_speaking_teacher",
            name = "English Speaking Coach",
            part = "PART T2",
            category = "Study",
            shortDescription = "Real-time conversational English practice with gentle grammar & pronunciation feedback",
            activation = "Voice: 'English sikhna hai', 'English speaking practice karo'",
            stopping = "Voice: 'stop practice'",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t2_interview_practice",
            name = "Interview & Speech Practice",
            part = "PART T2",
            category = "Study",
            shortDescription = "Simulates mock job interviews, assesses clarity, and gives structured feedback",
            activation = "Voice: 'mera mock interview lo Android developer ke liye'",
            stopping = "Voice: 'interview khatam'",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t2_decision_helper",
            name = "Decision Helper (Pros & Cons)",
            part = "PART T2",
            category = "Study",
            shortDescription = "Compares career, gadget, or life options with unbiased pros, cons, and weighted score",
            activation = "Voice: 'decision lene mein madad karo: College A vs College B'",
            stopping = "Comparison matrix output",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t2_creative_writer",
            name = "Creative Writer (Shayari, Stories)",
            part = "PART T2",
            category = "Study",
            shortDescription = "Generates shayari, poetry, YouTube scripts, stories, Instagram captions, and bios",
            activation = "Voice: 'ek shaandar shayari sunao', 'dosti par kavita likho'",
            stopping = "Instant recitation",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t2_private_local_diary",
            name = "Private Nightly Diary & Mood Tracker",
            part = "PART T2",
            category = "Study",
            shortDescription = "Encrypted local-only journal and nightly mood check-in (never uploaded to cloud)",
            activation = "Voice: 'aaj ka diary entry likho...', 'aaj mera mood kaisa tha'",
            stopping = "Saves locally on device",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t2_second_brain",
            name = "Second Brain Notes Search",
            part = "PART T2",
            category = "Study",
            shortDescription = "Searches personal notes, memories, and saved facts ('wo jo pichle mahine likha tha')",
            activation = "Voice: 'maine pichle hafte kya note kiya tha?', 'search my notes'",
            stopping = "Retrieves matching entries",
            status = FeatureStatus.WORKING
        ),

        // === PART T3: PHOTO FEATURES ===
        FeatureEntry(
            id = "t3_object_identifier",
            name = "Camera 'Ye Kya Hai?' Identifier",
            part = "PART T3",
            category = "Photo",
            shortDescription = "Identifies objects, plants, medicine labels, bills, and unknown hardware from photos",
            activation = "Camera + Voice: 'ye kya hai?', 'ye kaun sa plant hai?'",
            stopping = "Provides identification and safe context",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t3_gallery_search",
            name = "Semantic Gallery Search",
            part = "PART T3",
            category = "Photo",
            shortDescription = "Searches device photos by descriptive scene text ('Rahul ki photo', 'beach wali photo')",
            activation = "Voice: 'pichle mahine ki beach wali photo dikhao'",
            stopping = "Opens matched photos in gallery",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t3_doc_scanner_pdf",
            name = "Document Scanner to PDF",
            part = "PART T3",
            category = "Photo",
            shortDescription = "Scans physical documents, crops edges, optimizes contrast, and compiles to PDF",
            activation = "Voice: 'ye document scan karke PDF banao'",
            stopping = "Saves PDF to Downloads",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t3_photo_editor_ai",
            name = "Photo Editor & Background Remover",
            part = "PART T3",
            category = "Photo",
            shortDescription = "Applies aesthetic filters, crops aspect ratios, and isolates foreground subjects",
            activation = "Voice: 'photo ka background hatao', 'filter lagao'",
            stopping = "Exports edited image",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t3_ai_image_gen",
            name = "AI Image Generator",
            part = "PART T3",
            category = "Photo",
            shortDescription = "Generates original images from natural text prompts via OpenAI DALL-E or Gemini",
            activation = "Voice: 'ek futuristic cyberpunk city ki image generate karo'",
            stopping = "Saves generated graphic",
            status = FeatureStatus.WORKING,
            statusDetails = "Uses OpenAI DALL-E 3 or Gemini image capabilities"
        ),

        // === PART T4: MONEY AND WORK ===
        FeatureEntry(
            id = "t4_sms_expense_tracker",
            name = "Bank SMS Expense Tracker",
            part = "PART T4",
            category = "Finance",
            shortDescription = "Parses bank debit/credit transaction SMS completely locally with zero cloud upload",
            activation = "Voice: 'is mahine kitna kharcha hua?', 'kharcha track karo'",
            stopping = "Displays category spending summary",
            status = FeatureStatus.WORKING,
            statusDetails = "100% on-device private regex parsing; no banking credentials stored"
        ),
        FeatureEntry(
            id = "t4_market_price_alerts",
            name = "Share & Crypto Price Ticker",
            part = "PART T4",
            category = "Finance",
            shortDescription = "Checks stock, index (NIFTY/SENSEX), and crypto prices (never places trades)",
            activation = "Voice: 'Bitcoin ka price kya hai?', 'Reliance share ka rate batao'",
            stopping = "Reports current ticker quote",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t4_resume_email_writer",
            name = "Professional Resume & Email Writer",
            part = "PART T4",
            category = "Finance",
            shortDescription = "Drafts formal leave applications, resignation letters, cold emails, and tailored resume summaries",
            activation = "Voice: 'sick leave application likho boss ke liye', 'resume bullet points banao'",
            stopping = "Draft output ready to copy/send",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t4_meeting_call_summarizer",
            name = "Meeting Recorder & To-Do Extractor",
            part = "PART T4",
            category = "Finance",
            shortDescription = "Records authorized meetings and extracts bulleted takeaways and action items",
            activation = "Voice: 'meeting note karo aur action items nikalo'",
            stopping = "Voice: 'meeting khatam'",
            status = FeatureStatus.WORKING,
            statusDetails = "Requires mutual consent; does not bypass Android system call privacy policies"
        ),

        // === PART T5: DAILY LIFE ===
        FeatureEntry(
            id = "t5_fridge_recipe_chef",
            name = "Fridge Photo Chef & Voice Cooking Timer",
            part = "PART T5",
            category = "Daily",
            shortDescription = "Suggests delicious Indian recipes from ingredients in a photo with step-by-step voice guidance",
            activation = "Photo + Voice: 'fridge mein ye saman hai, kya banayein?'",
            stopping = "Step-by-step timer completes",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t5_fitness_water_reminder",
            name = "Fitness Workout & Water Tracker",
            part = "PART T5",
            category = "Daily",
            shortDescription = "Customizes home workout routines, counts sets, and reminds hourly water hydration",
            activation = "Voice: '10 pushups count karo', 'aaj ka diet plan batao', 'paani peene ka reminder'",
            stopping = "Tracks session completion",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t5_travel_train_flight",
            name = "Live Train & Flight Status Finder",
            part = "PART T5",
            category = "Daily",
            shortDescription = "Checks PNR status, running train delays, platform numbers, and airport flight schedules",
            activation = "Voice: 'train 12424 ka live status kya hai?', 'Delhi to Mumbai flight timing'",
            stopping = "Shows live running report",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t5_shopping_price_comparator",
            name = "Shopping Price Comparator",
            part = "PART T5",
            category = "Daily",
            shortDescription = "Compares product prices across Amazon, Flipkart, and Blinkit and summarizes reviews",
            activation = "Voice: 'is phone ka price compare karo Amazon aur Flipkart par'",
            stopping = "Price comparison card output",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t5_news_cricket_briefing",
            name = "Live Cricket Score & Morning Briefing",
            part = "PART T5",
            category = "Daily",
            shortDescription = "Instant live cricket scores, top headlines, weather forecast, and day schedule",
            activation = "Voice: 'LiaAI, morning briefing do', 'live cricket score kya hai?'",
            stopping = "Spoken morning synthesis",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t5_festival_panchang",
            name = "Festival & Panchang Reminders",
            part = "PART T5",
            category = "Daily",
            shortDescription = "Tithi, sunrise, sunset, auspicious muhurats, and upcoming Hindu festival dates",
            activation = "Voice: 'aaj ka panchang kya hai?', 'Diwali kab hai?'",
            stopping = "Shows panchang calendar details",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t5_song_recognizer",
            name = "Song Recognizer ('Ye Kaun Sa Gaana Hai?')",
            part = "PART T5",
            category = "Daily",
            shortDescription = "Listens to background audio or hummed lyrics and identifies track name and artist",
            activation = "Voice: 'ye kaun sa gaana baj raha hai?', 'identify this song'",
            stopping = "Displays song title and offers to play on YouTube/Spotify",
            status = FeatureStatus.WORKING
        ),

        // === PART T6: SMART SYSTEM ===
        FeatureEntry(
            id = "t6_wake_word_handsfree",
            name = "Hands-Free Wake Word Mode",
            part = "PART T6",
            category = "System",
            shortDescription = "Hands-free continuous listener activating on 'Hey Lia' or 'LiaAI'",
            activation = "Voice: 'hands-free mode chalu karo' or Settings toggle",
            stopping = "Voice: 'hands-free band karo'",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t6_six_languages",
            name = "6 Regional Indian Languages",
            part = "PART T6",
            category = "System",
            shortDescription = "Native support for Hindi, English, Bhojpuri, Punjabi, Marathi, and Tamil",
            activation = "Settings -> Spoken Language, or speak naturally in any of the 6 languages",
            stopping = "Switches language preference",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t6_ironman_personalities",
            name = "Iron Man & 4 Personality Modes",
            part = "PART T6",
            category = "System",
            shortDescription = "Customizable tones: Iron Man 'Sir' (JARVIS), Caring Friend, Strict Teacher, and Serious Professional",
            activation = "Settings -> Personality, or Voice: 'Iron Man style mein baat karo'",
            stopping = "Personality switch saved",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t6_plugin_architecture",
            name = "Modular Plugin System",
            part = "PART T6",
            category = "System",
            shortDescription = "Extensible architecture allowing new external actions and tools to register dynamically",
            activation = "Built into ActionExecutor and FeatureRegistry",
            stopping = "Always active",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t6_biometric_pin_gate",
            name = "Biometric & PIN Security Gate",
            part = "PART T6",
            category = "System",
            shortDescription = "Requires fingerprint or device PIN before executing sensitive actions (SMS SOS, clear data)",
            activation = "Automatic when triggering protected operations",
            stopping = "Authenticates or aborts",
            status = FeatureStatus.WORKING,
            statusDetails = "Honestly discloses voice biometric limits; uses Android BiometricPrompt & PIN"
        ),
        FeatureEntry(
            id = "t6_offline_essentials",
            name = "Offline Essential Commands",
            part = "PART T6",
            category = "System",
            shortDescription = "Controls flashlight, volume, home, alarms, and app launches with zero internet connection",
            activation = "Works automatically offline without cloud ping",
            stopping = "Executes locally via Android OS intents",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "t6_weekly_savings_report",
            name = "Weekly Productivity & Time-Saved Report",
            part = "PART T6",
            category = "System",
            shortDescription = "Quantifies tasks automated, queries answered, and estimated minutes saved each week",
            activation = "Voice: 'weekly report dikhao', 'maine kitna time save kiya?'",
            stopping = "Visual metrics breakdown",
            status = FeatureStatus.WORKING
        ),

        // === PART T7: HONESTY & LIMITS ===
        FeatureEntry(
            id = "t7_privacy_transparency",
            name = "Privacy & Android Boundary Disclosures",
            part = "PART T7",
            category = "System",
            shortDescription = "Never violates privacy (no secret wiretapping, no background spying, no banking screen snooping)",
            activation = "Enforced in all services and background loops",
            stopping = "Strict architectural invariant",
            status = FeatureStatus.WORKING
        ),

        // === PART U: FEATURE REGISTRY & KYA KYA JODA HAI ===
        FeatureEntry(
            id = "u_feature_registry",
            name = "Complete Feature Registry",
            part = "PART U",
            category = "System",
            shortDescription = "Central source of truth cataloging every feature across all parts with real-time status",
            activation = "Voice: 'tumhare andar maine kya kya add kiya hai', 'sab features batao', or tap 'Features' in Nav",
            stopping = "Interactive part-by-part voice pacing ('ruko', 'agla')",
            status = FeatureStatus.WORKING
        ),

        // === PART Z: TEACH BY DEMONSTRATION ===
        FeatureEntry(
            id = "z_teach_by_demo",
            name = "Teach by Demonstration Recorder",
            part = "PART Z",
            category = "Automation",
            shortDescription = "Watches user perform any task across any app once, recording taps, typed text, and navigation",
            activation = "Voice: 'seekhna shuru karo', 'ye seekho', 'learn this', 'meri screen dekho'",
            stopping = "Voice: 'seekhna band karo', 'bas itna', or red orb stop button",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "z_direct_route_optimizer",
            name = "Direct Route & Deep Link Optimizer",
            part = "PART Z",
            category = "Automation",
            shortDescription = "Extracts intent variables (contact, message) to generate fastest instant route (wa.me) with UI fallback",
            activation = "Automatic upon compiling demonstration",
            stopping = "Instant routing execution",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "z_routine_execution_engine",
            name = "One-Shot Routine Execution Engine",
            part = "PART Z",
            category = "Automation",
            shortDescription = "Executes fastest direct route or falls back to recorded UI sequence, verifies on screen, and speaks once upon completion",
            activation = "Voice trigger with wake word (e.g. 'Jarvis, Keshav'), time schedule, or event",
            stopping = "Speaks completion report ('Keshav ko hay bhej diya')",
            status = FeatureStatus.WORKING
        ),
        FeatureEntry(
            id = "z_privacy_guard",
            name = "Demonstration Privacy Guard",
            part = "PART Z",
            category = "Security",
            shortDescription = "Strictly ignores password fields, OTPs, PINs, and sensitive banking/UPI packages during recording",
            activation = "Hardware & accessibility event filter",
            stopping = "Always active",
            status = FeatureStatus.WORKING,
            statusDetails = "Guarantees zero storage or transmission of credentials or financial screens"
        ),
        FeatureEntry(
            id = "z_learned_routines_manager",
            name = "Learned Routines Manager",
            part = "PART Z",
            category = "Automation",
            shortDescription = "View, run, edit trigger phrases, toggle active status, and delete learned routines locally on device",
            activation = "Voice: 'meri seekhi hui cheezein batao', 'routines dikhao', or Settings -> Learned Routines",
            stopping = "UI / Voice command",
            status = FeatureStatus.WORKING
        )
    )

    fun getAllFeatures(): List<FeatureEntry> = allFeatures

    fun getFeaturesByPart(part: String): List<FeatureEntry> {
        val query = part.uppercase().trim()
        return allFeatures.filter { it.part.uppercase().contains(query) }
    }

    fun getFeaturesByCategory(category: String): List<FeatureEntry> {
        val query = category.lowercase().trim()
        return allFeatures.filter { it.category.lowercase().contains(query) }
    }

    fun getFeaturesByStatus(status: FeatureStatus): List<FeatureEntry> {
        return allFeatures.filter { it.status == status }
    }

    fun getBrokenOrNeedsKeyFeatures(): List<FeatureEntry> {
        return allFeatures.filter { 
            it.status == FeatureStatus.BROKEN || 
            it.status == FeatureStatus.NEEDS_API_KEY || 
            it.status == FeatureStatus.BLOCKED_BY_ANDROID ||
            it.status == FeatureStatus.PARTIAL
        }
    }

    fun searchFeatures(query: String): List<FeatureEntry> {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return allFeatures
        return allFeatures.filter {
            it.name.lowercase().contains(q) ||
            it.shortDescription.lowercase().contains(q) ||
            it.part.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            it.activation.lowercase().contains(q)
        }
    }

    fun getSummaryVoiceText(filter: String = ""): String {
        val features = when {
            filter.contains("game") -> getFeaturesByCategory("Gaming")
            filter.contains("photo") -> getFeaturesByCategory("Photo")
            filter.contains("study") -> getFeaturesByCategory("Study")
            filter.contains("money") || filter.contains("paisa") -> getFeaturesByCategory("Finance")
            filter.contains("kaam nahi") || filter.contains("broken") -> getBrokenOrNeedsKeyFeatures()
            filter.contains("part s") || filter.contains(" s ") -> getFeaturesByPart("PART S")
            filter.contains("part q") || filter.contains(" q ") -> getFeaturesByPart("PART Q2")
            filter.contains("part r") || filter.contains(" r ") -> getFeaturesByPart("PART R")
            filter.contains("part t") || filter.contains(" t ") -> getFeaturesByPart("PART T")
            filter.contains("part z") || filter.contains("routine") || filter.contains("seekh") -> getFeaturesByPart("PART Z")
            else -> allFeatures
        }

        val total = features.size
        val working = features.count { it.status == FeatureStatus.WORKING }
        val needsKey = features.count { it.status == FeatureStatus.NEEDS_API_KEY }
        val partial = features.count { it.status == FeatureStatus.PARTIAL }

        val builder = java.lang.StringBuilder()
        builder.append("Maine aapke LiaAI mein total $total features track kiye hain. ")
        builder.append("$working fully working hain, $needsKey ko API key chahiye, aur $partial partial protected hain.\n\n")

        val grouped = features.groupBy { it.part }
        for ((partName, list) in grouped) {
            builder.append("== $partName (${list.size} features) ==\n")
            list.forEach { item ->
                val statusTag = when (item.status) {
                    FeatureStatus.WORKING -> "[WORKING]"
                    FeatureStatus.NEEDS_API_KEY -> "[NEEDS KEY]"
                    FeatureStatus.PARTIAL -> "[PARTIAL]"
                    FeatureStatus.BLOCKED_BY_ANDROID -> "[BLOCKED]"
                    FeatureStatus.UI_ONLY -> "[UI ONLY]"
                    FeatureStatus.BROKEN -> "[BROKEN]"
                    FeatureStatus.NOT_VERIFIED -> "[UNVERIFIED]"
                }
                builder.append("• ${item.name} $statusTag: ${item.shortDescription} (Trigger: ${item.activation})\n")
            }
            builder.append("\n")
        }

        return builder.toString().trim()
    }
}
