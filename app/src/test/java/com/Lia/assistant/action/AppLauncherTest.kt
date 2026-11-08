package com.Lia.assistant.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLauncherTest {

    private val sampleApps = mapOf(
        "youtube" to "com.google.android.youtube",
        "youtube music" to "com.google.android.apps.youtube.music",
        "whatsapp" to "com.whatsapp",
        "camera" to "com.android.camera",
        "settings" to "com.android.settings",
        "google maps" to "com.google.android.apps.maps",
        "instagram" to "com.instagram.android"
    )

    @Test
    fun testExactMatch() {
        assertEquals("com.google.android.youtube", AppLauncher.findApp("youtube", sampleApps))
        assertEquals("com.whatsapp", AppLauncher.findApp("whatsapp", sampleApps))
    }

    @Test
    fun testPrefixAndClosestLength() {
        assertEquals("com.google.android.youtube", AppLauncher.findApp("youtub", sampleApps))
        assertEquals("com.google.android.apps.youtube.music", AppLauncher.findApp("youtube music", sampleApps))
        assertEquals("com.google.android.apps.maps", AppLauncher.findApp("maps", sampleApps))
    }

    @Test
    fun testNoLooseSubstring() {
        // "app" should not match "whatsapp" because it's a loose substring, not a prefix or exact
        assertNull(AppLauncher.findApp("app", mapOf("whatsapp" to "com.whatsapp")))
    }

    @Test
    fun testEmptyQuery() {
        assertNull(AppLauncher.findApp("", sampleApps))
        assertNull(AppLauncher.findApp("   ", sampleApps))
    }
}
