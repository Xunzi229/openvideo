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

object PlayerExitPolicy {
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
}
