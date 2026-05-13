package com.example.openvideo.core.player

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSessionManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private var mediaSession: MediaSessionCompat? = null

    fun create(callback: MediaSessionCompat.Callback): MediaSessionCompat {
        val session = MediaSessionCompat(context, "OpenVideoSession").apply {
            setCallback(callback)
            isActive = true
        }
        mediaSession = session
        return session
    }

    fun updatePlaybackState(state: Int, position: Long, speed: Float) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, position, speed)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    fun updateMetadata(title: String, duration: Long) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .build()
        mediaSession?.setMetadata(metadata)
    }

    fun getSessionToken(): MediaSessionCompat.Token? = mediaSession?.sessionToken

    fun release() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
    }
}
