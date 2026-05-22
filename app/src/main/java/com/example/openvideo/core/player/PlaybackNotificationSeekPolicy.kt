package com.example.openvideo.core.player

/**
 * Maps tap zones on the notification progress row to seek targets.
 * RemoteViews cannot host plain View or SeekBar; empty TextView tap zones approximate scrubbing.
 */
object PlaybackNotificationSeekPolicy {

    const val ZONE_COUNT = 12

    fun seekPositionMs(zoneIndex: Int, durationMs: Long): Long {
        if (durationMs <= 0L || ZONE_COUNT <= 0) return 0L
        val index = zoneIndex.coerceIn(0, ZONE_COUNT - 1)
        val fraction = (index + 0.5f) / ZONE_COUNT.toFloat()
        return (durationMs * fraction).toLong().coerceIn(0L, durationMs)
    }
}
