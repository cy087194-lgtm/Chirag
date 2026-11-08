package com.Lia.assistant.forge

object ForgeHtml {

    fun stripFences(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```html", ignoreCase = true)) {
            text = text.substring(7).trim()
        } else if (text.startsWith("```")) {
            text = text.substring(3).trim()
        }

        if (text.endsWith("```")) {
            text = text.substring(0, text.length - 3).trim()
        }
        return text
    }

    fun looksLikeHtml(raw: String): Boolean {
        val lower = raw.lowercase().trim()
        return lower.contains("<!doctype html") ||
                lower.contains("<html") ||
                (lower.contains("<head") && lower.contains("<body")) ||
                lower.contains("<div")
    }

    fun repairTruncated(html: String): String {
        var fixed = html.trim()

        // Close common unclosed tags
        if (!fixed.contains("</html>", ignoreCase = true)) {
            if (fixed.contains("<script", ignoreCase = true) && !fixed.contains("</script>", ignoreCase = true)) {
                fixed += "\n</script>"
            }
            if (fixed.contains("<style", ignoreCase = true) && !fixed.contains("</style>", ignoreCase = true)) {
                fixed += "\n</style>"
            }
            if (fixed.contains("<body", ignoreCase = true) && !fixed.contains("</body>", ignoreCase = true)) {
                fixed += "\n</body>"
            }
            fixed += "\n</html>"
        }
        return fixed
    }

    private const val SAFETY_NET_SCRIPT = """
<script id="forge-safety-net">
(function() {
    if (window.__FORGE_SAFETY_NET_ATTACHED__) return;
    window.__FORGE_SAFETY_NET_ATTACHED__ = true;
    setTimeout(function() {
        var elements = document.querySelectorAll('*');
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            if (el.tagName === 'CANVAS' || el.tagName === 'SCRIPT' || el.tagName === 'STYLE') continue;
            var style = window.getComputedStyle(el);
            if (style.opacity === '0' && style.position !== 'fixed' && style.pointerEvents !== 'none' && el.getAttribute('aria-hidden') !== 'true') {
                el.style.transition = 'opacity 0.6s ease';
                el.style.opacity = '1';
            }
        }
    }, 1400);
})();
</script>
"""

    fun withSafetyNet(html: String): String {
        if (html.contains("id=\"forge-safety-net\"")) return html

        return if (html.contains("</body>", ignoreCase = true)) {
            html.replace("</body>", "$SAFETY_NET_SCRIPT\n</body>", ignoreCase = true)
        } else if (html.contains("</html>", ignoreCase = true)) {
            html.replace("</html>", "$SAFETY_NET_SCRIPT\n</html>", ignoreCase = true)
        } else {
            "$html\n$SAFETY_NET_SCRIPT"
        }
    }
}
