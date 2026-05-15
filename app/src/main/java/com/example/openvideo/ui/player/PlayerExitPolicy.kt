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
    val releaseDelayMs: Long
)

object PlayerExitPolicy {
    private const val PLAYER_EXIT_RELEASE_DELAY_MS = 250L

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
        PlayerExitPresentation(releaseDelayMs = PLAYER_EXIT_RELEASE_DELAY_MS)

    fun transitionStrategyFor(sdkInt: Int): PlayerExitTransitionStrategy =
        if (sdkInt >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PlayerExitTransitionStrategy.OVERRIDE_ACTIVITY_TRANSITION
        } else {
            PlayerExitTransitionStrategy.OVERRIDE_PENDING_TRANSITION
        }
}
