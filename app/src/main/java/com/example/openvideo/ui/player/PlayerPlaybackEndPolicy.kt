package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.LoopMode
import com.example.openvideo.core.prefs.PlaybackEndBehavior

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
        endBehavior: PlaybackEndBehavior
    ): PlayerPlaybackEndDecision {
        if (abLoopState == PlayerAbLoopState.LOOPING && abLoopPointA >= 0L) {
            return PlayerPlaybackEndDecision(
                action = PlayerPlaybackEndAction.REPLAY_CURRENT,
                seekPositionMs = abLoopPointA
            )
        }

        return when (endBehavior) {
            PlaybackEndBehavior.REPLAY -> PlayerPlaybackEndDecision(
                action = PlayerPlaybackEndAction.REPLAY_CURRENT,
                seekPositionMs = 0L
            )
            PlaybackEndBehavior.STOP -> PlayerPlaybackEndDecision(
                action = PlayerPlaybackEndAction.STOP_AT_END
            )
            PlaybackEndBehavior.RETURN_TO_LIST -> PlayerPlaybackEndDecision(
                action = PlayerPlaybackEndAction.RETURN_TO_LIST
            )
            PlaybackEndBehavior.PLAY_NEXT -> decisionForPlayNext(currentIndex, queueSize)
            PlaybackEndBehavior.FOLLOW_SETTINGS -> decideFromPlaybackSettings(
                currentIndex = currentIndex,
                queueSize = queueSize,
                autoPlayNext = autoPlayNext,
                loopMode = loopMode
            )
        }
    }

    private fun decideFromPlaybackSettings(
        currentIndex: Int,
        queueSize: Int,
        autoPlayNext: Boolean,
        loopMode: LoopMode
    ): PlayerPlaybackEndDecision {
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

        return PlayerPlaybackEndDecision(action = PlayerPlaybackEndAction.STOP_AT_END)
    }

    private fun decisionForPlayNext(
        currentIndex: Int,
        queueSize: Int
    ): PlayerPlaybackEndDecision {
        val nextIndex = PlayerQueueLoopPolicy.nextIndexAfterEnded(
            currentIndex = currentIndex,
            queueSize = queueSize,
            autoPlayNext = true,
            loopMode = LoopMode.LIST
        )
        if (nextIndex != null) {
            return PlayerPlaybackEndDecision(
                action = PlayerPlaybackEndAction.PLAY_NEXT,
                nextIndex = nextIndex
            )
        }
        return PlayerPlaybackEndDecision(action = PlayerPlaybackEndAction.STOP_AT_END)
    }
}
