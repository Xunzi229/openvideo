package com.example.openvideo.core.player

import android.app.PendingIntent

internal data class PlaybackNotificationPayload(
    val title: String,
    val statusText: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val canSkipToPrevious: Boolean,
    val canSkipToNext: Boolean,
    val contentIntent: PendingIntent?
)
