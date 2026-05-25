package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerActivityNotificationControllerSourceTest {

    @Test
    fun playbackNotificationLogicLivesOutsidePlayerActivity() {
        val activity = playerActivitySource()
        val controller = sourceFile("PlayerPlaybackNotificationController.kt")

        assertTrue(activity.contains("private val playbackNotifications by lazy"))
        assertTrue(activity.contains("playbackNotifications.ensurePermission()"))
        assertTrue(activity.contains("playbackNotifications.registerHandlers()"))
        assertTrue(activity.contains("playbackNotifications.syncSnapshot("))
        assertTrue(activity.contains("playbackNotifications.startIfNeeded("))
        assertTrue(activity.contains("playbackNotifications.skipQueueVideo("))

        assertTrue(controller.contains("class PlayerPlaybackNotificationController"))
        assertTrue(controller.contains("PlayerNotificationPermissionPolicy.shouldRequestPermission("))
        assertTrue(controller.contains("PlaybackNotificationCoordinator.Snapshot("))
        assertTrue(controller.contains("PlayerBackgroundServicePolicy.startDecision("))
        assertTrue(controller.contains("PlaybackServiceIntents.start("))
        assertTrue(controller.contains("PlaybackServiceIntents.stop("))
        assertTrue(controller.contains("PlayerQueueSkipPolicy.nextIndex("))
        assertTrue(controller.contains("PlayerQueueSkipPolicy.previousIndex("))

        assertFalse(activity.contains("PlayerNotificationPermissionPolicy.shouldRequestPermission("))
        assertFalse(activity.contains("PlaybackNotificationCoordinator.Snapshot("))
        assertFalse(activity.contains("PlayerQueueSkipPolicy.nextIndex("))
    }

    private fun playerActivitySource(): String = sourceFile("PlayerActivity.kt")

    private fun sourceFile(fileName: String): String {
        val relativePath: Path = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            fileName
        )
        val path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
