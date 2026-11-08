package com.Lia.assistant.voice

class ToolCallDeduplicator(private val maxSize: Int = 100) {
    private val seenIds = LinkedHashSet<String>()
    private val lock = Any()

    /**
     * Checks if the tool call ID has been seen before.
     * If not seen, records it and returns true (call should be processed).
     * If already seen, returns false (call is a duplicate and should be skipped).
     */
    fun shouldProcess(id: String): Boolean {
        if (id.isBlank()) return false
        synchronized(lock) {
            if (seenIds.contains(id)) {
                return false
            }
            if (seenIds.size >= maxSize) {
                val oldest = seenIds.iterator().next()
                seenIds.remove(oldest)
            }
            seenIds.add(id)
            return true
        }
    }

    fun clear() {
        synchronized(lock) {
            seenIds.clear()
        }
    }

    fun size(): Int {
        synchronized(lock) {
            return seenIds.size
        }
    }
}
