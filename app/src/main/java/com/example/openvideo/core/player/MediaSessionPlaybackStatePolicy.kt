package com.example.openvideo.core.player

import android.support.v4.media.session.PlaybackStateCompat

/**
 * 将播放状态与可用 MediaSession 动作（含 skip 能力）统一建模，供锁屏/蓝牙 AVRCP 读取。
 */
object MediaSessionPlaybackStatePolicy {

    data class Update(
        val state: Int,
        val positionMs: Long,
        val speed: Float,
        val actions: Long
    )

    fun resolve(
        isPlaying: Boolean,
        positionMs: Long,
        speed: Float,
        canSkipToNext: Boolean,
        canSkipToPrevious: Boolean
    ): Update {
        var actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SEEK_TO
        if (canSkipToNext) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        }
        if (canSkipToPrevious) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }
        return Update(
            state = if (isPlaying) {
                PlaybackStateCompat.STATE_PLAYING
            } else {
                PlaybackStateCompat.STATE_PAUSED
            },
            positionMs = positionMs,
            speed = if (isPlaying) speed else 0f,
            actions = actions
        )
    }
}
