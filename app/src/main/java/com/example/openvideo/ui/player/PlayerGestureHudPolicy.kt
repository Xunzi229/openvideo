package com.example.openvideo.ui.player

import kotlin.math.roundToInt

enum class PlayerGestureHudKind {
    SEEK,
    BRIGHTNESS,
    VOLUME,
    SPEED,
    LOCK
}

data class PlayerGestureHud(
    val kind: PlayerGestureHudKind,
    val primaryText: String,
    val secondaryText: String = "",
    val levelPercent: Int? = null
)

data class DoubleTapSeekState(
    val side: PlayerSwipeSide,
    val accumulatedMs: Long,
    val lastTapUptimeMs: Long
)

data class DoubleTapSeekResult(
    val deltaMs: Long,
    val hud: PlayerGestureHud,
    val nextState: DoubleTapSeekState,
    val isAccumulated: Boolean
)

object PlayerGestureHudPolicy {
    private const val DOUBLE_TAP_ACCUMULATE_TIMEOUT_MS = 650L

    fun seek(targetMs: Long, durationMs: Long, deltaMs: Long): PlayerGestureHud {
        val signedDelta = if (deltaMs >= 0) "+${formatTime(deltaMs)}" else "-${formatTime(-deltaMs)}"
        val duration = if (PlayerTimeline.hasSeekableDuration(durationMs)) formatTime(durationMs) else "--:--"
        return PlayerGestureHud(
            kind = PlayerGestureHudKind.SEEK,
            primaryText = signedDelta,
            secondaryText = "${formatTime(targetMs)} / $duration"
        )
    }

    fun level(kind: PlayerGestureHudKind, value: Float): PlayerGestureHud {
        val percent = (value.coerceIn(0f, 1f) * 100f).roundToInt()
        return PlayerGestureHud(
            kind = kind,
            primaryText = "$percent%",
            levelPercent = percent
        )
    }

    fun speed(speed: Float): PlayerGestureHud =
        PlayerGestureHud(
            kind = PlayerGestureHudKind.SPEED,
            primaryText = "${String.format(java.util.Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')}x"
        )

    fun doubleTapSeek(
        previous: DoubleTapSeekState?,
        tapSide: PlayerSwipeSide,
        intervalMs: Long,
        nowMs: Long
    ): DoubleTapSeekResult {
        val direction = when (tapSide) {
            PlayerSwipeSide.LEFT -> -1
            PlayerSwipeSide.RIGHT -> 1
            PlayerSwipeSide.NONE -> 0
        }
        val step = intervalMs.coerceAtLeast(0)
        val canAccumulate = previous != null &&
            direction != 0 &&
            previous.side == tapSide &&
            nowMs - previous.lastTapUptimeMs <= DOUBLE_TAP_ACCUMULATE_TIMEOUT_MS
        val accumulated = when {
            direction == 0 -> 0
            canAccumulate -> previous!!.accumulatedMs + step
            else -> step
        }
        val delta = accumulated * direction
        return DoubleTapSeekResult(
            deltaMs = delta,
            hud = seek(targetMs = accumulated, durationMs = 0, deltaMs = delta).copy(secondaryText = ""),
            nextState = DoubleTapSeekState(
                side = tapSide,
                accumulatedMs = accumulated,
                lastTapUptimeMs = nowMs
            ),
            isAccumulated = canAccumulate
        )
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms.coerceAtLeast(0) / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}
