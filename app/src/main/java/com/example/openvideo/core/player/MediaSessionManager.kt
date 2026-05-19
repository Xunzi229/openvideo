package com.example.openvideo.core.player

import android.content.Context
import android.os.SystemClock
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

    fun updatePlaybackState(state: Int, position: Long, speed: Float, actions: Long) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, position, speed, SystemClock.elapsedRealtime())
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

    fun clearMetadata() {
        mediaSession?.setMetadata(null)
    }

    fun markStopped() {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(0)
            .setState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    fun getSessionToken(): MediaSessionCompat.Token? = mediaSession?.sessionToken

    fun release() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
    }
}
