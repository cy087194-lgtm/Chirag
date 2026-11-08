package com.Lia.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallDeduplicatorTest {

    @Test
    fun testDeduplicationOfCalls() {
        val deduplicator = ToolCallDeduplicator(maxSize = 5)

        assertTrue(deduplicator.shouldProcess("call_1"))
        assertFalse(deduplicator.shouldProcess("call_1")) // Duplicate

        assertTrue(deduplicator.shouldProcess("call_2"))
        assertTrue(deduplicator.shouldProcess("call_3"))
        assertFalse(deduplicator.shouldProcess("call_2")) // Duplicate

        assertFalse(deduplicator.shouldProcess("")) // Blank should be skipped
    }

    @Test
    fun testEvictionWhenExceedingCapacity() {
        val deduplicator = ToolCallDeduplicator(maxSize = 3)

        assertTrue(deduplicator.shouldProcess("c1"))
        assertTrue(deduplicator.shouldProcess("c2"))
        assertTrue(deduplicator.shouldProcess("c3"))
        assertEquals(3, deduplicator.size())

        // Adding c4 should evict oldest (c1)
        assertTrue(deduplicator.shouldProcess("c4"))
        assertEquals(3, deduplicator.size())

        // c1 was evicted, so it can be processed again
        assertTrue(deduplicator.shouldProcess("c1"))
    }
}
