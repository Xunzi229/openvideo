package com.example.openvideo.ui.player

import android.content.pm.ActivityInfo
import com.example.openvideo.R
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerControlStateTest {

    @Test
    fun playingStateUsesPauseIcon() {
        assertEquals(R.drawable.ic_pause, PlayerControlState.playPauseIcon(isPlayingOrRequested = true))
    }

    @Test
    fun pausedStateUsesPlayIcon() {
        assertEquals(R.drawable.ic_play, PlayerControlState.playPauseIcon(isPlayingOrRequested = false))
    }

    @Test
    fun lockedControlsOnlyExposeLockButton() {
        assertEquals(
            ControlVisibility(
                chromeVisible = false,
                lockButtonVisible = true,
                lockButtonSelected = true
            ),
            PlayerControlState.visibilityFor(isLocked = true, controlsVisible = true)
        )
    }

    @Test
    fun unlockedControlsExposeFullChromeWhenVisible() {
        assertEquals(
            ControlVisibility(
                chromeVisible = true,
                lockButtonVisible = true,
                lockButtonSelected = false
            ),
            PlayerControlState.visibilityFor(isLocked = false, controlsVisible = true)
        )
    }

    @Test
    fun playerStartsInLandscape() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, PlayerOrientationPolicy.defaultOrientation())
    }

    @Test
    fun lockButtonFloatsOnLeftCenterEdge() {
        assertEquals(
            LockButtonPlacement(
                startMarginDp = 18,
                sizeDp = 52,
                constrainToVerticalCenter = true
            ),
            PlayerControlState.lockButtonPlacement()
        )
    }

    @Test
    fun settingsDialogUsesLandscapeFriendlyBounds() {
        val bounds = PlayerSettingsLayoutPolicy.dialogBounds(screenWidth = 1920, screenHeight = 1080)

        assertEquals(1440, bounds.width)
        assertEquals(756, bounds.height)
    }
}
