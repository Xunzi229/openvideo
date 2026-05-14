package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLockGesturePolicyTest {

    @Test
    fun unlockedTouchesAreNotConsumedByLockPolicy() {
        assertEquals(
            PlayerLockTouchDecision(consumeTouch = false, revealLockedControls = false),
            PlayerLockGesturePolicy.onTouch(isLocked = false, action = PlayerTouchAction.UP)
        )
    }

    @Test
    fun lockedTouchesAreConsumedSoPlaybackGesturesCannotLeakThrough() {
        assertEquals(
            PlayerLockTouchDecision(consumeTouch = true, revealLockedControls = false),
            PlayerLockGesturePolicy.onTouch(isLocked = true, action = PlayerTouchAction.DOWN)
        )
        assertEquals(
            PlayerLockTouchDecision(consumeTouch = true, revealLockedControls = false),
            PlayerLockGesturePolicy.onTouch(isLocked = true, action = PlayerTouchAction.MOVE)
        )
    }

    @Test
    fun lockedTouchUpRevealsOnlyLockedControls() {
        assertEquals(
            PlayerLockTouchDecision(consumeTouch = true, revealLockedControls = true),
            PlayerLockGesturePolicy.onTouch(isLocked = true, action = PlayerTouchAction.UP)
        )
    }

    @Test
    fun lockedTouchCancelDoesNotRevealControls() {
        assertEquals(
            PlayerLockTouchDecision(consumeTouch = true, revealLockedControls = false),
            PlayerLockGesturePolicy.onTouch(isLocked = true, action = PlayerTouchAction.CANCEL)
        )
    }

    @Test
    fun unlockedBackPressFinishesPlayer() {
        assertEquals(
            PlayerLockBackDecision(finishPlayer = true, revealLockedControls = false),
            PlayerLockGesturePolicy.onBackPressed(isLocked = false)
        )
    }

    @Test
    fun lockedBackPressOnlyRevealsUnlockControl() {
        assertEquals(
            PlayerLockBackDecision(finishPlayer = false, revealLockedControls = true),
            PlayerLockGesturePolicy.onBackPressed(isLocked = true)
        )
    }
}
