package com.Lia.assistant.voice

import org.junit.Assert.assertTrue
import org.junit.Test

class LiaToolCatalogTest {

    @Test
    fun testToolCatalogContainsAllEssentialPhoneControlTools() {
        val declarations = LiaToolCatalog.declarations()
        val toolNames = mutableListOf<String>()
        for (i in 0 until declarations.length()) {
            val toolObj = declarations.getJSONObject(i)
            toolNames.add(toolObj.getString("name"))
        }

        val requiredTools = listOf(
            "open_app",
            "go_home",
            "open_settings",
            "open_camera",
            "read_screen",
            "tap_text",
            "type_text",
            "scroll_screen",
            "go_back",
            "recent_apps",
            "set_volume",
            "toggle_torch",
            "get_battery_status",
            "save_memory",
            "get_memories",
            "forget_memory",
            "explain_screen",
            "autonomous_agent",
            "web_research",
            "game_coach"
        )

        for (expected in requiredTools) {
            assertTrue("Expected tool '$expected' to be registered in catalog", toolNames.contains(expected))
        }
    }
}
