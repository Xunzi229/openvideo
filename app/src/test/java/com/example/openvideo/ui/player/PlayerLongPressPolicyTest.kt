package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.LongPressAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLongPressPolicyTest {

    @Test
    fun speedActionStartsTemporarySpeedAndKeepsRestoreSpeed() {
        val decision = PlayerLongPressPolicy.onPress(
            action = LongPressAction.SPEED,
            requestedSpeed = 2.5f,
            restoreSpeed = 1.25f
        )

        assertTrue(decision.startLongPress)
        assertEquals(2.5f, decision.targetSpeed!!, 0.001f)
        assertEquals(1.25f, decision.restoreSpeed, 0.001f)
    }

    @Test
    fun noneActionDoesNotStartTemporarySpeed() {
        val decision = PlayerLongPressPolicy.onPress(
            action = LongPressAction.NONE,
            requestedSpeed = 2.0f,
            restoreSpeed = 1.0f
        )

        assertFalse(decision.startLongPress)
        assertNull(decision.targetSpeed)
        assertEquals(1.0f, decision.restoreSpeed, 0.001f)
    }

    @Test
    fun speedActionClampsUnsafePreferenceValues() {
        assertEquals(
            PlayerLongPressPolicy.MIN_SPEED,
            PlayerLongPressPolicy.onPress(LongPressAction.SPEED, 0f, 1.0f).targetSpeed!!,
            0.001f
        )
        assertEquals(
            PlayerLongPressPolicy.MAX_SPEED,
            PlayerLongPressPolicy.onPress(LongPressAction.SPEED, 99f, 1.0f).targetSpeed!!,
            0.001f
        )
        assertEquals(
            PlayerLongPressPolicy.DEFAULT_LONG_PRESS_SPEED,
            PlayerLongPressPolicy.onPress(LongPressAction.SPEED, Float.NaN, 1.0f).targetSpeed!!,
            0.001f
        )
    }

    @Test
    fun releaseOnlyRestoresWhenLongPressWasActive() {
        val restore = PlayerLongPressPolicy.onRelease(isLongPressing = true, restoreSpeed = 1.5f)
        val idle = PlayerLongPressPolicy.onRelease(isLongPressing = false, restoreSpeed = 1.5f)

        assertTrue(restore.shouldRestoreSpeed)
        assertEquals(1.5f, restore.restoreSpeed!!, 0.001f)
        assertFalse(idle.shouldRestoreSpeed)
        assertNull(idle.restoreSpeed)
    }

    @Test
    fun releaseFallsBackToNormalSpeedWhenSavedSpeedIsInvalid() {
        val restore = PlayerLongPressPolicy.onRelease(isLongPressing = true, restoreSpeed = Float.NaN)

        assertTrue(restore.shouldRestoreSpeed)
        assertEquals(PlayerLongPressPolicy.NORMAL_SPEED, restore.restoreSpeed!!, 0.001f)
    }
}
