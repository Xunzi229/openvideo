package com.example.openvideo.core.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class DecodeMode { SOFT, HARD }
enum class RenderMode { SURFACE, TEXTURE }
enum class AspectRatio { DEFAULT, RATIO_4_3, RATIO_16_9, FILL }

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    var player: ExoPlayer? = null
        private set

    private var trackSelector: DefaultTrackSelector? = null

    var decodeMode = DecodeMode.HARD
    var renderMode = RenderMode.SURFACE
    var aspectRatio = AspectRatio.DEFAULT

    fun initialize(): ExoPlayer {
        trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector!!)
            .build()

        this.player = player
        return player
    }

    fun release() {
        player?.release()
        player = null
        trackSelector = null
    }

    fun setMediaUri(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekForward(ms: Long = 10_000) {
        player?.let {
            it.seekTo((it.currentPosition + ms).coerceAtMost(it.duration))
        }
    }

    fun seekBackward(ms: Long = 10_000) {
        player?.let {
            it.seekTo((it.currentPosition - ms).coerceAtLeast(0))
        }
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
    }

    @OptIn(UnstableApi::class)
    fun applyDecodeMode(mode: DecodeMode) {
        decodeMode = mode
        trackSelector?.let { selector ->
            val params = selector.buildUponParameters()
            when (mode) {
                DecodeMode.SOFT -> {
                    // Force software decoder by overriding to prefer VP9/AVC software decoders
                    params.setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
                }
                DecodeMode.HARD -> {
                    params.setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
                }
            }
            selector.setParameters(params)
        }
    }

    fun getAspectRatioValue(): Float {
        return when (aspectRatio) {
            AspectRatio.DEFAULT -> 0f
            AspectRatio.RATIO_4_3 -> 4f / 3f
            AspectRatio.RATIO_16_9 -> 16f / 9f
            AspectRatio.FILL -> 0f
        }
    }

    val isPlaying: Boolean get() = player?.isPlaying == true
    val currentPosition: Long get() = player?.currentPosition ?: 0
    val duration: Long get() = player?.duration ?: 0
    val playbackState: Int get() = player?.playbackState ?: Player.STATE_IDLE

    fun addListener(listener: Player.Listener) {
        player?.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        player?.removeListener(listener)
    }
}
