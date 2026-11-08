package com.Lia.assistant.forge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeLogicTest {

    @Test
    fun testSseParsing() {
        val ssePayload = """{"candidates":[{"content":{"parts":[{"text":"<!DOCTYPE html><html>"}]}}]}"""
        val text = SseParser.extractText(ssePayload)
        assertEquals("<!DOCTYPE html><html>", text)

        val finishPayload = """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":""}]}}]}"""
        assertEquals("STOP", SseParser.extractFinishReason(finishPayload))

        assertNull(SseParser.extractText("[DONE]"))
        assertNull(SseParser.extractText(""))
    }

    @Test
    fun testFenceStripping() {
        val fenced = "```html\n<!DOCTYPE html><html></html>\n```"
        val stripped = ForgeHtml.stripFences(fenced)
        assertEquals("<!DOCTYPE html><html></html>", stripped)
    }

    @Test
    fun testLooksLikeHtml() {
        assertTrue(ForgeHtml.looksLikeHtml("<!DOCTYPE html><html><head></head><body></body></html>"))
        assertTrue(ForgeHtml.looksLikeHtml("<div>Hello World</div>"))
        assertFalse(ForgeHtml.looksLikeHtml("Just some plain text"))
    }

    @Test
    fun testTruncationRepair() {
        val unclosed = "<html><head><style>body { color: red; }</style></head><body><script>console.log('hi')"
        val repaired = ForgeHtml.repairTruncated(unclosed)
        assertTrue(repaired.contains("</script>"))
        assertTrue(repaired.contains("</body>"))
        assertTrue(repaired.contains("</html>"))
    }

    @Test
    fun testSafetyNetIdempotency() {
        val html = "<html><body><h1>Test</h1></body></html>"
        val withNet = ForgeHtml.withSafetyNet(html)
        assertTrue(withNet.contains("id=\"forge-safety-net\""))

        // Second call should not double-inject
        val doubleNet = ForgeHtml.withSafetyNet(withNet)
        assertEquals(withNet, doubleNet)
    }

    @Test
    fun testStageDetectionProgression() {
        assertEquals(ForgeStage.INITIALIZING, ForgeStageDetector.detect("<!DOCTYPE html><head>"))
        assertEquals(ForgeStage.SKELETON, ForgeStageDetector.detect("<!DOCTYPE html><body><header>"))
        assertEquals(ForgeStage.STYLING, ForgeStageDetector.detect("<!DOCTYPE html><body><style>.hero{}</style>"))
        assertEquals(ForgeStage.VISUALS, ForgeStageDetector.detect("<!DOCTYPE html><body><canvas id='hero-3d'></canvas>"))
        assertEquals(ForgeStage.INTERACTIVITY, ForgeStageDetector.detect("<!DOCTYPE html><body><script>gsap.to()</script>"))
        assertEquals(ForgeStage.FINALIZING, ForgeStageDetector.detect("<!DOCTYPE html><body></body></html>"))
    }
}
