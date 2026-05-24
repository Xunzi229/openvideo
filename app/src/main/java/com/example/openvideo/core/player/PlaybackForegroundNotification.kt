package com.example.openvideo.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.openvideo.R

internal class PlaybackForegroundNotification(
    private val context: Context
) {
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.playback_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.playback_notification_channel_description)
                setShowBadge(false)
            }
            context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }

    fun buildPlaceholderNotification(): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_playback)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.playback_notification_status_playing))
            .setOngoing(true)
            .build()

    private companion object {
        const val CHANNEL_ID = "playback_channel_v2"
    }
}
