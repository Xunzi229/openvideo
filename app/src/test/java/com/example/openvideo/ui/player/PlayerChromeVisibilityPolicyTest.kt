package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerChromeVisibilityPolicyTest {

    @Test
    fun showControlsMakesChromeVisibleAndSchedulesAutoHide() {
        val presentation = PlayerChromeVisibilityPolicy.show(
            controlsOpacityPercent = 85,
            autoHideSeconds = 3
        )

        assertEquals(true, presentation.controlsVisible)
        assertEquals(true, presentation.containerVisible)
        assertEquals(0.85f, presentation.alpha, 0.001f)
        assertEquals(3_000L, presentation.hideDelayMs)
    }

    @Test
    fun showControlsDoesNotScheduleAutoHideWhenDisabled() {
        val presentation = PlayerChromeVisibilityPolicy.show(
            controlsOpacityPercent = 85,
            autoHideSeconds = 0
        )

        assertNull(presentation.hideDelayMs)
    }

    @Test
    fun hideControlsKeepsStateInvisibleWithoutScheduling() {
        val presentation = PlayerChromeVisibilityPolicy.hide()

        assertEquals(false, presentation.controlsVisible)
        assertEquals(false, presentation.containerVisible)
        assertEquals(0f, presentation.alpha, 0.001f)
        assertNull(presentation.hideDelayMs)
    }

    @Test
    fun lockedRevealUsesShortLockDelay() {
        val presentation = PlayerChromeVisibilityPolicy.showLocked(controlsOpacityPercent = 120)

        assertEquals(true, presentation.controlsVisible)
        assertEquals(true, presentation.containerVisible)
        assertEquals(1f, presentation.alpha, 0.001f)
        assertEquals(PlayerChromePolicy.lockedControlsHideDelayMs(), presentation.hideDelayMs)
    }

    @Test
    fun pictureInPictureHidesChromeWithoutScheduling() {
        val presentation = PlayerChromeVisibilityPolicy.pictureInPicture()

        assertEquals(false, presentation.controlsVisible)
        assertEquals(false, presentation.containerVisible)
        assertEquals(0f, presentation.alpha, 0.001f)
        assertNull(presentation.hideDelayMs)
    }
}
