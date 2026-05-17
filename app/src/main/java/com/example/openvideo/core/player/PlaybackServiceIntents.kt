package com.example.openvideo.core.player

import android.content.Context
import android.content.Intent
import com.example.openvideo.ui.player.PlayerActivity
import com.example.openvideo.ui.player.putSessionQueue

/**
 * 集中构造 [PlaybackService] 与从通知回到 [PlayerActivity] 的 Intent。
 */
object PlaybackServiceIntents {

    fun start(context: Context, title: String, isPlaying: Boolean): Intent =
        Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_START
            putExtra(PlaybackService.EXTRA_TITLE, title)
            putExtra(PlaybackService.EXTRA_IS_PLAYING, isPlaying)
        }

    fun refresh(context: Context): Intent =
        Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_REFRESH
        }

    fun togglePlayPause(context: Context): Intent =
        serviceAction(context, PlaybackService.ACTION_TOGGLE_PLAY_PAUSE)

    fun skipToNext(context: Context): Intent =
        serviceAction(context, PlaybackService.ACTION_SKIP_TO_NEXT)

    fun skipToPrevious(context: Context): Intent =
        serviceAction(context, PlaybackService.ACTION_SKIP_TO_PREVIOUS)

    fun stop(context: Context): Intent =
        Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_DISMISS
        }

    fun openPlayer(context: Context, snapshot: PlaybackNotificationCoordinator.Snapshot): Intent =
        Intent(context, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("video_uri", snapshot.videoUri)
            putExtra("video_title", snapshot.title)
            putExtra("video_id", snapshot.videoId)
            putExtra("video_path", snapshot.videoPath)
            putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, snapshot.videoWidth)
            putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, snapshot.videoHeight)
            putSessionQueue(snapshot.queue)
        }

    private fun serviceAction(context: Context, action: String): Intent =
        Intent(context, PlaybackService::class.java).apply { this.action = action }
}
