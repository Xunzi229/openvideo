package com.openvideo.app.ui.player

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import com.openvideo.app.core.prefs.AppPrefs
import com.openvideo.app.core.prefs.PlaybackOrientation
import com.openvideo.app.core.prefs.PlayerPrefs
import com.openvideo.app.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28, 33], application = Application::class)
class PlayerVideoOrientationControllerTest {
    private lateinit var activity: Activity
    private lateinit var appPrefs: AppPrefs
    private lateinit var playerPrefs: PlayerPrefs
    private lateinit var controller: PlayerVideoOrientationController
    private var manuallyOverridden = false

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        appPrefs = AppPrefs(context)
        playerPrefs = PlayerPrefs(context)
        activity = Robolectric.buildActivity(Activity::class.java).get()
        activity.intent = Intent()
        controller = PlayerVideoOrientationController(
            activity = activity,
            playerPrefs = playerPrefs,
            appPrefs = appPrefs,
            userOverrodeOrientationProvider = { manuallyOverridden },
            onUserOverrodeOrientationChanged = { manuallyOverridden = it }
        )
    }

    @Test
    fun preferenceDefaultsAndUnknownValuesRetainExistingBehavior() {
        assertEquals(PlaybackOrientation.DEFAULT, appPrefs.playbackOrientation)
        activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()
            .putString("playback_orientation", "unknown").commit()
        assertEquals(PlaybackOrientation.DEFAULT, appPrefs.playbackOrientation)
    }

    @Test
    fun preferencePersistsAndRestoringDefaultKeepsTheOldAutoOrientationSetting() {
        playerPrefs.autoOrientationByVideo = false
        appPrefs.playbackOrientation = PlaybackOrientation.PORTRAIT
        assertEquals(PlaybackOrientation.PORTRAIT, AppPrefs(activity).playbackOrientation)
        appPrefs.playbackOrientation = PlaybackOrientation.LANDSCAPE
        assertEquals(PlaybackOrientation.LANDSCAPE, AppPrefs(activity).playbackOrientation)
        appPrefs.playbackOrientation = PlaybackOrientation.DEFAULT
        assertEquals(PlaybackOrientation.DEFAULT, AppPrefs(activity).playbackOrientation)
        assertFalse(playerPrefs.autoOrientationByVideo)
    }

    @Test
    fun defaultStartupPreservesLandscapePortraitSquareAndUnknownDimensions() {
        for ((width, height) in listOf(1920 to 1080, 720 to 1280, 1000 to 1000, 0 to 0)) {
            activity.intent.putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, width)
                .putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, height)
            controller.applyInitial()
            assertEquals(
                PlayerOrientationPolicy.initialOrientationForVideo(width, height, true),
                activity.requestedOrientation
            )
        }
        playerPrefs.autoOrientationByVideo = false
        controller.applyInitial()
        assertEquals(PlayerOrientationPolicy.defaultOrientation(), activity.requestedOrientation)
    }

    @Test
    fun portraitStartupOverridesAllDimensionsAndTheOldAutoOrientationSwitch() {
        appPrefs.playbackOrientation = PlaybackOrientation.PORTRAIT
        for (autoOrientation in listOf(true, false)) {
            playerPrefs.autoOrientationByVideo = autoOrientation
            for ((width, height) in listOf(1920 to 1080, 720 to 1280, 1000 to 1000, 0 to 0)) {
                activity.intent.putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, width)
                    .putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, height)
                controller.applyInitial()
                assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
            }
        }
    }

    @Test
    fun lateVideoSizeAndRotationCallbacksCannotUndoPortraitPreference() {
        appPrefs.playbackOrientation = PlaybackOrientation.PORTRAIT
        controller.applyInitial()
        controller.apply(1920, 1080)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
        controller.apply(720, 1280, pixelWidthHeightRatio = 2f, unappliedRotationDegrees = 90)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    fun landscapeStartupOverridesAllDimensionsAndTheOldAutoOrientationSwitch() {
        appPrefs.playbackOrientation = PlaybackOrientation.LANDSCAPE
        for (autoOrientation in listOf(true, false)) {
            playerPrefs.autoOrientationByVideo = autoOrientation
            for ((width, height) in listOf(1920 to 1080, 720 to 1280, 1000 to 1000, 0 to 0)) {
                activity.intent.putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, width)
                    .putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, height)
                controller.applyInitial()
                assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
            }
        }
    }

    @Test
    fun portraitSizeAndRotationCallbacksCannotUndoLandscapePreference() {
        appPrefs.playbackOrientation = PlaybackOrientation.LANDSCAPE
        controller.applyInitial()
        controller.apply(720, 1280)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
        controller.apply(1920, 1080, unappliedRotationDegrees = 90)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
    }

    @Test
    fun manualPortraitSurvivesCallbacksButTheNextVideoStartsLandscape() {
        appPrefs.playbackOrientation = PlaybackOrientation.LANDSCAPE
        controller.applyInitial()
        manuallyOverridden = true
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        controller.apply(720, 1280)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
        controller.preApplyForItem(item(0, 0))
        assertFalse(manuallyOverridden)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
        controller.preApplyForItem(item(720, 1280))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
    }

    @Test
    fun restoringDefaultAfterLandscapeAllowsPortraitVideosToRotateNormally() {
        appPrefs.playbackOrientation = PlaybackOrientation.LANDSCAPE
        controller.applyInitial()
        appPrefs.playbackOrientation = PlaybackOrientation.DEFAULT
        controller.apply(720, 1280)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    fun manualFullscreenSurvivesCallbacksButTheNextVideoStartsPortrait() {
        appPrefs.playbackOrientation = PlaybackOrientation.PORTRAIT
        controller.applyInitial()
        manuallyOverridden = true
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        controller.apply(1920, 1080)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)

        // Unknown dimensions must also reset the previous video's manual fullscreen choice.
        controller.preApplyForItem(item(0, 0))
        assertFalse(manuallyOverridden)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
        controller.preApplyForItem(item(1920, 1080))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    fun defaultCallbacksKeepRotationPixelAspectAndManualOverrideBehavior() {
        controller.apply(1920, 1080, unappliedRotationDegrees = 90)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
        controller.apply(720, 1280, pixelWidthHeightRatio = 3f)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
        manuallyOverridden = true
        controller.apply(720, 1280)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
        controller.preApplyForItem(item(720, 1280))
        assertFalse(manuallyOverridden)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    fun restoringDefaultRestoresAutomaticOrientationAndDisabledAutoKeepsItsBehavior() {
        appPrefs.playbackOrientation = PlaybackOrientation.PORTRAIT
        controller.applyInitial()
        appPrefs.playbackOrientation = PlaybackOrientation.DEFAULT
        controller.apply(1920, 1080)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)

        playerPrefs.autoOrientationByVideo = false
        controller.apply(720, 1280)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
        controller.apply(0, 0)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
    }

    private fun item(width: Int, height: Int) = VideoItem(
        id = 1L, title = "Test", path = "test.mp4", uri = Uri.parse("content://test/video/1"),
        duration = 1000L, size = 100L, width = width, height = height, dateAdded = 0L,
        thumbnailUri = null
    )
}
