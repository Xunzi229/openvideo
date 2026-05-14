package com.example.openvideo.ui.player

data class PlayerFirstFrameDecision(
    val showScrim: Boolean = false,
    val hideScrim: Boolean = false,
    val nextAwaitingFirstFrame: Boolean
)

object PlayerFirstFramePolicy {
    fun onShowForNewMedia(): PlayerFirstFrameDecision =
        PlayerFirstFrameDecision(
            showScrim = true,
            nextAwaitingFirstFrame = true
        )

    fun onRenderedFirstFrame(isAwaitingFirstFrame: Boolean): PlayerFirstFrameDecision =
        PlayerFirstFrameDecision(
            hideScrim = true,
            nextAwaitingFirstFrame = false
        )

    fun onReady(
        isAwaitingFirstFrame: Boolean,
        hasVideoTrack: Boolean
    ): PlayerFirstFrameDecision =
        when {
            !isAwaitingFirstFrame -> PlayerFirstFrameDecision(nextAwaitingFirstFrame = false)
            hasVideoTrack -> PlayerFirstFrameDecision(nextAwaitingFirstFrame = true)
            else -> PlayerFirstFrameDecision(
                hideScrim = true,
                nextAwaitingFirstFrame = false
            )
        }
}
