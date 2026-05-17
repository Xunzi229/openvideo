package com.example.openvideo.ui.player

data class PlayerFirstFrameScrimPresentation(
    val visible: Boolean,
    val alpha: Float
)

object PlayerFirstFrameScrimPolicy {
    const val SCRIM_ALPHA = 1f

    fun initialScrimVisible(isAwaitingFirstFrame: Boolean, warmResume: Boolean): Boolean =
        isAwaitingFirstFrame && !warmResume

    fun scrimVisibleOnReattach(isAwaitingFirstFrame: Boolean): Boolean = isAwaitingFirstFrame

    fun presentation(decision: PlayerFirstFrameDecision): PlayerFirstFrameScrimPresentation? =
        when {
            decision.showScrim -> PlayerFirstFrameScrimPresentation(
                visible = true,
                alpha = SCRIM_ALPHA
            )
            decision.hideScrim -> PlayerFirstFrameScrimPresentation(
                visible = false,
                alpha = SCRIM_ALPHA
            )
            else -> null
        }
}
