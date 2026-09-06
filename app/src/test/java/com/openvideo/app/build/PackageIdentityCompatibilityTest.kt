package com.openvideo.app.build

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import com.openvideo.app.BuildConfig
import com.openvideo.app.R
import com.openvideo.app.core.player.PlaybackService
import com.openvideo.app.core.ui.LibrarySwipeFrameLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PackageIdentityCompatibilityTest {
    private val applicationId = "com.example.openvideo"
    private val namespace = "com.openvideo.app"
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun codeNamespaceChangesWithoutChangingInstalledApplicationIdentity() {
        assertEquals(applicationId, BuildConfig.APPLICATION_ID)
        assertEquals(applicationId, context.packageName)
        assertEquals(namespace, R::class.java.`package`!!.name)
        assertEquals(applicationId, context.resources.getResourcePackageName(R.string.app_name))
    }

    @Test fun phoneAndTvLaunchersKeepThePublishedComponentName() {
        for (category in listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(applicationId)
            val activities = context.packageManager.queryIntentActivities(intent, 0)
            assertEquals(category, 1, activities.size)
            val activity = activities.single().activityInfo
            assertEquals("$applicationId.ui.MainActivity", activity.name)
            assertEquals("$namespace.ui.MainActivity", activity.targetActivity)
            assertTrue(activity.exported)
        }
    }

    @Test fun sharedAndViewedUrlsResolveToTheCompatibleEntryPoint() {
        val intents = listOf(Intent(Intent.ACTION_SEND).setType("text/plain")) +
            listOf("http", "https", "rtsp").map { Intent(Intent.ACTION_VIEW, Uri.parse("$it://example.com/video.mp4")) }
        for (intent in intents) {
            val activities = context.packageManager.queryIntentActivities(intent.setPackage(applicationId), PackageManager.MATCH_DEFAULT_ONLY)
            assertEquals(intent.toString(), 1, activities.size)
            assertEquals("$namespace.ui.MainActivity", activities.single().activityInfo.targetActivity)
        }
    }

    @Test fun oldActivityNamesResolveToNewClassesWithoutBecomingExported() {
        val suffixes = listOf("ui.player.PlayerActivity", "ui.player.CompatibilityPlayerActivity",
            "ui.player.PlayerDisplaySettingsActivity", "ui.player.PlayerPlaybackSettingsActivity",
            "ui.player.PlayerAudioSettingsActivity", "ui.player.PlayerSubtitleSettingsActivity",
            "ui.player.PlayerGestureSettingsActivity", "ui.settings.NotificationSettingsActivity")
        for (suffix in suffixes) {
            val info = context.packageManager.getActivityInfo(ComponentName(applicationId, "$applicationId.$suffix"), 0)
            assertEquals("$namespace.$suffix", info.targetActivity)
            assertFalse(info.exported)
            assertNotNull(Class.forName(info.targetActivity))
        }
    }

    @Test fun playbackActionsRemainCompatibleWithThePublishedProtocol() {
        assertEquals(listOf("START_PLAYBACK_SERVICE", "REFRESH_PLAYBACK_NOTIFICATION", "TOGGLE_PLAY_PAUSE",
            "SKIP_TO_NEXT", "SKIP_TO_PREVIOUS", "SEEK_TO_MS", "DISMISS_PLAYBACK_NOTIFICATION")
            .map { "$applicationId.action.$it" },
            listOf(PlaybackService.ACTION_START, PlaybackService.ACTION_REFRESH, PlaybackService.ACTION_TOGGLE_PLAY_PAUSE,
                PlaybackService.ACTION_SKIP_TO_NEXT, PlaybackService.ACTION_SKIP_TO_PREVIOUS,
                PlaybackService.ACTION_SEEK_TO_MS, PlaybackService.ACTION_DISMISS))
    }

    @Test fun mainLayoutInflatesTheRenamedCustomView() {
        val themedContext = ContextThemeWrapper(context, R.style.Theme_OpenVideo)
        val root = LayoutInflater.from(themedContext).inflate(R.layout.activity_main, null)
        assertTrue(root.findViewById<android.view.View>(R.id.phone_tab_host) is LibrarySwipeFrameLayout)
    }
}
