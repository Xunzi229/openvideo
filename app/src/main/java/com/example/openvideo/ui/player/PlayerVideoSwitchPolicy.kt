package com.example.openvideo.ui.player

data class PlayerVideoSwitchReset(
    val hasSkippedIntro: Boolean,
    val hasSkippedOutro: Boolean,
    val abLoopState: PlayerAbLoopState,
    val abLoopPointA: Long,
    val abLoopPointB: Long,
    val pendingSeekTarget: Long?,
    val seekGestureAnchorPositionMs: Long?,
    val doubleTapSeekState: DoubleTapSeekState?,
    val doubleTapSeekAnchorPositionMs: Long?,
    val keepGestureHudAfterActionUp: Boolean,
    val awaitFirstFrame: Boolean,
    val manualVideoZoom: PlayerVideoZoomState
)

object PlayerVideoSwitchPolicy {

    fun resetForNewVideo(): PlayerVideoSwitchReset =
        PlayerVideoSwitchReset(
            hasSkippedIntro = false,
            hasSkippedOutro = false,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1,
            abLoopPointB = -1,
            pendingSeekTarget = null,
            seekGestureAnchorPositionMs = null,
            doubleTapSeekState = null,
            doubleTapSeekAnchorPositionMs = null,
            keepGestureHudAfterActionUp = false,
            awaitFirstFrame = true,
            manualVideoZoom = PlayerVideoZoomState.IDENTITY
        )
}
