package com.example.openvideo.ui.player

import com.example.openvideo.core.player.PlaybackTimelinePolicy

object PlayerTimeline {
    const val SCALED_SEEK_BAR_MAX = 10_000

    data class SeekBarState(
        val enabled: Boolean,
        val max: Int,
        val progress: Int
    )

    fun seekBarState(positionMs: Long, durationMs: Long): SeekBarState {
        if (!hasSeekableDuration(durationMs)) {
            return SeekBarState(enabled = false, max = 0, progress = 0)
        }

        if (durationMs <= Int.MAX_VALUE) {
            val max = durationMs.toInt()
            return SeekBarState(
                enabled = true,
                max = max,
                progress = positionMs.coerceIn(0, durationMs).toInt()
            )
        }

        return SeekBarState(
            enabled = true,
            max = SCALED_SEEK_BAR_MAX,
            progress = ((positionMs.coerceIn(0, durationMs).toDouble() / durationMs) * SCALED_SEEK_BAR_MAX)
                .toInt()
                .coerceIn(0, SCALED_SEEK_BAR_MAX)
        )
    }

    fun positionFromSeekBar(progress: Int, max: Int, durationMs: Long): Long {
        if (!hasSeekableDuration(durationMs) || max <= 0) return 0
        if (durationMs <= Int.MAX_VALUE) return progress.toLong().coerceIn(0, durationMs)

        return ((progress.coerceIn(0, max).toDouble() / max) * durationMs)
            .toLong()
            .coerceIn(0, durationMs)
    }

    fun safeSeekTarget(currentMs: Long, deltaMs: Long, durationMs: Long): Long {
        val raw = currentMs + deltaMs
        return if (hasSeekableDuration(durationMs)) raw.coerceIn(0, durationMs) else raw.coerceAtLeast(0)
    }

    fun durationText(durationMs: Long, formatter: (Long) -> String): String {
        return if (hasSeekableDuration(durationMs)) formatter(durationMs) else "--:--"
    }

    fun hasSeekableDuration(durationMs: Long): Boolean {
        return PlaybackTimelinePolicy.hasSeekableDuration(durationMs)
    }
}
