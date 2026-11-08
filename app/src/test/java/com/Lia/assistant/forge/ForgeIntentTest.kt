package com.Lia.assistant.forge

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ForgeIntentTest {

    @Test
    fun testValidBuildIntents() {
        assertNotNull(ForgeIntent.extract("Lia, ek cafe ki website banao"))
        assertNotNull(ForgeIntent.extract("Create a portfolio website for a photographer"))
        assertNotNull(ForgeIntent.extract("build a modern landing page for a startup"))
        assertNotNull(ForgeIntent.extract("make a bakery webpage"))
    }

    @Test
    fun testIgnoredQuestionsAndGeneralQueries() {
        assertNull(ForgeIntent.extract("How to build a website?"))
        assertNull(ForgeIntent.extract("what is a landing page?"))
        assertNull(ForgeIntent.extract("Can you tell me about websites?"))
        assertNull(ForgeIntent.extract("What is the weather today"))
        assertNull(ForgeIntent.extract(""))
    }
}
