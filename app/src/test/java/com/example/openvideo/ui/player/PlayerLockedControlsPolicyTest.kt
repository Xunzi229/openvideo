package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLockedControlsPolicyTest {

    @Test
    fun lockedControlsOnlyExposeLockButton() {
        assertEquals(
            ControlVisibility(
                chromeVisible = false,
                lockButtonVisible = true,
                lockButtonSelected = true,
                fullscreenButtonVisible = false
            ),
            PlayerLockedControlsPolicy.visibility(isLocked = true, controlsVisible = true)
        )
    }

    @Test
    fun unlockedControlsExposeFullChromeWhenVisible() {
        assertEquals(
            ControlVisibility(
                chromeVisible = true,
                lockButtonVisible = true,
                lockButtonSelected = false,
                fullscreenButtonVisible = true
            ),
            PlayerLockedControlsPolicy.visibility(isLocked = false, controlsVisible = true)
        )
    }

    @Test
    fun allChromeRegionsHiddenWhenLocked() {
        PlayerChromeRegion.entries.forEach { region ->
            assertFalse(
                "Region $region must stay hidden while locked.",
                PlayerLockedControlsPolicy.isChromeRegionVisible(
                    region = region,
                    isLocked = true,
                    controlsVisible = true
                )
            )
        }
    }

    @Test
    fun chromeRegionsFollowVisibilityWhenUnlocked() {
        PlayerChromeRegion.entries.forEach { region ->
            assertTrue(
                PlayerLockedControlsPolicy.isChromeRegionVisible(
                    region = region,
                    isLocked = false,
                    controlsVisible = true
                )
            )
            assertFalse(
                PlayerLockedControlsPolicy.isChromeRegionVisible(
                    region = region,
                    isLocked = false,
                    controlsVisible = false
                )
            )
        }
    }

    @Test
    fun lockedModeOnlyAllowsLockAndRevealInteractions() {
        assertTrue(PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.LOCK_TOGGLE, isLocked = true))
        assertTrue(PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.REVEAL_LOCKED_CHROME, isLocked = true))
        assertFalse(PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.CHROME_TOGGLE, isLocked = true))
        assertFalse(PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.TRANSPORT, isLocked = true))
        assertFalse(PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SETTINGS, isLocked = true))
        assertFalse(PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SEEK_BAR, isLocked = true))
        assertFalse(PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.BACK, isLocked = true))
        assertFalse(PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.GESTURE_PLAYBACK, isLocked = true))
    }

    @Test
    fun lockedModeHidesFloatingFullscreenButton() {
        assertFalse(
            PlayerLockedControlsPolicy.visibility(isLocked = true, controlsVisible = true).fullscreenButtonVisible
        )
        assertTrue(
            PlayerLockedControlsPolicy.visibility(isLocked = false, controlsVisible = true).fullscreenButtonVisible
        )
    }

    @Test
    fun unlockedModeAllowsAllInteractions() {
        PlayerLockedInteraction.entries.forEach { interaction ->
            assertTrue(
                "Interaction $interaction must be allowed when unlocked.",
                PlayerLockedControlsPolicy.allows(interaction, isLocked = false)
            )
        }
    }
}
