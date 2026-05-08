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
    fun settingsPanelUsesRightDrawerInLandscape() {
        val bounds = PlayerSettingsLayoutPolicy.panelBounds(screenWidth = 1920, screenHeight = 1080)

        assertEquals(416, bounds.width)
        assertEquals(1074, bounds.height)
        assertEquals(
            android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL,
            PlayerSettingsLayoutPolicy.panelGravity(screenWidth = 1920, screenHeight = 1080)
        )
    }

    @Test
    fun landscapeSettingsPanelWidthComesFromFourColumnGridNeeds() {
        assertEquals(
            416,
            PlayerSettingsLayoutPolicy.landscapeContentWidth(
                columnCount = 4,
                cellWidthDp = 104,
                columnGapDp = 0.1,
                horizontalPaddingDp = 0.3,
                density = 1f
            )
        )

        assertEquals(
            416,
            PlayerSettingsLayoutPolicy.panelBounds(screenWidth = 2560, screenHeight = 1440).width
        )

        assertEquals(
            416,
            PlayerSettingsLayoutPolicy.panelBounds(screenWidth = 760, screenHeight = 500).width
        )
    }

    @Test
    fun landscapeSettingsPanelWidthScalesDpFormulaWithScreenDensity() {
        val bounds = PlayerSettingsLayoutPolicy.panelBounds(
            screenWidth = 1920,
            screenHeight = 1080,
            density = 2f
        )

        assertEquals(832, bounds.width)
        assertEquals(1068, bounds.height)
        assertEquals(
            1568,
            PlayerSettingsLayoutPolicy.landscapeContentWidth(
                columnCount = 4,
                cellWidthDp = 160,
                columnGapDp = 32,
                horizontalPaddingDp = 24,
                density = 2f
            )
        )
    }

    @Test
    fun landscapeSettingsPanelWidthAcceptsFractionalDpSpacing() {
        assertEquals(
            640,
            PlayerSettingsLayoutPolicy.landscapeContentWidth(
                columnCount = 4,
                cellWidthDp = 160,
                columnGapDp = 0.1,
                horizontalPaddingDp = 0.3,
                density = 1f
            )
        )

        assertEquals(
            416,
            PlayerSettingsLayoutPolicy.panelBounds(screenWidth = 1920, screenHeight = 1080).width
        )
    }

    @Test
    fun settingsPanelUsesBottomSheetInPortrait() {
        val bounds = PlayerSettingsLayoutPolicy.panelBounds(screenWidth = 1080, screenHeight = 1920)

        assertEquals(1080, bounds.width)
        assertEquals(1152, bounds.height)
        assertEquals(android.view.Gravity.BOTTOM, PlayerSettingsLayoutPolicy.panelGravity(screenWidth = 1080, screenHeight = 1920))
    }

    @Test
    fun settingsNavigationNeedsScrollOnShortLandscapeScreens() {
        assertEquals(
            true,
            PlayerSettingsLayoutPolicy.navigationNeedsScroll(
                availableHeightDp = 280,
                itemCount = 6
            )
        )
    }
}
