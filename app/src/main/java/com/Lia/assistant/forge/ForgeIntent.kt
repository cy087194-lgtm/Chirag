package com.Lia.assistant.forge

object ForgeIntent {

    private val SUBJECT_KEYWORDS = listOf(
        "website", "web page", "webpage", "site", "landing page", "portfolio", "blog"
    )

    private val BUILD_VERBS = listOf(
        "build", "create", "make", "generate", "code", "design", "develop",
        "bana", "banao", "banaye", "banado", "banakar"
    )

    private val QUESTION_WORDS = listOf(
        "how to", "what is", "kaise banate", "kyun", "can a website", "why do"
    )

    /**
     * Extracts a website creation prompt if the input represents a direct build command.
     * Needs a website/web page/landing page subject AND a build verb.
     * Ignores informational questions (e.g. "How to build a website?").
     */
    fun extract(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val lower = trimmed.lowercase()

        // Ignore informational questions
        for (q in QUESTION_WORDS) {
            if (lower.startsWith(q) || lower.contains(q)) {
                return null
            }
        }
        if (lower.endsWith("?")) {
            return null
        }

        val hasSubject = SUBJECT_KEYWORDS.any { lower.contains(it) }
        val hasBuildVerb = BUILD_VERBS.any { lower.contains(it) }

        if (hasSubject && hasBuildVerb) {
            return trimmed
        }

        return null
    }
}
