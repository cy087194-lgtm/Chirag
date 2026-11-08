package com.Lia.assistant.agent

sealed class ParsedIntent {
    data class NegatedCommand(val originalText: String, val prohibitedAction: String) : ParsedIntent()
    data class WhatsAppMultiStep(val contactName: String, val messageText: String) : ParsedIntent()
    data class GameCoachCommand(val action: GameCoachAction) : ParsedIntent()
    data class WebResearchCommand(val query: String, val topicCategory: String) : ParsedIntent()
    data class AppLaunchCommand(val appName: String) : ParsedIntent()
    data class OneShotCompositeCommand(val rawCommand: String) : ParsedIntent()
    data class AlwaysWatchCommand(val isStart: Boolean, val condition: String? = null) : ParsedIntent()
    data class ScreenIntelligenceCommand(val actionType: String, val query: String = "") : ParsedIntent()
    data class FocusModeCommand(val enable: Boolean) : ParsedIntent()
    data class AutoClickerCommand(val target: String, val intervalSeconds: Int = 5) : ParsedIntent()
    data class FeatureRegistryQuery(val rawQuery: String, val filter: String) : ParsedIntent()
    data class StudyAndBrainCommand(val subType: String, val query: String) : ParsedIntent()
    data class PhotoFeatureCommand(val subType: String, val query: String) : ParsedIntent()
    data class MoneyAndWorkCommand(val subType: String, val query: String) : ParsedIntent()
    data class DailyLifeCommand(val subType: String, val query: String) : ParsedIntent()
    data class SmartSystemCommand(val subType: String, val query: String) : ParsedIntent()
    data class UniversalMessageCommand(val contactName: String, val messageText: String, val app: String) : ParsedIntent()
    data class DirectCallCommand(val contactName: String) : ParsedIntent()
    data class TeachModeCommand(val action: TeachAction, val query: String = "") : ParsedIntent()
    data class ExecuteLearnedRoutineCommand(val routineId: String, val routineName: String) : ParsedIntent()
    data class AppDownloadHelp(val rawQuery: String) : ParsedIntent()
    data class GeneralCommand(val rawText: String) : ParsedIntent()
}

enum class TeachAction {
    START_LEARNING,
    STOP_LEARNING,
    LIST_ROUTINES,
    DELETE_ROUTINE,
    TOGGLE_ROUTINE,
    RELEARN
}

enum class GameCoachAction {
    START, STOP, PAUSE, RESUME, STATUS
}

object SmartIntentParser {

    private val NEGATION_PATTERNS = listOf(
        Regex("\\bmat\\s+karna\\b", RegexOption.IGNORE_CASE),
        Regex("\\bmat\\s+karo\\b", RegexOption.IGNORE_CASE),
        Regex("\\bband\\s+mat\\s+karna\\b", RegexOption.IGNORE_CASE),
        Regex("\\bmat\\s+hatao\\b", RegexOption.IGNORE_CASE),
        Regex("\\bmat\\s+chhedna\\b", RegexOption.IGNORE_CASE),
        Regex("\\bdo\\s+not\\b", RegexOption.IGNORE_CASE),
        Regex("\\bdon't\\b", RegexOption.IGNORE_CASE),
        Regex("\\bnever\\b", RegexOption.IGNORE_CASE),
        Regex("\\bnahi\\s+karna\\b", RegexOption.IGNORE_CASE)
    )

    /**
     * Checks if user explicitly commands NOT to perform an action.
     */
    fun isNegated(text: String): Boolean {
        val lower = text.lowercase().trim()
        return NEGATION_PATTERNS.any { it.containsMatchIn(lower) }
    }

    /**
     * Parses the command into a typed intent.
     */
    fun parse(rawText: String): ParsedIntent {
        val text = rawText.trim()
        val lower = text.lowercase()

        // 1. Check Negation First (Requirement 5)
        if (isNegated(lower)) {
            return ParsedIntent.NegatedCommand(
                originalText = text,
                prohibitedAction = extractProhibitedAction(text)
            )
        }

        // 1a. Check Feature Registry Queries (Requirement PART U)
        if (lower.contains("kya kya joda") ||
            lower.contains("kya kya add") ||
            lower.contains("sab features batao") ||
            lower.contains("features dikhao") ||
            lower.contains("feature registry") ||
            lower.contains("all features") ||
            lower.contains("list of features") ||
            lower.contains("maine kya kya add kiya") ||
            (lower.contains("feature") && lower.contains("batao"))
        ) {
            val filter = when {
                lower.contains("game") -> "game"
                lower.contains("photo") -> "photo"
                lower.contains("study") || lower.contains("padhai") -> "study"
                lower.contains("money") || lower.contains("paisa") || lower.contains("kharcha") -> "money"
                lower.contains("kaam nahi") || lower.contains("broken") -> "broken"
                lower.contains("part s") || lower.contains(" s ") -> "part s"
                lower.contains("part q") || lower.contains(" q ") -> "part q"
                lower.contains("part r") || lower.contains(" r ") -> "part r"
                lower.contains("part t") || lower.contains(" t ") -> "part t"
                else -> ""
            }
            return ParsedIntent.FeatureRegistryQuery(rawQuery = text, filter = filter)
        }

        // 1a-2. Check App Download & Installation Queries
        if (lower.contains("download") || lower.contains("install")) {
            if (lower.contains("app") || lower.contains("apk") || lower.contains("phone") || lower.contains("mobile") ||
                lower.contains("karen") || lower.contains("karein") || lower.contains("nahi ho raha") ||
                lower.contains("nahin ho raha") || lower.contains("kaise") || lower.contains("bug")
            ) {
                return ParsedIntent.AppDownloadHelp(rawQuery = text)
            }
        }

        // 1a-3. Check PART Z Teach by Demonstration Commands
        if (lower in listOf("seekhna band karo", "bas itna", "stop learning", "seekhna roko", "recording band karo") ||
            (lower.contains("seekh") && (lower.contains("band") || lower.contains("roko") || lower.contains("stop")))
        ) {
            return ParsedIntent.TeachModeCommand(TeachAction.STOP_LEARNING)
        }

        if (lower in listOf("seekhna shuru karo", "ye seekho", "learn this", "meri screen dekho aur seekho", "start learning") ||
            lower.contains("seekhna shuru") || lower.contains("ye seekho") || lower.contains("learn this")
        ) {
            return ParsedIntent.TeachModeCommand(TeachAction.START_LEARNING)
        }

        if (lower.contains("seekhi hui cheezein") || lower.contains("meri seekhi hui") || lower.contains("kya kya seekha") || lower.contains("learned routines") || lower.contains("show routines") || lower == "routines") {
            return ParsedIntent.TeachModeCommand(TeachAction.LIST_ROUTINES)
        }

        if (lower.contains("hata do") || lower.contains("delete karo") || lower.contains("delete routine") || lower.contains("remove routine")) {
            val name = when {
                lower.startsWith("delete routine") -> text.substringAfter("routine", "").trim()
                lower.startsWith("remove routine") -> text.substringAfter("routine", "").trim()
                lower.contains("wala") -> text.substringBefore("wala", "").trim()
                lower.contains("routine") -> text.substringBefore("routine", "").trim()
                else -> text.replace(Regex("(?i)(hata do|delete karo)"), "").trim()
            }
            if (name.isNotBlank()) {
                return ParsedIntent.TeachModeCommand(TeachAction.DELETE_ROUTINE, query = name)
            }
        }

        if (lower.contains("wala band karo") || lower.contains("routine band karo") || lower.contains("disable routine") || lower.contains("routine disable karo")) {
            val name = when {
                lower.startsWith("disable routine") -> text.substringAfter("routine", "").trim()
                lower.contains("wala") -> text.substringBefore("wala", "").trim()
                lower.contains("routine") -> text.substringBefore("routine", "").trim()
                else -> text.replace(Regex("(?i)(band karo|disable)"), "").trim()
            }
            if (name.isNotBlank()) {
                return ParsedIntent.TeachModeCommand(TeachAction.TOGGLE_ROUTINE, query = name)
            }
        }

        if (lower.contains("wala badlo") || lower.contains("wala change karo") || lower.contains("routine badlo") || lower.startsWith("relearn routine")) {
            val name = when {
                lower.startsWith("relearn routine") -> text.substringAfter("routine", "").trim()
                lower.contains("wala") -> text.substringBefore("wala", "").trim()
                lower.contains("routine") -> text.substringBefore("routine", "").trim()
                else -> text.replace(Regex("(?i)(badlo|change karo)"), "").trim()
            }
            if (name.isNotBlank()) {
                return ParsedIntent.TeachModeCommand(TeachAction.RELEARN, query = name)
            }
        }

        // 1b. Check Coach & Instant Stop Commands (Requirement T1, T1-FIX, T1-FIX2, T1-STOP)
        if (lower in listOf("stop", "ruk", "ruko", "ruk jao", "bas", "bas karo", "chup", "chup ho ja", "band karo", "coach off", "watch off", "coach chup", "cancel", "enough", "shut up") ||
            lower.contains("game coach off") || lower.contains("game coach band") || lower.contains("coach band") || lower.contains("watch band")
        ) {
            return ParsedIntent.GameCoachCommand(GameCoachAction.STOP)
        }

        if (lower == "coach on" || lower == "watch on" || lower == "game coach on" || lower == "game shuru" ||
            lower.contains("coach on") || lower.contains("game coach on") || lower.contains("game coach chalu") ||
            lower.contains("meri screen dekhte raho") || lower.contains("screen dekhti rehna") ||
            (lower.contains("game") && (lower.contains("tips") || lower.contains("coach"))) ||
            lower.contains("live coach")
        ) {
            return ParsedIntent.GameCoachCommand(GameCoachAction.START)
        }

        // 1c. Check Always-Watch Mode (Requirement S0)
        if (lower.contains("screen") && (lower.contains("band karo") || lower.contains("stop karo") || lower.contains("mat dekho"))) {
            return ParsedIntent.AlwaysWatchCommand(isStart = false)
        }
        if (lower.contains("screen dekhte raho") || lower.contains("screen dekho aur") || lower.contains("always watch")) {
            val condition = if (lower.contains("batana") || lower.contains("alert")) {
                val cond = text.substringAfter("raho", "").substringAfter("watch", "").replace(Regex("(?i)^(,|aur|to)?\\s*"), "").trim()
                if (cond.isNotBlank()) cond else null
            } else null
            return ParsedIntent.AlwaysWatchCommand(isStart = true, condition = condition)
        }

        // 1c. Check Screen Intelligence (Requirement S1)
        if (lower.contains("translate") && (lower.contains("screen") || lower.contains("page"))) {
            return ParsedIntent.ScreenIntelligenceCommand(actionType = "translate")
        }
        if ((lower.contains("bill") || lower.contains("total") || lower.contains("amount")) && (lower.contains("kitna") || lower.contains("kya"))) {
            return ParsedIntent.ScreenIntelligenceCommand(actionType = "question", query = text)
        }
        if (lower.contains("scam check") || lower.contains("fraud check") || lower.contains("scam alert")) {
            return ParsedIntent.ScreenIntelligenceCommand(actionType = "scam_check")
        }
        if (lower.contains("copy") && lower.contains("screen")) {
            return ParsedIntent.ScreenIntelligenceCommand(actionType = "copy_text")
        }

        // 1d. Focus Mode & Auto-Clicker (Requirement S2 & S3)
        if (lower.contains("padhai mode") || lower.contains("focus mode")) {
            val isOff = lower.contains("band") || lower.contains("off") || lower.contains("roko")
            return ParsedIntent.FocusModeCommand(enable = !isOff)
        }
        if (lower.contains("auto click") || (lower.contains("har ") && lower.contains("second") && lower.contains("tap"))) {
            val target = text.substringAfter("par ", "").substringAfter("pe ", "").substringBefore(" tap").trim()
            return ParsedIntent.AutoClickerCommand(target = if (target.isNotBlank()) target else "button")
        }

        // 2. Game Review and Guides
        if (lower.contains("game review") || lower.contains("aaj ka game")) {
            return ParsedIntent.StudyAndBrainCommand("game_review", text)
        }
        if (lower.contains("sensitivity") || lower.contains("landing tips") || lower.contains("game guide")) {
            return ParsedIntent.StudyAndBrainCommand("game_guide", text)
        }

        // 2b. Check Study & Brain Commands (Requirement T2)
        if (lower.contains("diary entry") || (lower.contains("diary") && lower.contains("likho"))) {
            return ParsedIntent.StudyAndBrainCommand("save_diary", text)
        }
        if (lower.contains("second brain") || (lower.contains("search") && lower.contains("notes")) || lower.contains("pichle mahine kya note")) {
            return ParsedIntent.StudyAndBrainCommand("second_brain", text)
        }
        if (lower.contains("english sikhna") || lower.contains("english speaking") || lower.contains("english practice")) {
            return ParsedIntent.StudyAndBrainCommand("english_practice", text)
        }
        if (lower.contains("mock interview") || lower.contains("interview practice")) {
            return ParsedIntent.StudyAndBrainCommand("interview_practice", text)
        }
        if (lower.contains("decision") && (lower.contains("madad") || lower.contains("compare") || lower.contains("vs"))) {
            return ParsedIntent.StudyAndBrainCommand("decision_helper", text)
        }
        if (lower.contains("shayari") || lower.contains("kavita") || lower.contains("poem") || lower.contains("story likho")) {
            return ParsedIntent.StudyAndBrainCommand("creative_writing", text)
        }
        if (lower.contains("math") && (lower.contains("solve") || lower.contains("sawal"))) {
            return ParsedIntent.StudyAndBrainCommand("math_solve", text)
        }

        // 2c. Check Photo Commands (Requirement T3)
        if (lower.contains("ye kya hai") || lower.contains("identify") || lower.contains("kaun sa plant") || lower.contains("medicine label")) {
            return ParsedIntent.PhotoFeatureCommand("identify", text)
        }
        if (lower.contains("document scan") || lower.contains("pdf banao") || lower.contains("scan to pdf")) {
            return ParsedIntent.PhotoFeatureCommand("scan_pdf", text)
        }
        if (lower.contains("image generate") || lower.contains("photo banao") || lower.contains("ai image")) {
            return ParsedIntent.PhotoFeatureCommand("image_gen", text)
        }

        // 2d. Check Money & Work Commands (Requirement T4)
        if (lower.contains("kharcha") || lower.contains("expense summary") || lower.contains("kitna kharch")) {
            return ParsedIntent.MoneyAndWorkCommand("expense_summary", text)
        }
        if ((lower.contains("share") || lower.contains("stock") || lower.contains("bitcoin") || lower.contains("crypto") || lower.contains("nifty")) &&
            (lower.contains("price") || lower.contains("rate") || lower.contains("bhav"))) {
            return ParsedIntent.MoneyAndWorkCommand("price_alert", text)
        }
        if (lower.contains("leave application") || lower.contains("resume") || lower.contains("formal email")) {
            return ParsedIntent.MoneyAndWorkCommand("pro_writing", text)
        }
        if (lower.contains("meeting note") || lower.contains("action items")) {
            return ParsedIntent.MoneyAndWorkCommand("meeting_summary", text)
        }

        // 2e. Check Daily Life Commands (Requirement T5)
        if (lower.contains("fridge mein") || lower.contains("kya banayein") || lower.contains("recipe batao")) {
            return ParsedIntent.DailyLifeCommand("fridge_recipe", text)
        }
        if (lower.contains("workout") || lower.contains("pushup") || lower.contains("diet plan") || lower.contains("water reminder")) {
            return ParsedIntent.DailyLifeCommand("fitness", text)
        }
        if (lower.contains("train status") || lower.contains("flight timing") || lower.contains("itinerary")) {
            return ParsedIntent.DailyLifeCommand("travel", text)
        }
        if (lower.contains("price compare") || (lower.contains("compare") && (lower.contains("amazon") || lower.contains("flipkart")))) {
            return ParsedIntent.DailyLifeCommand("shopping_compare", text)
        }
        if (lower.contains("morning briefing") || lower.contains("cricket score") || lower.contains("live score")) {
            return ParsedIntent.DailyLifeCommand("morning_briefing", text)
        }
        if (lower.contains("panchang") || lower.contains("tithi") || lower.contains("muhurat") || lower.contains("festival")) {
            return ParsedIntent.DailyLifeCommand("panchang", text)
        }
        if (lower.contains("kaun sa gaana") || lower.contains("identify song") || lower.contains("song name")) {
            return ParsedIntent.DailyLifeCommand("song_id", text)
        }

        // 2f. Check Smart System Commands (Requirement T6)
        if (lower.contains("weekly report") || lower.contains("kitna time save")) {
            return ParsedIntent.SmartSystemCommand("weekly_report", text)
        }
        if (lower.contains("iron man") || lower.contains("jarvis style")) {
            return ParsedIntent.SmartSystemCommand("iron_man", text)
        }
        if (lower.contains("hands-free") || lower.contains("wake word")) {
            return ParsedIntent.SmartSystemCommand("hands_free", text)
        }

        // 3. Check Universal Voice Messaging & Calls (Requirement PART V2 & PART W)
        // e.g. "Keshav ko hay bhej do", "Rahul ko SMS bhejo kal milte hain", "Mummy ko call karo"
        if (lower.contains("call") || lower.contains("phone lagao") || lower.contains("dial")) {
            val contact = extractCallRecipient(text)
            if (contact != null) {
                return ParsedIntent.DirectCallCommand(contact)
            }
        }

        if (lower.contains("bhej") || lower.contains("send") || lower.contains("bolo") || lower.contains("message")) {
            val extracted = extractUniversalRecipientAndMessage(text)
            if (extracted != null) {
                val (recipient, msg, targetApp) = extracted
                if (targetApp == "whatsapp") {
                    return ParsedIntent.WhatsAppMultiStep(
                        contactName = recipient,
                        messageText = msg
                    )
                } else {
                    return ParsedIntent.UniversalMessageCommand(
                        contactName = recipient,
                        messageText = msg,
                        app = targetApp
                    )
                }
            }
        }

        // 4. Check Web Research / Breaking News / Updates (Requirement 2)
        if (lower.contains("breaking news") ||
            lower.contains("aaj ki news") ||
            lower.contains("aaj ka samachar") ||
            lower.contains("latest news") ||
            (lower.contains("free fire") && (lower.contains("update") || lower.contains("event") || lower.contains("patch"))) ||
            lower.startsWith("search ") ||
            lower.contains("google pe search") ||
            lower.contains("internet par search") ||
            lower.contains("kya chal raha hai")
        ) {
            val category = when {
                lower.contains("free fire") -> "gaming_free_fire"
                lower.contains("news") || lower.contains("samachar") -> "breaking_news"
                else -> "general_web"
            }
            return ParsedIntent.WebResearchCommand(
                query = text,
                topicCategory = category
            )
        }

        // 4b. Check One-Shot Composite Multi-Step Command (Requirement PART Q2)
        // e.g. "YouTube par Pro Shivay gamer search karo, pehli video par click karke like aur subscribe karo, aur full screen mein chalao."
        val isComposite = (lower.contains("karke") || lower.contains("ke baad") || lower.contains("phir") ||
                (lower.contains("aur") && (lower.contains("click") || lower.contains("like") || lower.contains("subscribe") || lower.contains("screen") || lower.contains("chalao") || lower.contains("search")))) ||
                (lower.contains("youtube") && (lower.contains("search") || lower.contains("click") || lower.contains("video")))
        if (isComposite) {
            return ParsedIntent.OneShotCompositeCommand(text)
        }

        // 5. Check App Launch
        if (lower.startsWith("open ") || lower.contains("khol") || lower.contains("chalao")) {
            val app = com.Lia.assistant.action.AppLauncher.cleanQuery(text)
            if (app.isNotBlank() && app != lower) {
                return ParsedIntent.AppLaunchCommand(app)
            }
        }

        return ParsedIntent.GeneralCommand(text)
    }

    private fun extractProhibitedAction(text: String): String {
        return text.replace(Regex("(?i)\\b(mat\\s+karna|mat\\s+karo|band\\s+mat\\s+karna|do\\s+not|don't|never|nahi\\s+karna)\\b"), "")
            .trim()
    }

    private fun extractCallRecipient(text: String): String? {
        val lower = text.lowercase().trim()
        val koIdx = lower.indexOf(" ko ")
        if (koIdx > 0) {
            val before = text.substring(0, koIdx).trim()
            return before.split(" ").lastOrNull()?.trim()
        }
        val clean = lower.replace(Regex("(?i)\\b(call|karo|phone|lagao|ko)\\b"), "").trim()
        return if (clean.isNotBlank()) clean else null
    }

    private fun extractUniversalRecipientAndMessage(text: String): Triple<String, String, String>? {
        val lower = text.lowercase().trim()

        val app = when {
            lower.contains("sms") || lower.contains("text message") -> "sms"
            lower.contains("telegram") -> "telegram"
            lower.contains("instagram") -> "instagram"
            else -> "whatsapp"
        }

        val koIndex = lower.indexOf(" ko ")
        if (koIndex > 0) {
            val beforeKo = text.substring(0, koIndex).trim()
            // Remove any prefix like "whatsapp par", "sms pe" before recipient
            val words = beforeKo.split(" ")
            val recipient = words.lastOrNull()?.trim() ?: return null

            var afterKo = text.substring(koIndex + 4).trim()
            // Clean out target app mention if placed after 'ko'
            afterKo = afterKo.replace(Regex("(?i)\\b(whatsapp|telegram|instagram|sms)\\s+(par|pe)?\\s*"), "")
            afterKo = afterKo.replace(Regex("^(bhej do|send karo|message karo|bolo|likho|send|bhejo)\\s+", RegexOption.IGNORE_CASE), "")
            afterKo = afterKo.replace(Regex("\\s+(bhej do|bhejo|send karo|send)$", RegexOption.IGNORE_CASE), "").trim()
            afterKo = afterKo.trim('"', '\'')

            if (recipient.isNotBlank() && afterKo.isNotBlank()) {
                return Triple(recipient, afterKo, app)
            }
        }
        return null
    }

    private fun extractWhatsAppRecipientAndMessage(text: String): Pair<String, String>? {
        val res = extractUniversalRecipientAndMessage(text) ?: return null
        return Pair(res.first, res.second)
    }
}
