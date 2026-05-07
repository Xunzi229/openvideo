package com.example.openvideo.ui.player

object IntroOutroSkipPolicy {
    enum class Kind { INTRO, OUTRO }

    data class Target(val positionMs: Long, val kind: Kind)

    fun skipTarget(
        enabled: Boolean,
        currentPositionMs: Long,
        durationMs: Long,
        introSeconds: Int,
        outroSeconds: Int,
        hasSkippedIntro: Boolean,
        hasSkippedOutro: Boolean
    ): Target? {
        if (!enabled) return null

        val introEndMs = introSeconds.coerceAtLeast(0) * 1000L
        if (!hasSkippedIntro && introEndMs > 0 && currentPositionMs < introEndMs) {
            return Target(introEndMs, Kind.INTRO)
        }

        val duration = durationMs.takeIf { it > 0 } ?: return null
        val outroMs = outroSeconds.coerceAtLeast(0) * 1000L
        val outroStartMs = (duration - outroMs).coerceAtLeast(0)
        if (!hasSkippedOutro && outroMs > 0 && currentPositionMs >= outroStartMs && currentPositionMs < duration) {
            return Target(duration, Kind.OUTRO)
        }

        return null
    }
}
