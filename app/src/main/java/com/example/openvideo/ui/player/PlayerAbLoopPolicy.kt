package com.example.openvideo.ui.player

enum class PlayerAbLoopState { IDLE, POINT_A_SET, LOOPING }

enum class PlayerAbLoopEvent {
    POINT_A_SET,
    LOOP_STARTED,
    INVALID_POINT_B,
    CANCELLED
}

data class PlayerAbLoopResult(
    val state: PlayerAbLoopState,
    val pointA: Long,
    val pointB: Long,
    val event: PlayerAbLoopEvent
)

object PlayerAbLoopPolicy {

    fun onToggle(
        state: PlayerAbLoopState,
        pointA: Long,
        pointB: Long,
        currentPositionMs: Long
    ): PlayerAbLoopResult {
        return when (state) {
            PlayerAbLoopState.IDLE -> PlayerAbLoopResult(
                state = PlayerAbLoopState.POINT_A_SET,
                pointA = currentPositionMs.coerceAtLeast(0),
                pointB = -1,
                event = PlayerAbLoopEvent.POINT_A_SET
            )
            PlayerAbLoopState.POINT_A_SET -> {
                if (currentPositionMs > pointA) {
                    PlayerAbLoopResult(
                        state = PlayerAbLoopState.LOOPING,
                        pointA = pointA,
                        pointB = currentPositionMs,
                        event = PlayerAbLoopEvent.LOOP_STARTED
                    )
                } else {
                    PlayerAbLoopResult(
                        state = PlayerAbLoopState.IDLE,
                        pointA = -1,
                        pointB = -1,
                        event = PlayerAbLoopEvent.INVALID_POINT_B
                    )
                }
            }
            PlayerAbLoopState.LOOPING -> PlayerAbLoopResult(
                state = PlayerAbLoopState.IDLE,
                pointA = -1,
                pointB = -1,
                event = PlayerAbLoopEvent.CANCELLED
            )
        }
    }

    fun loopSeekTarget(
        state: PlayerAbLoopState,
        pointA: Long,
        pointB: Long,
        currentPositionMs: Long
    ): Long? {
        if (state != PlayerAbLoopState.LOOPING) return null
        if (pointA < 0 || pointB <= pointA) return null
        return pointA.takeIf { currentPositionMs >= pointB }
    }
}
