package com.example.openvideo.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlaybackNotificationStabilitySourceTest {

    @Test
    fun notificationUiDedupKeyDoesNotIncludeChangingPlaybackPosition() {
        val source = playbackServiceSource()
        val publishBlock = source.substringAfter("private fun publishNotification(")
            .substringBefore("\n    private fun playbackNotificationUiKey(")
        val keyBlock = source.substringAfter("private fun playbackNotificationUiKey(")
            .substringBefore("\n    private data class NotificationPayload")

        assertTrue(keyBlock.contains("payload.title"))
        assertTrue(keyBlock.contains("payload.isPlaying"))
        assertTrue(keyBlock.contains("payload.durationMs"))
        assertFalse(keyBlock.contains("payload.statusText"))
        assertFalse(keyBlock.contains("payload.positionMs"))
        assertFalse(keyBlock.contains("progress"))

        assertTrue(publishBlock.contains("PlaybackNotificationProgressPolicy.progressBucket"))
        assertTrue(publishBlock.contains("lastProgressBucket"))
    }

    @Test
    fun playbackNotificationUsesCenteredCustomTransportControls() {
        val service = playbackServiceSource()
        val layout = loadSource(
            Paths.get("src", "main", "res", "layout", "notification_playback.xml")
        )

        assertTrue(service.contains("R.layout.notification_playback"))
        assertTrue(service.contains("setCustomContentView"))
        assertTrue(service.contains("setCustomBigContentView"))
        assertFalse(service.contains("NotificationCompat.DecoratedCustomViewStyle()"))
        assertFalse(service.contains("MediaNotificationCompat.MediaStyle()"))
        assertFalse(service.contains("ic_notification_action_spacer"))
        assertTrue(service.contains("R.id.notification_root"))
        assertTrue(service.contains("R.id.notification_title"))
        assertTrue(service.contains("R.id.notification_status_text"))
        assertTrue(service.contains("payload.contentIntent?.let"))
        assertTrue(service.contains("ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()"))

        assertTrue(layout.contains("android:id=\"@+id/notification_root\""))
        assertTrue(layout.contains("android:id=\"@+id/notification_transport_row\""))
        assertTrue(layout.contains("android:gravity=\"center_horizontal|center_vertical\""))
        assertFalse(layout.contains("android:paddingEnd=\"28dp\""))
        assertTrue(layout.contains("android:id=\"@+id/btn_previous\""))
        assertTrue(layout.contains("android:id=\"@+id/btn_play_pause\""))
        assertTrue(layout.contains("android:id=\"@+id/btn_next\""))
        assertTrue(layout.contains("android:id=\"@+id/notification_progress\""))
        assertTrue(layout.contains("android:id=\"@+id/notification_progress_row\""))
        assertTrue(layout.contains("android:id=\"@+id/notification_time_elapsed\""))
        assertTrue(layout.contains("android:id=\"@+id/notification_time_duration\""))
        assertTrue(layout.contains("android:id=\"@+id/seek_zone_0\""))
        assertFalse(layout.contains("<View android:id=\"@+id/seek_zone_0\""))
        assertTrue(service.contains("applyPlaybackNotificationProgress"))
        assertTrue(service.contains("PlaybackNotificationProgressPolicy.barState"))
        assertTrue(service.contains("PlaybackNotificationProgressPolicy.timeLabels"))
        assertTrue(service.contains("PlaybackNotificationSeekPolicy"))
        assertTrue(service.contains("ACTION_SEEK_TO_MS"))
    }

    private fun playbackServiceSource(): String = loadSource(
        Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "player",
            "PlaybackService.kt"
        )
    )

    private fun loadSource(relativePath: Path): String {
        val path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
