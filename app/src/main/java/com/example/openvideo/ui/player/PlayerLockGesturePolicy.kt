package com.example.openvideo.ui.player

enum class PlayerTouchAction {
    DOWN,
    MOVE,
    UP,
    CANCEL,
    OTHER
}

data class PlayerLockTouchDecision(
    val consumeTouch: Boolean,
    val revealLockedControls: Boolean
)

data class PlayerLockBackDecision(
    val finishPlayer: Boolean,
    val revealLockedControls: Boolean
)

object PlayerLockGesturePolicy {
    fun onTouch(isLocked: Boolean, action: PlayerTouchAction): PlayerLockTouchDecision {
        if (!isLocked) {
            return PlayerLockTouchDecision(
                consumeTouch = false,
                revealLockedControls = false
            )
        }
        return PlayerLockTouchDecision(
            consumeTouch = true,
            revealLockedControls = action == PlayerTouchAction.UP
        )
    }

    fun onBackPressed(isLocked: Boolean): PlayerLockBackDecision =
        if (isLocked) {
            PlayerLockBackDecision(finishPlayer = false, revealLockedControls = true)
        } else {
            PlayerLockBackDecision(finishPlayer = true, revealLockedControls = false)
        }
}
