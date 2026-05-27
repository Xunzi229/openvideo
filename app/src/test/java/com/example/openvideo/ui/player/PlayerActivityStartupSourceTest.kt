package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerActivityStartupSourceTest {

    @Test
    fun controlsAreAttachedAfterPlayerIsInitialized() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val onCreate = source.substringAfter("override fun onCreate(savedInstanceState: Bundle?) {")
            .substringBefore("\n    private fun applyPlayerSettings()")

        assertTrue(
            "setupControls should run after initialize so player listeners attach to the active player",
            onCreate.indexOf("viewModel.initialize(") < onCreate.indexOf("setupControls()")
        )
    }

    @Test
    fun playerKeepsBlackScrimUntilFirstFrameWhenOpeningOrSwitchingVideo() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val firstFrameController = String(Files.readAllBytes(playerFirstFrameControllerSource()))
        val eventController = String(Files.readAllBytes(playerEventControllerSource()))

        assertTrue(source.contains("private lateinit var firstFrameScrim: View"))
        assertTrue(source.contains("R.id.player_first_frame_scrim"))
        assertTrue(source.contains("firstFrames.showForNewMedia()"))
        assertTrue(firstFrameController.contains("private fun applyDecision(decision: PlayerFirstFrameDecision)"))
        assertTrue(firstFrameController.contains("PlayerFirstFramePolicy.onShowForNewMedia()"))
        assertTrue(firstFrameController.contains("PlayerFirstFramePolicy.onRenderedFirstFrame("))
        assertTrue(firstFrameController.contains("PlayerFirstFramePolicy.onReady("))
        assertTrue(eventController.contains("override fun onRenderedFirstFrame()"))
        assertTrue(source.contains("onRenderedFirstFrame = { firstFrames.onRenderedFirstFrame() }"))
        assertTrue(source.contains("firstFrames.showForNewMedia()") && source.contains("viewModel.switchToVideo("))

        sequenceOf(playerLayoutSource("layout"), playerLayoutSource("layout-land")).forEach { layout ->
            val layoutSource = String(Files.readAllBytes(layout))
            assertTrue(layoutSource.contains("android:id=\"@+id/player_first_frame_scrim\""))
            assertTrue(layoutSource.contains("android:background=\"@color/player_bg\""))
            assertTrue(
                "Scrim should sit above PlayerView and below gesture/controls to cover TextureView first-frame flashes",
                layoutSource.indexOf("@+id/player_view") < layoutSource.indexOf("@+id/player_first_frame_scrim") &&
                    layoutSource.indexOf("@+id/player_first_frame_scrim") < layoutSource.indexOf("@+id/gesture_overlay")
            )
        }
    }

    @Test
    fun playerResetsProgressSaveCursorWhenSwitchingVideo() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(source.contains("PlaybackProgressPolicy.onPositionTick("))
        assertTrue(source.contains("lastHistorySavedPositionMs = decision.nextLastSavedPositionMs"))
        assertTrue(source.contains("lastHistorySavedPositionMs = PlaybackProgressPolicy.onNewMedia()"))
    }

    @Test
    fun playerUsesSubtitlePresentationPolicyDuringPositionTicks() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(source.contains("private fun applySubtitlePresentation()"))
        assertTrue(source.contains("PlayerSubtitlePresentationPolicy.present("))
        assertTrue(source.contains("tvSubtitle.visibility = if (presentation.visible) View.VISIBLE else View.GONE"))
    }

    @Test
    fun notificationPermissionRequestIsRememberedAndDoesNotGatePlayback() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val controller = String(Files.readAllBytes(playerNotificationControllerSource()))
        val permissionBlock = source.substringAfter("private fun ensureNotificationPermission()")
            .substringBefore("\n    private fun registerPlaybackNotificationHandlers()")
        val startServiceBlock = source.substringAfter("private fun startPlaybackServiceIfNeeded")
            .substringBefore("\n    private fun stopPlaybackService")

        assertTrue(permissionBlock.contains("playbackNotifications.ensurePermission()"))
        assertTrue(controller.contains("KEY_NOTIFICATION_PERMISSION_REQUESTED"))
        assertTrue(controller.contains("requestedBefore ="))
        assertTrue(controller.contains("notificationEnabled = playerPrefs.bgPlaybackNotificationEnabled"))
        assertTrue(controller.contains(".edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply()"))
        assertTrue(controller.contains("notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)"))
        assertTrue(startServiceBlock.contains("playbackNotifications.startIfNeeded(isPlaying)"))
        assertTrue(controller.contains("PlayerBackgroundServicePolicy.startDecision("))
        assertTrue(controller.contains("runCatching { ContextCompat.startForegroundService(activity, intent) }"))
    }

    @Test
    fun notificationResumeSuppressesOpenTransitionAndOnlyReattachesPlayer() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val onCreate = source.substringAfter("override fun onCreate(savedInstanceState: Bundle?) {")
            .substringBefore("\n    private fun applyPlayerSettings()")
        val onNewIntent = source.substringAfter("override fun onNewIntent(intent: Intent) {")
            .substringBefore("\n    private fun applyPlayerSettings()")

        assertTrue(source.contains("const val EXTRA_FROM_PLAYBACK_NOTIFICATION"))
        assertTrue(source.contains("private fun suppressNotificationOpenTransition("))
        assertTrue(onCreate.contains("suppressNotificationOpenTransition(intent)"))
        assertTrue(onNewIntent.contains("suppressNotificationOpenTransition(intent)"))
        assertTrue(onNewIntent.contains("reattachPlayerSurfaceFromBackground()"))
        assertTrue(onNewIntent.indexOf("suppressNotificationOpenTransition(intent)") < onNewIntent.indexOf("reattachPlayerSurfaceFromBackground()"))
        assertTrue(onNewIntent.contains("setIntent(intent)"))
        assertTrue(onNewIntent.indexOf("setIntent(intent)") < onNewIntent.indexOf("reattachPlayerSurfaceFromBackground()"))
    }

    private fun playerActivitySource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerActivity.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerNotificationControllerSource(): Path {
        return kotlinSource("PlayerPlaybackNotificationController.kt")
    }

    private fun playerFirstFrameControllerSource(): Path {
        return kotlinSource("PlayerFirstFrameController.kt")
    }

    private fun playerEventControllerSource(): Path {
        return kotlinSource("PlayerEventController.kt")
    }

    private fun kotlinSource(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            name
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerLayoutSource(folder: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            folder,
            "activity_player.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
