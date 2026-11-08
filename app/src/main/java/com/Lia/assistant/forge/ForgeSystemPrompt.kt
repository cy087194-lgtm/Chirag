package com.Lia.assistant.forge

object ForgeSystemPrompt {

    private const val BASE_DIRECTIVES = """
You are the world-class Website Forge engine for Lia AI.
Generate a complete, fully functional, self-contained single-page HTML5 website based on the user's prompt.

MANDATORY RULES:
1. Return ONLY the complete HTML5 code starting with <!DOCTYPE html> and ending with </html>. Do NOT include markdown code fences (no ```html).
2. STACK & LIBRARIES:
   - Tailwind CSS via CDN: <script src="https://cdn.tailwindcss.com"></script>
   - GSAP via CDN: <script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.5/gsap.min.js"></script>
   - Three.js via CDN: <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
3. 3D & VISUALS:
   - Hero section MUST contain an interactive Three.js 3D canvas (e.g. geometric energy field, particle cosmos, floating 3D objects reacting to pointer and touch).
   - Use CSS 3D transforms (perspective: 1000px, transform-style: preserve-3d, rotateX, rotateY on hover/drag) across cards and showcase items.
   - High-contrast, dark-mode night aesthetics with vibrant luminous accents.
4. CONTENT & STRUCTURE (5 to 6 sections):
   - 1: Floating Navigation bar with logo, links, and responsive mobile hamburger menu.
   - 2: Immersive 3D Hero with bold headline, subtitle, magnetic CTA button.
   - 3: Bento Grid / Feature showcase with 3D hover tilt.
   - 4: Interactive Interactive Section (calculator, pricing toggle, live filter, or tabbed showcase).
   - 5: Testimonials or Gallery (using https://picsum.photos or https://i.pravatar.cc with dark gradient overlays). Maximum 2 photo sections.
   - 6: Final Call to Action with particle background and Footer.
5. ROBUSTNESS & ACCESSIBILITY:
   - Never hide content waiting for animations to trigger; all text and controls must be legible immediately.
   - Fully responsive on mobile, tablet, and desktop screens.
   - Respect prefers-reduced-motion in animations.
"""

    fun forNewSite(prompt: String): String {
        return "$BASE_DIRECTIVES\n\nUSER PROMPT:\nBuild a magnificent website for: $prompt"
    }

    fun forEdit(currentHtml: String, changeRequest: String): String {
        return """
$BASE_DIRECTIVES

EXISTING WEBSITE HTML:
$currentHtml

CHANGE REQUEST:
$changeRequest

INSTRUCTIONS FOR EDIT:
Apply the requested changes cleanly to the existing website. Maintain all other working sections, scripts, styles, and Three.js 3D scenes. Return the complete updated HTML file from <!DOCTYPE html> to </html>.
""".trimIndent()
    }
}
