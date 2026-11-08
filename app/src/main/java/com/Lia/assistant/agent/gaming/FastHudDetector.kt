package com.Lia.assistant.agent.gaming

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max

object FastHudDetector {

    /**
     * Fast pixel inspection on HUD elements:
     * 1. Health bar level / colour (< 30% HP)
     * 2. Perimeter red damage indicators (direction of incoming fire)
     */
    fun detectFastHudEvents(bitmap: Bitmap): List<TacticalEvent> {
        val events = mutableListOf<TacticalEvent>()
        val w = bitmap.width
        val h = bitmap.height
        if (w < 100 || h < 100) return emptyList()

        // 1. Check HP Bar Region: Lower-center / bottom 15% of screen
        val hpStartY = (h * 0.85f).toInt()
        val hpEndY = (h * 0.95f).toInt()
        val hpStartX = (w * 0.35f).toInt()
        val hpEndX = (w * 0.65f).toInt()

        var totalSamples = 0
        var criticalRedHpCount = 0

        val stepX = max(1, (hpEndX - hpStartX) / 20)
        val stepY = max(1, (hpEndY - hpStartY) / 5)

        for (x in hpStartX..hpEndX step stepX) {
            for (y in hpStartY..hpEndY step stepY) {
                if (x < w && y < h) {
                    val px = bitmap.getPixel(x, y)
                    val r = Color.red(px)
                    val g = Color.green(px)
                    val b = Color.blue(px)
                    totalSamples++
                    // Red flashing health bar (high red, low green/blue)
                    if (r > 190 && g < 70 && b < 70) {
                        criticalRedHpCount++
                    }
                }
            }
        }

        if (totalSamples > 0 && (criticalRedHpCount.toFloat() / totalSamples) > 0.25f) {
            events.add(
                TacticalEvent(
                    type = "low_health",
                    direction = "front",
                    urgency = 3,
                    say = "Health 30% hai, heal karo"
                )
            )
        }

        // 2. Check Perimeter Damage Flash (Red arc indicating bullet hit direction)
        val damageDir = detectDamageDirection(bitmap, w, h)
        if (damageDir != null) {
            val sayText = when (damageDir) {
                "left" -> "left se fire, cover lo"
                "right" -> "right se fire, cover lo"
                "behind" -> "peeche se fire, move karo"
                else -> "samne se fire, cover lo"
            }
            events.add(
                TacticalEvent(
                    type = "enemy_near",
                    direction = damageDir,
                    urgency = 3,
                    say = sayText
                )
            )
        }

        return events
    }

    private fun detectDamageDirection(bitmap: Bitmap, w: Int, h: Int): String? {
        val margin = 30
        var leftRed = 0
        var rightRed = 0
        var topRed = 0
        var bottomRed = 0

        // Sample along left and right vertical edges
        val vStep = max(1, h / 20)
        for (y in margin until (h - margin) step vStep) {
            if (isRedDamagePixel(bitmap.getPixel(10, y))) leftRed++
            if (isRedDamagePixel(bitmap.getPixel(w - 11, y))) rightRed++
        }

        // Sample along top and bottom edges
        val hStep = max(1, w / 20)
        for (x in margin until (w - margin) step hStep) {
            if (isRedDamagePixel(bitmap.getPixel(x, 10))) topRed++
            if (isRedDamagePixel(bitmap.getPixel(x, h - 11))) bottomRed++
        }

        val maxRed = max(max(leftRed, rightRed), max(topRed, bottomRed))
        if (maxRed >= 4) {
            return when (maxRed) {
                leftRed -> "left"
                rightRed -> "right"
                bottomRed -> "behind"
                else -> "front"
            }
        }
        return null
    }

    private fun isRedDamagePixel(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return r > 210 && g < 60 && b < 60
    }
}
