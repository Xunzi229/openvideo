package com.example.openvideo.ui.player

import android.os.Build
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.prefs.GestureAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerActivityRemainingPoliciesTest {

    @Test
    fun sessionResumeAndDecodeMode() {
        assertTrue(PlayerSessionResumePolicy.shouldRestorePlaybackPosition(true))
        assertEquals(DecodeMode.SOFT, PlayerDecodeModePolicy.decodeMode(true))
        assertEquals(DecodeMode.HARD, PlayerDecodeModePolicy.decodeMode(false))
    }

    @Test
    fun chromeSettingsOverlayAndScreenLock() {
        assertTrue(PlayerChromeSettingsOverlayPolicy.suppressesControlAutoHide(true))
        assertEquals(0.85f, PlayerChromeSettingsOverlayPolicy.restoreContainerAlpha(true, 0.85f), 0.001f)
        assertEquals(0.85f, PlayerScreenLockChromePolicy.revealChrome(0.85f).alpha, 0.001f)
    }

    @Test
    fun gestureDispatchAndNotificationHelpers() {
        var seekCalled = false
        PlayerGestureDispatchPolicy.onHorizontalSwipe(GestureAction.SEEK) { seekCalled = true }
        assertTrue(seekCalled)
        assertTrue(
            PlayerNotificationRefreshPolicy.shouldRefreshBackgroundPlayback(
                isFinishing = false,
                backgroundAudio = true,
                notificationEnabled = true,
                isActivityForeground = false
            )
        )
        assertTrue(PlayerNotificationPermissionPolicy.requiresRuntimePermission(Build.VERSION_CODES.TIRAMISU))
        assertTrue(PlayerPipCompatPolicy.isInPictureInPictureMode(Build.VERSION_CODES.O, true))
    }

    @Test
    fun subtitleAndFirstFrameHelpers() {
        assertEquals("line", PlayerSubtitlePresentationPolicy.resolveSubtitleText(true, "line"))
        assertEquals("", PlayerSubtitlePresentationPolicy.resolveSubtitleText(false, "line"))
        assertTrue(PlayerFirstFrameScrimPolicy.initialScrimVisible(isAwaitingFirstFrame = true, warmResume = false))
        assertFalse(PlayerFirstFrameScrimPolicy.initialScrimVisible(isAwaitingFirstFrame = true, warmResume = true))
    }

    @Test
    fun abLoopAndLockButtonStyle() {
        assertTrue(PlayerAbLoopButtonStylePolicy.shouldHighlight(PlayerAbLoopEvent.POINT_A_SET))
        assertTrue(PlayerAbLoopButtonStylePolicy.shouldClearHighlight(PlayerAbLoopEvent.CANCELLED))
        assertTrue(PlayerLockButtonStylePolicy.shouldUseAccentTint(true))
    }
}
