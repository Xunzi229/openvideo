package com.example.openvideo.ui.player

data class DoubleTapSeekPreview(
    val deltaMs: Long,
    val targetMs: Long,
    val seekable: Boolean,
    val isAccumulated: Boolean,
    val anchorPositionMs: Long,
    val nextState: DoubleTapSeekState,
    val timelineFraction: Float?
)

data class DoubleTapSeekStep(
    val deltaMs: Long,
    val nextState: DoubleTapSeekState,
    val isAccumulated: Boolean
)

object PlayerDoubleTapSeekPolicy {
    private const val DOUBLE_TAP_ACCUMULATE_TIMEOUT_MS = 650L

    fun step(
        previous: DoubleTapSeekState?,
        tapSide: PlayerSwipeSide,
        intervalMs: Long,
        nowMs: Long
    ): DoubleTapSeekStep {
        val direction = when (tapSide) {
            PlayerSwipeSide.LEFT -> -1
            PlayerSwipeSide.RIGHT -> 1
            PlayerSwipeSide.NONE -> 0
        }
        val stepMs = intervalMs.coerceAtLeast(0)
        val canAccumulate = previous != null &&
            direction != 0 &&
            previous.side == tapSide &&
            nowMs - previous.lastTapUptimeMs <= DOUBLE_TAP_ACCUMULATE_TIMEOUT_MS
        val accumulatedMs = when {
            direction == 0 -> 0
            canAccumulate -> previous!!.accumulatedMs + stepMs
            else -> stepMs
        }
        return DoubleTapSeekStep(
            deltaMs = accumulatedMs * direction,
            nextState = DoubleTapSeekState(
                side = tapSide,
                accumulatedMs = accumulatedMs,
                lastTapUptimeMs = nowMs
            ),
            isAccumulated = canAccumulate
        )
    }

    fun preview(
        previous: DoubleTapSeekState?,
        tapSide: PlayerSwipeSide,
        intervalMs: Long,
        nowMs: Long,
        anchorPositionMs: Long?,
        currentPositionMs: Long,
        durationMs: Long
    ): DoubleTapSeekPreview {
        val step = step(previous, tapSide, intervalMs, nowMs)
        val anchor = if (step.isAccumulated) anchorPositionMs ?: currentPositionMs else currentPositionMs
        val target = PlayerTimeline.safeSeekTarget(anchor, step.deltaMs, durationMs)
        val seekable = PlayerTimeline.hasSeekableDuration(durationMs)
        return DoubleTapSeekPreview(
            deltaMs = step.deltaMs,
            targetMs = target,
            seekable = seekable,
            isAccumulated = step.isAccumulated,
            anchorPositionMs = anchor,
            nextState = step.nextState,
            timelineFraction = if (seekable) {
                (target.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
        )
    }
}
