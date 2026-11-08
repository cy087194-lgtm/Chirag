package com.Lia.assistant.forge

object ForgeStageDetector {

    fun detect(partialHtml: String): ForgeStage {
        val lower = partialHtml.lowercase()

        if (lower.contains("</html>")) {
            return ForgeStage.FINALIZING
        }

        if (lower.contains("gsap") || lower.contains("addeventlistener") || (lower.contains("<script") && lower.contains("function"))) {
            return ForgeStage.INTERACTIVITY
        }

        if (lower.contains("<canvas") || lower.contains("three") || lower.contains("webgl")) {
            return ForgeStage.VISUALS
        }

        if (lower.contains("<style") || lower.contains("tailwind") || lower.contains("class=\"")) {
            return ForgeStage.STYLING
        }

        if (lower.contains("<body") || lower.contains("<header") || lower.contains("<nav") || lower.contains("<section")) {
            return ForgeStage.SKELETON
        }

        return ForgeStage.INITIALIZING
    }

    fun extractSectionNames(html: String): List<String> {
        val regex = Regex("""<section[^>]*id=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val sections = mutableListOf<String>()
        val matches = regex.findAll(html)
        for (match in matches) {
            val name = match.groupValues[1].replace("-", " ").replace("_", " ").capitalizeWords()
            if (name.isNotBlank() && !sections.contains(name)) {
                sections.add(name)
            }
        }
        return sections
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
