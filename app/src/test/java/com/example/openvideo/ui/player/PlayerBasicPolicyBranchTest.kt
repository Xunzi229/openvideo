package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.GestureAction
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerBasicPolicyBranchTest {
    @Test
    fun notificationRefreshRequiresEveryBackgroundPlaybackCondition() {
        for (mask in 0 until 16) {
            val finishing = mask and 1 != 0
            val audio = mask and 2 != 0
            val notification = mask and 4 != 0
            val foreground = mask and 8 != 0
            assertEquals(
                "state=$mask", mask == 6,
                PlayerNotificationRefreshPolicy.shouldRefreshBackgroundPlayback(finishing, audio, notification, foreground)
            )
        }
    }

    @Test
    fun horizontalSwipeOnlyDispatchesSeekForSeekAction() {
        for (action in GestureAction.entries) {
            var calls = 0
            PlayerGestureDispatchPolicy.onHorizontalSwipe(action) { calls++ }
            assertEquals(action.name, if (action == GestureAction.SEEK) 1 else 0, calls)
        }
    }

    @Test
    fun overlayRestoresPreviousChromeVisibilityAndAlpha() {
        for (visible in listOf(false, true)) {
            assertEquals(visible, PlayerChromeSettingsOverlayPolicy.suppressesControlAutoHide(visible))
            assertEquals(visible, PlayerChromeSettingsOverlayPolicy.hidesAllChromeRegions(visible))
            assertEquals(visible, PlayerChromeSettingsOverlayPolicy.restoreContainerVisible(visible))
            assertEquals(if (visible) 0.6f else 0f, PlayerChromeSettingsOverlayPolicy.restoreContainerAlpha(visible, 0.6f))
        }
    }

    @Test
    fun appliesBothStatesOfPlaybackPreferenceSwitches() {
        for (enabled in listOf(false, true)) {
            assertEquals(enabled, PlayerSessionResumePolicy.shouldRestorePlaybackPosition(enabled))
            assertEquals(enabled, PlayerScreenOnPolicy.shouldKeepScreenOn(enabled))
            assertEquals(enabled, PlayerVolumeBoostApplyPolicy.shouldReapplyOnAudioSessionChange(enabled))
            assertEquals(enabled, PlayerLockButtonStylePolicy.shouldUseAccentTint(enabled))
        }
    }

    @Test
    fun abLoopButtonsShowSetAndLoopingButClearInvalidOrCancelledPoints() {
        val states = mapOf(
            PlayerAbLoopEvent.POINT_A_SET to true,
            PlayerAbLoopEvent.LOOP_STARTED to true,
            PlayerAbLoopEvent.INVALID_POINT_B to false,
            PlayerAbLoopEvent.CANCELLED to false
        )
        states.forEach { (event, highlight) ->
            assertEquals(event.name, highlight, PlayerAbLoopButtonStylePolicy.shouldHighlight(event))
            assertEquals(event.name, !highlight, PlayerAbLoopButtonStylePolicy.shouldClearHighlight(event))
        }
    }
}
