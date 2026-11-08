package com.Lia.assistant.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OneShotExecutionTest {

    @Test
    fun planLocally_userCompositeCommand_generatesOrderedBackToBackSteps() {
        val command = "YouTube par Pro Shivay gamer search karo, pehli video par click karke like aur subscribe karo, aur full screen mein chalao."
        val plan = OneShotPlanner.planLocally(command)

        assertNotNull(plan)
        assertTrue("Plan should contain at least 5 steps", plan.steps.size >= 5)

        // Step 1: launch YouTube
        val step1 = plan.steps[0]
        assertEquals("launch_app", step1.action)
        assertEquals("youtube", step1.target.lowercase())

        // Step 2: search button
        val step2 = plan.steps[1]
        assertEquals("click", step2.action)
        assertEquals("search", step2.target)

        // Step 3: type query
        val step3 = plan.steps[2]
        assertEquals("type_and_search", step3.action)
        assertTrue(step3.query.contains("Pro Shivay", ignoreCase = true))

        // Step 4: first video
        val step4 = plan.steps[3]
        assertEquals("click_first_video", step4.action)

        // Like step
        val likeStep = plan.steps.firstOrNull { it.target == "like" }
        assertNotNull("Like step should be present", likeStep)
        assertTrue("Like step should be skippable if already done", likeStep!!.canSkipIfDone)

        // Subscribe step
        val subStep = plan.steps.firstOrNull { it.target == "subscribe" }
        assertNotNull("Subscribe step should be present", subStep)
        assertTrue("Subscribe step should be skippable if already done", subStep!!.canSkipIfDone)

        // Fullscreen step
        val fsStep = plan.steps.firstOrNull { it.action == "fullscreen" }
        assertNotNull("Fullscreen step should be present", fsStep)
    }

    @Test
    fun smartIntentParser_identifiesCompositeOneShotCommand() {
        val command = "YouTube par Pro Shivay gamer search karo, pehli video par click karke like aur subscribe karo, aur full screen mein chalao."
        val parsed = SmartIntentParser.parse(command)

        assertTrue(
            "Expected OneShotCompositeCommand for multi-step composite instructions",
            parsed is ParsedIntent.OneShotCompositeCommand
        )
        assertEquals(command, (parsed as ParsedIntent.OneShotCompositeCommand).rawCommand)
    }

    @Test
    fun oneShotProgressHolder_tracksLiveStepProgress() {
        OneShotProgressHolder.startTask("Play Video", 6)
        assertEquals("(1/6)", OneShotProgressHolder.liveProgressText.value)
        assertTrue(OneShotProgressHolder.isExecuting.value)

        OneShotProgressHolder.updateStep(3, 6)
        assertEquals("(3/6)", OneShotProgressHolder.liveProgressText.value)

        OneShotProgressHolder.updateStep(6, 6)
        assertEquals("(6/6)", OneShotProgressHolder.liveProgressText.value)

        OneShotProgressHolder.finishTask()
        assertEquals(null, OneShotProgressHolder.liveProgressText.value)
        assertTrue(!OneShotProgressHolder.isExecuting.value)
    }
}
