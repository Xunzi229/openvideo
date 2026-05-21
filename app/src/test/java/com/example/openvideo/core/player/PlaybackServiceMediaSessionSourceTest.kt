package com.example.openvideo.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Source guards for Sprint 0.2.5 lock-screen / notification MediaSession sync.
 */
class PlaybackServiceMediaSessionSourceTest {

    @Test
    fun publishNotificationSyncsMediaSessionThroughPolicies() {
        val source = playbackServiceSource()
        assertTrue(source.contains("MediaSessionPlaybackStatePolicy.resolve("))
        assertTrue(source.contains("PlaybackNotificationSkipCapabilityPolicy.fromSnapshot("))
        assertTrue(source.contains("mediaSessionManager.updatePlaybackState("))
    }

    @Test
    fun customNotificationKeepsMediaSessionStateSeparateFromNotificationLayout() {
        val source = playbackServiceSource()
        assertTrue(source.contains("syncMediaSession(payload.isPlaying, payload.positionMs)"))
        assertFalse(source.contains("NotificationCompat.DecoratedCustomViewStyle()"))
        assertFalse(source.contains("MediaNotificationCompat.MediaStyle()"))
    }

    @Test
    fun notificationSkipButtonsFollowSkipCapabilityPolicy() {
        val source = playbackServiceSource()
        assertTrue(source.contains("canSkipToPrevious"))
        assertTrue(source.contains("canSkipToNext"))
        assertFalse(source.contains("hasQueue"))
    }

    @Test
    fun playerActivitySnapshotIncludesLoopModeForSkipCapability() {
        val source = playerActivitySource()
        assertTrue(source.contains("loopMode = playerPrefs.loopMode"))
    }

    private fun playbackServiceSource(): String = loadSource(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "core", "player", "PlaybackService.kt")
    )

    private fun playerActivitySource(): String = loadSource(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", "PlayerActivity.kt")
    )

    private fun loadSource(relativePath: Path): String {
        val path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
