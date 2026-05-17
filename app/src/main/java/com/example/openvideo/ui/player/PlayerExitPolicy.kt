package com.example.openvideo.ui.player

data class PlayerExitState(
    val isFinishing: Boolean = false,
    val hasReleased: Boolean = false
)

data class PlayerFinishDecision(
    val shouldFinish: Boolean,
    val nextState: PlayerExitState
)

data class PlayerReleaseDecision(
    val shouldRelease: Boolean,
    val nextState: PlayerExitState
)

enum class PlayerExitTransitionStrategy {
    OVERRIDE_ACTIVITY_TRANSITION,
    OVERRIDE_PENDING_TRANSITION
}

data class PlayerExitPresentation(
    val releaseDelayMs: Long,
    val finishDelayMs: Long
)

/** Semantic backdrop when hiding the video surface during exit (Activity maps to `R.color`). */
enum class PlayerExitBackdrop {
    APP_BASE
}

data class PlayerExitFrameDecision(
    val shouldPrepare: Boolean,
    val cancelPlayerViewAnimation: Boolean,
    val hidePlayerView: Boolean,
    val backdrop: PlayerExitBackdrop
)

object PlayerExitPolicy {
    private const val PLAYER_EXIT_RELEASE_DELAY_MS = 250L
    private const val PLAYER_EXIT_FINISH_DELAY_MS = 180L

    fun requestFinish(state: PlayerExitState): PlayerFinishDecision {
        if (state.isFinishing) {
            return PlayerFinishDecision(shouldFinish = false, nextState = state)
        }
        return PlayerFinishDecision(
            shouldFinish = true,
            nextState = state.copy(isFinishing = true)
        )
    }

    fun requestRelease(state: PlayerExitState): PlayerReleaseDecision {
        if (state.hasReleased) {
            return PlayerReleaseDecision(shouldRelease = false, nextState = state)
        }
        return PlayerReleaseDecision(
            shouldRelease = true,
            nextState = state.copy(hasReleased = true)
        )
    }

    fun finishPresentation(): PlayerExitPresentation =
        PlayerExitPresentation(
            releaseDelayMs = PLAYER_EXIT_RELEASE_DELAY_MS,
            finishDelayMs = PLAYER_EXIT_FINISH_DELAY_MS
        )

    fun transitionStrategyFor(sdkInt: Int): PlayerExitTransitionStrategy =
        if (sdkInt >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PlayerExitTransitionStrategy.OVERRIDE_ACTIVITY_TRANSITION
        } else {
            PlayerExitTransitionStrategy.OVERRIDE_PENDING_TRANSITION
        }

    /**
     * Prepares a static app-colored frame before [android.app.Activity.finish] so the
     * TextureView/Surface layer cannot flash black over the previous screen.
     */
    fun exitFrameDecision(playerViewInitialized: Boolean): PlayerExitFrameDecision {
        if (!playerViewInitialized) {
            return PlayerExitFrameDecision(
                shouldPrepare = false,
                cancelPlayerViewAnimation = false,
                hidePlayerView = false,
                backdrop = PlayerExitBackdrop.APP_BASE
            )
        }
        return PlayerExitFrameDecision(
            shouldPrepare = true,
            cancelPlayerViewAnimation = true,
            hidePlayerView = true,
            backdrop = PlayerExitBackdrop.APP_BASE
        )
    }
}
