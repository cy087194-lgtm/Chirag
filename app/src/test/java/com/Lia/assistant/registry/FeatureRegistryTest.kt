package com.Lia.assistant.registry

import com.Lia.assistant.agent.GameCoachAction
import com.Lia.assistant.agent.ParsedIntent
import com.Lia.assistant.agent.SmartIntentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureRegistryTest {

    @Test
    fun featureRegistry_containsAllMajorParts() {
        val allFeatures = FeatureRegistry.getAllFeatures()
        assertTrue("Expected at least 35 features in registry, got ${allFeatures.size}", allFeatures.size >= 35)

        val parts = allFeatures.map { it.part }.toSet()
        assertTrue(parts.any { it.contains("PART Q2") })
        assertTrue(parts.any { it.contains("PART R") })
        assertTrue(parts.any { it.contains("PART S") })
        assertTrue(parts.any { it.contains("PART T1") })
        assertTrue(parts.any { it.contains("PART T2") })
        assertTrue(parts.any { it.contains("PART T3") })
        assertTrue(parts.any { it.contains("PART T4") })
        assertTrue(parts.any { it.contains("PART T5") })
        assertTrue(parts.any { it.contains("PART T6") })
        assertTrue(parts.any { it.contains("PART T7") })
        assertTrue(parts.any { it.contains("PART U") })
        assertTrue(parts.any { it.contains("PART Z") })
    }

    @Test
    fun featureRegistry_filterByCategoryAndPart() {
        val gameFeatures = FeatureRegistry.getFeaturesByCategory("Gaming")
        assertTrue(gameFeatures.isNotEmpty())
        assertTrue(gameFeatures.any { it.id == "t1_game_coach" })

        val photoFeatures = FeatureRegistry.getFeaturesByCategory("Photo")
        assertTrue(photoFeatures.isNotEmpty())
        assertTrue(photoFeatures.any { it.id == "t3_object_identifier" })

        val studyFeatures = FeatureRegistry.getFeaturesByCategory("Study")
        assertTrue(studyFeatures.isNotEmpty())
        assertTrue(studyFeatures.any { it.id == "t2_step_by_step_tutor" })

        val brokenOrKeyFeatures = FeatureRegistry.getBrokenOrNeedsKeyFeatures()
        assertTrue(brokenOrKeyFeatures.isNotEmpty())
        assertTrue(brokenOrKeyFeatures.any { it.status == FeatureStatus.NEEDS_API_KEY })
    }

    @Test
    fun featureRegistry_getSummaryVoiceText_containsCountsAndGroupings() {
        val text = FeatureRegistry.getSummaryVoiceText()
        assertTrue(text.contains("total"))
        assertTrue(text.contains("fully working"))
        assertTrue(text.contains("PART Q2"))
        assertTrue(text.contains("PART T1"))
    }

    @Test
    fun smartIntentParser_detectsFeatureQueries() {
        val intent1 = SmartIntentParser.parse("tumhare andar maine kya kya add kiya hai")
        assertTrue(intent1 is ParsedIntent.FeatureRegistryQuery)

        val intent2 = SmartIntentParser.parse("kya kya joda hai sab batao")
        assertTrue(intent2 is ParsedIntent.FeatureRegistryQuery)

        val intent3 = SmartIntentParser.parse("sirf game wale feature batao")
        assertTrue(intent3 is ParsedIntent.FeatureRegistryQuery)
        assertEquals("game", (intent3 as ParsedIntent.FeatureRegistryQuery).filter)
    }

    @Test
    fun smartIntentParser_detectsGameCoachOnOff() {
        val onIntent = SmartIntentParser.parse("game coach on")
        assertTrue(onIntent is ParsedIntent.GameCoachCommand)
        assertEquals(GameCoachAction.START, (onIntent as ParsedIntent.GameCoachCommand).action)

        val stopIntent = SmartIntentParser.parse("chup")
        assertTrue(stopIntent is ParsedIntent.GameCoachCommand)
        assertEquals(GameCoachAction.STOP, (stopIntent as ParsedIntent.GameCoachCommand).action)
    }
}
