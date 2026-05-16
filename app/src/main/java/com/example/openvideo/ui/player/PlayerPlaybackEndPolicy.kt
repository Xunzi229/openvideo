package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.LoopMode

enum class PlayerPlaybackEndAction {
    PLAY_NEXT,
    REPLAY_CURRENT,
    STOP_AT_END,
    RETURN_TO_LIST
}

data class PlayerPlaybackEndDecision(
    val action: PlayerPlaybackEndAction,
    val nextIndex: Int? = null,
    val seekPositionMs: Long? = null
)

object PlayerPlaybackEndPolicy {

    fun decide(
        currentIndex: Int,
        queueSize: Int,
        autoPlayNext: Boolean,
        loopMode: LoopMode,
        abLoopState: PlayerAbLoopState,
        abLoopPointA: Long,
        returnToListWhenDone: Boolean
    ): PlayerPlaybackEndDecision {
        if (abLoopState == PlayerAbLoopState.LOOPING && abLoopPointA >= 0L) {
            return PlayerPlaybackEndDecision(
                action = PlayerPlaybackEndAction.REPLAY_CURRENT,
                seekPositionMs = abLoopPointA
            )
        }

        if (loopMode == LoopMode.SINGLE) {
            return PlayerPlaybackEndDecision(
                action = PlayerPlaybackEndAction.REPLAY_CURRENT,
                seekPositionMs = 0L
            )
        }

        val nextIndex = PlayerQueueLoopPolicy.nextIndexAfterEnded(
            currentIndex = currentIndex,
            queueSize = queueSize,
            autoPlayNext = autoPlayNext,
            loopMode = loopMode
        )
        if (nextIndex != null) {
            return PlayerPlaybackEndDecision(
                action = PlayerPlaybackEndAction.PLAY_NEXT,
                nextIndex = nextIndex
            )
        }

        return PlayerPlaybackEndDecision(
            action = if (returnToListWhenDone) {
                PlayerPlaybackEndAction.RETURN_TO_LIST
            } else {
                PlayerPlaybackEndAction.STOP_AT_END
            }
        )
    }
}
