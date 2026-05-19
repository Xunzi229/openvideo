package com.example.openvideo.core.player

import com.example.openvideo.core.prefs.LoopMode
import com.example.openvideo.data.model.VideoItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 前台播放通知与 [PlayerActivity] 之间的会话快照与控制回调桥接。
 * Activity 在 onStart 注册回调；Service 通过快照构造 ContentIntent 与队列切歌。
 */
@Singleton
class PlaybackNotificationCoordinator @Inject constructor() {

    data class Snapshot(
        val videoUri: String,
        val title: String,
        val videoId: Long,
        val videoPath: String,
        val videoWidth: Int,
        val videoHeight: Int,
        val queue: List<VideoItem>,
        val loopMode: LoopMode,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long
    )

    @Volatile
    var snapshot: Snapshot? = null
        private set

    var onTogglePlayPause: (() -> Unit)? = null
        private set

    var onSkipToNext: (() -> Unit)? = null
        private set

    var onSkipToPrevious: (() -> Unit)? = null
        private set

    fun updateSnapshot(snapshot: Snapshot) {
        this.snapshot = snapshot
    }

    fun clearSnapshot() {
        snapshot = null
    }

    fun updatePlaybackProgress(isPlaying: Boolean, positionMs: Long, durationMs: Long) {
        snapshot = snapshot?.copy(
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs
        )
    }

    fun registerHandlers(
        onTogglePlayPause: () -> Unit,
        onSkipToNext: () -> Unit,
        onSkipToPrevious: () -> Unit
    ) {
        this.onTogglePlayPause = onTogglePlayPause
        this.onSkipToNext = onSkipToNext
        this.onSkipToPrevious = onSkipToPrevious
    }

    fun clearHandlers() {
        onTogglePlayPause = null
        onSkipToNext = null
        onSkipToPrevious = null
    }

    fun hasActivityHandlers(): Boolean = onTogglePlayPause != null
}
