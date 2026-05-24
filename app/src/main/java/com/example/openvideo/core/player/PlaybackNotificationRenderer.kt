package com.example.openvideo.core.player

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.openvideo.R

internal class PlaybackNotificationRenderer(
    private val context: Context
) {
    fun build(payload: PlaybackNotificationPayload): Notification {
        val remoteViews = buildPlaybackRemoteViews(payload)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_playback)
            .setContentTitle(payload.title)
            .setContentText(payload.statusText)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(payload.isPlaying)
            .setContentIntent(payload.contentIntent)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .build()
    }

    private fun buildPlaybackRemoteViews(payload: PlaybackNotificationPayload): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notification_playback)
        views.setTextViewText(R.id.notification_title, payload.title)
        views.setTextViewText(R.id.notification_status_text, payload.statusText)
        views.setTextColor(
            R.id.notification_title,
            ContextCompat.getColor(context, R.color.playback_notification_title)
        )
        views.setTextColor(
            R.id.notification_status_text,
            ContextCompat.getColor(context, R.color.playback_notification_status)
        )
        payload.contentIntent?.let { openPlayerIntent ->
            views.setOnClickPendingIntent(R.id.notification_root, openPlayerIntent)
            views.setOnClickPendingIntent(R.id.notification_title, openPlayerIntent)
            views.setOnClickPendingIntent(R.id.notification_status_text, openPlayerIntent)
        }

        views.setImageViewResource(
            R.id.btn_play_pause,
            if (payload.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        applyPlaybackNotificationIconTint(views)
        views.setOnClickPendingIntent(
            R.id.btn_play_pause,
            servicePendingIntent(REQUEST_TOGGLE, PlaybackService.ACTION_TOGGLE_PLAY_PAUSE)
        )

        views.setViewVisibility(
            R.id.btn_previous,
            if (payload.canSkipToPrevious) View.VISIBLE else View.INVISIBLE
        )
        if (payload.canSkipToPrevious) {
            views.setOnClickPendingIntent(
                R.id.btn_previous,
                servicePendingIntent(REQUEST_PREVIOUS, PlaybackService.ACTION_SKIP_TO_PREVIOUS)
            )
        }

        views.setViewVisibility(
            R.id.btn_next,
            if (payload.canSkipToNext) View.VISIBLE else View.INVISIBLE
        )
        if (payload.canSkipToNext) {
            views.setOnClickPendingIntent(
                R.id.btn_next,
                servicePendingIntent(REQUEST_NEXT, PlaybackService.ACTION_SKIP_TO_NEXT)
            )
        }

        applyPlaybackNotificationProgress(views, payload)

        return views
    }

    private fun applyPlaybackNotificationProgress(
        views: RemoteViews,
        payload: PlaybackNotificationPayload
    ) {
        val bar = PlaybackNotificationProgressPolicy.barState(
            positionMs = payload.positionMs,
            durationMs = payload.durationMs
        )
        val times = PlaybackNotificationProgressPolicy.timeLabels(
            positionMs = payload.positionMs,
            durationMs = payload.durationMs
        )
        val statusColor = ContextCompat.getColor(context, R.color.playback_notification_status)

        views.setViewVisibility(
            R.id.notification_progress_row,
            if (bar.visible) View.VISIBLE else View.GONE
        )
        if (!bar.visible) {
            return
        }

        views.setTextViewText(R.id.notification_time_elapsed, times.elapsed)
        views.setTextViewText(R.id.notification_time_duration, times.duration)
        views.setTextColor(R.id.notification_time_elapsed, statusColor)
        views.setTextColor(R.id.notification_time_duration, statusColor)
        views.setProgressBar(R.id.notification_progress, bar.max, bar.progress, false)

        for (zoneIndex in 0 until PlaybackNotificationSeekPolicy.ZONE_COUNT) {
            val viewId = SEEK_ZONE_VIEW_IDS[zoneIndex]
            val seekMs = PlaybackNotificationSeekPolicy.seekPositionMs(
                zoneIndex = zoneIndex,
                durationMs = payload.durationMs
            )
            views.setOnClickPendingIntent(
                viewId,
                seekZonePendingIntent(zoneIndex, seekMs)
            )
        }
    }

    private fun applyPlaybackNotificationIconTint(views: RemoteViews) {
        val iconColor = ContextCompat.getColor(context, R.color.playback_notification_icon)
        listOf(R.id.btn_previous, R.id.btn_play_pause, R.id.btn_next).forEach { viewId ->
            @Suppress("DEPRECATION")
            views.setInt(viewId, "setColorFilter", iconColor)
        }
    }

    private fun servicePendingIntent(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            Intent(context, PlaybackService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun seekZonePendingIntent(zoneIndex: Int, positionMs: Long): PendingIntent =
        PendingIntent.getService(
            context,
            REQUEST_SEEK_ZONE_BASE + zoneIndex,
            Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_SEEK_TO_MS
                putExtra(PlaybackService.EXTRA_SEEK_POSITION_MS, positionMs)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private companion object {
        const val CHANNEL_ID = "playback_channel_v2"
        const val REQUEST_PREVIOUS = 11
        const val REQUEST_TOGGLE = 12
        const val REQUEST_NEXT = 13
        const val REQUEST_SEEK_ZONE_BASE = 20

        val SEEK_ZONE_VIEW_IDS = intArrayOf(
            R.id.seek_zone_0,
            R.id.seek_zone_1,
            R.id.seek_zone_2,
            R.id.seek_zone_3,
            R.id.seek_zone_4,
            R.id.seek_zone_5,
            R.id.seek_zone_6,
            R.id.seek_zone_7,
            R.id.seek_zone_8,
            R.id.seek_zone_9,
            R.id.seek_zone_10,
            R.id.seek_zone_11
        )
    }
}
