package com.example.openvideo.core.player

import android.support.v4.media.session.MediaSessionCompat

internal class PlaybackServiceMediaSessionConnector(
    private val mediaSessionManager: MediaSessionManager,
    private val playerManager: PlayerManager,
    private val onTogglePlayPause: () -> Unit,
    private val onSkipToNext: () -> Unit,
    private val onSkipToPrevious: () -> Unit,
    private val onSeekChanged: () -> Unit,
    private val onStop: () -> Unit
) {
    fun create(): MediaSessionCompat =
        mediaSessionManager.create(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                onTogglePlayPause()
            }

            override fun onPause() {
                onTogglePlayPause()
            }

            override fun onSkipToNext() {
                onSkipToNext()
            }

            override fun onSkipToPrevious() {
                onSkipToPrevious()
            }

            override fun onSeekTo(pos: Long) {
                playerManager.seekTo(pos)
                onSeekChanged()
            }

            override fun onStop() {
                onStop()
            }
        })
}
