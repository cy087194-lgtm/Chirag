package com.Lia.assistant.agent

import com.Lia.assistant.agent.demonstration.DemonstrationRecorder
import com.Lia.assistant.agent.demonstration.LearnedRoutine
import com.Lia.assistant.agent.demonstration.RecordedAction
import com.Lia.assistant.agent.demonstration.RecordedActionType
import com.Lia.assistant.agent.demonstration.RoutineCompiler
import com.Lia.assistant.agent.demonstration.RoutineStep
import com.Lia.assistant.agent.demonstration.RoutineTrigger
import com.Lia.assistant.agent.demonstration.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachByDemonstrationTest {

    @Test
    fun testTeachModeStartIntentParsing() {
        val intent1 = SmartIntentParser.parse("seekhna shuru karo")
        assertTrue(intent1 is ParsedIntent.TeachModeCommand)
        assertEquals(TeachAction.START_LEARNING, (intent1 as ParsedIntent.TeachModeCommand).action)

        val intent2 = SmartIntentParser.parse("ye seekho")
        assertTrue(intent2 is ParsedIntent.TeachModeCommand)
        assertEquals(TeachAction.START_LEARNING, (intent2 as ParsedIntent.TeachModeCommand).action)

        val intent3 = SmartIntentParser.parse("learn this")
        assertTrue(intent3 is ParsedIntent.TeachModeCommand)
        assertEquals(TeachAction.START_LEARNING, (intent3 as ParsedIntent.TeachModeCommand).action)

        val intent4 = SmartIntentParser.parse("meri screen dekho aur seekho")
        assertTrue(intent4 is ParsedIntent.TeachModeCommand)
        assertEquals(TeachAction.START_LEARNING, (intent4 as ParsedIntent.TeachModeCommand).action)
    }

    @Test
    fun testTeachModeStopIntentParsing() {
        val intent1 = SmartIntentParser.parse("seekhna band karo")
        assertTrue(intent1 is ParsedIntent.TeachModeCommand)
        assertEquals(TeachAction.STOP_LEARNING, (intent1 as ParsedIntent.TeachModeCommand).action)

        val intent2 = SmartIntentParser.parse("bas itna")
        assertTrue(intent2 is ParsedIntent.TeachModeCommand)
        assertEquals(TeachAction.STOP_LEARNING, (intent2 as ParsedIntent.TeachModeCommand).action)

        val intent3 = SmartIntentParser.parse("stop learning")
        assertTrue(intent3 is ParsedIntent.TeachModeCommand)
        assertEquals(TeachAction.STOP_LEARNING, (intent3 as ParsedIntent.TeachModeCommand).action)
    }

    @Test
    fun testTeachModeManagementIntentParsing() {
        val listIntent = SmartIntentParser.parse("meri seekhi hui cheezein batao")
        assertTrue(listIntent is ParsedIntent.TeachModeCommand)
        assertEquals(TeachAction.LIST_ROUTINES, (listIntent as ParsedIntent.TeachModeCommand).action)

        val deleteIntent = SmartIntentParser.parse("Keshav wala routine hata do")
        assertTrue(deleteIntent is ParsedIntent.TeachModeCommand)
        val del = deleteIntent as ParsedIntent.TeachModeCommand
        assertEquals(TeachAction.DELETE_ROUTINE, del.action)
        assertEquals("Keshav", del.query)

        val toggleIntent = SmartIntentParser.parse("Keshav wala routine band karo")
        assertTrue(toggleIntent is ParsedIntent.TeachModeCommand)
        val tog = toggleIntent as ParsedIntent.TeachModeCommand
        assertEquals(TeachAction.TOGGLE_ROUTINE, tog.action)
        assertEquals("Keshav", tog.query)
    }

    @Test
    fun testPrivacyGuardProtectsBankingPackages() {
        assertTrue(DemonstrationRecorder.isPackageSensitive("com.phonepe.app"))
        assertTrue(DemonstrationRecorder.isPackageSensitive("net.one97.paytm"))
        assertTrue(DemonstrationRecorder.isPackageSensitive("com.google.android.apps.nbu.paisa.user"))
        assertTrue(DemonstrationRecorder.isPackageSensitive("in.org.npci.upiapp"))
        assertTrue(DemonstrationRecorder.isPackageSensitive("com.sbi.upi"))
        assertTrue(DemonstrationRecorder.isPackageSensitive("com.snapwork.hdfc"))

        // Standard non-banking packages must NOT be blocked
        assertFalse(DemonstrationRecorder.isPackageSensitive("com.whatsapp"))
        assertFalse(DemonstrationRecorder.isPackageSensitive("com.google.android.youtube"))
        assertFalse(DemonstrationRecorder.isPackageSensitive("com.android.chrome"))
    }

    @Test
    fun testRoutineCompiler_removesDuplicateClicksAndGeneratesWhatsAppDirectRoute() {
        val now = 1000L
        val raw = listOf(
            // Open whatsapp
            RecordedAction(
                type = RecordedActionType.CLICK,
                packageName = "com.whatsapp",
                text = "Keshav",
                viewId = "com.whatsapp:id/contact_row",
                timestamp = now
            ),
            // Accidental immediate second tap within 100ms
            RecordedAction(
                type = RecordedActionType.CLICK,
                packageName = "com.whatsapp",
                text = "Keshav",
                viewId = "com.whatsapp:id/contact_row",
                timestamp = now + 80
            ),
            // Type message
            RecordedAction(
                type = RecordedActionType.TYPE_TEXT,
                packageName = "com.whatsapp",
                inputText = "hay",
                timestamp = now + 500
            ),
            // Click send
            RecordedAction(
                type = RecordedActionType.CLICK,
                packageName = "com.whatsapp",
                contentDescription = "Send",
                viewId = "com.whatsapp:id/send",
                timestamp = now + 900
            )
        )

        // 1. Direct check of deduplication
        val cleaned = RoutineCompiler.cleanMistakesAndDeduplicate(raw)
        assertEquals("Duplicate click should be pruned", 3, cleaned.size)

        // 2. Compilation check
        val routine = RoutineCompiler.compile(
            context = null,
            rawActions = raw,
            customName = "Keshav",
            customTriggerPhrase = "Keshav"
        )

        assertNotNull(routine)
        assertEquals("Keshav", routine.name)
        assertEquals("Keshav", routine.trigger.phrase)
        assertTrue(routine.trigger.requiresWakeWord)
        assertEquals("Keshav", routine.variables["contactName"])
        assertEquals("hay", routine.variables["messageText"])

        // Direct route optimization
        assertTrue(routine.directRouteAvailable)
        assertNotNull(routine.directRouteUri)
        assertTrue(routine.directRouteUri!!.contains("wa.me"))

        // Fallback steps: duplicate tap was pruned
        assertEquals(3, routine.steps.size)
    }

    @Test
    fun testLearnedRoutineSerialization() {
        val original = LearnedRoutine(
            id = "test_routine_1",
            name = "Keshav Greeting",
            trigger = RoutineTrigger(
                type = TriggerType.VOICE_PHRASE,
                phrase = "Keshav",
                requiresWakeWord = true
            ),
            directRouteAvailable = true,
            directRouteUri = "https://wa.me/?text=hay",
            steps = listOf(
                RoutineStep(
                    stepIndex = 1,
                    description = "Open Keshav's chat",
                    actionType = RecordedActionType.CLICK,
                    targetPackage = "com.whatsapp",
                    targetText = "Keshav"
                ),
                RoutineStep(
                    stepIndex = 2,
                    description = "Type hay",
                    actionType = RecordedActionType.TYPE_TEXT,
                    targetPackage = "com.whatsapp",
                    textToType = "hay"
                )
            ),
            variables = mapOf("contactName" to "Keshav", "messageText" to "hay"),
            isEnabled = true,
            createdAt = 12345678L,
            lastRunTime = 87654321L,
            lastRunDurationMs = 450L,
            lastRunStatus = "SUCCESS",
            successCount = 5,
            failureCount = 0
        )

        val json = original.toJson()
        val restored = LearnedRoutine.fromJson(json)

        assertNotNull(restored)
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.trigger.phrase, restored.trigger.phrase)
        assertEquals(original.trigger.requiresWakeWord, restored.trigger.requiresWakeWord)
        assertEquals(original.directRouteAvailable, restored.directRouteAvailable)
        assertEquals(original.directRouteUri, restored.directRouteUri)
        assertEquals(original.steps.size, restored.steps.size)
        assertEquals(original.variables["contactName"], restored.variables["contactName"])
        assertEquals(original.successCount, restored.successCount)
        assertEquals(original.lastRunDurationMs, restored.lastRunDurationMs)
    }

    @Test
    fun testWakeWordEnforcementOnRoutineTrigger() {
        val trigger = RoutineTrigger(
            type = TriggerType.VOICE_PHRASE,
            phrase = "keshav",
            requiresWakeWord = true
        )

        fun isMatch(rawSpeech: String, isExplicitWakeWord: Boolean): Boolean {
            val lower = rawSpeech.lowercase().trim()
            if (trigger.requiresWakeWord && !isExplicitWakeWord) {
                val hasEmbeddedWakeWord = lower.startsWith("jarvis") ||
                        lower.startsWith("lia") ||
                        lower.startsWith("hey jarvis") ||
                        lower.startsWith("hey lia")
                if (!hasEmbeddedWakeWord) return false
            }

            val clean = lower
                .replace(Regex("^(hey\\s+)?(jarvis|lia|nova|assistant)[,\\s]*"), "")
                .trim()

            return clean == trigger.phrase ||
                    clean.startsWith(trigger.phrase) ||
                    Regex("\\b${Regex.escape(trigger.phrase)}\\b").containsMatchIn(clean)
        }

        // 1. Spoken with explicit wake word: "Jarvis, Keshav" -> MUST match
        assertTrue(isMatch("Jarvis, Keshav", isExplicitWakeWord = false))
        assertTrue(isMatch("Keshav", isExplicitWakeWord = true))
        assertTrue(isMatch("Hey Lia, Keshav", isExplicitWakeWord = false))

        // 2. Casual background chatter mentioning Keshav without wake word -> MUST be rejected
        assertFalse(isMatch("aaj main Keshav ke ghar gaya tha", isExplicitWakeWord = false))
        assertFalse(isMatch("Keshav kahan hai", isExplicitWakeWord = false))
        assertFalse(isMatch("TV show Keshav", isExplicitWakeWord = false))
    }
}
