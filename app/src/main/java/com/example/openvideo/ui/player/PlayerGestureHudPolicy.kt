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
    fun seek(targetMs: Long, durationMs: Long, deltaMs: Long): PlayerGestureHud {
        val signedDelta = if (deltaMs >= 0) {
            "+${PlayerTimeFormatter.format(deltaMs)}"
        } else {
            "-${PlayerTimeFormatter.format(-deltaMs)}"
        }
        val duration = if (PlayerTimeline.hasSeekableDuration(durationMs)) {
            PlayerTimeFormatter.format(durationMs)
        } else {
            "--:--"
        }
        return PlayerGestureHud(
            kind = PlayerGestureHudKind.SEEK,
            primaryText = signedDelta,
            secondaryText = "${PlayerTimeFormatter.format(targetMs)} / $duration"
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
        val step = PlayerDoubleTapSeekPolicy.step(previous, tapSide, intervalMs, nowMs)
        val accumulated = step.nextState.accumulatedMs
        return DoubleTapSeekResult(
            deltaMs = step.deltaMs,
            hud = seek(targetMs = accumulated, durationMs = 0, deltaMs = step.deltaMs).copy(secondaryText = ""),
            nextState = step.nextState,
            isAccumulated = step.isAccumulated
        )
    }
}
