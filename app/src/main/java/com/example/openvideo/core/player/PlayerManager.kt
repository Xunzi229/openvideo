package com.example.openvideo.core.player

import android.content.Context
import android.graphics.Bitmap
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.openvideo.core.prefs.AspectRatio
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class DecodeMode { SOFT, HARD }
enum class RenderMode { SURFACE, TEXTURE }

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    var player: ExoPlayer? = null
        private set

    private var trackSelector: DefaultTrackSelector? = null

    var decodeMode = DecodeMode.HARD
    var renderMode = RenderMode.SURFACE
    var aspectRatio = AspectRatio.FIT

    fun initialize(): ExoPlayer {
        trackSelector = DefaultTrackSelector(context)

        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector!!)
            .build()

        this.player = player
        return player
    }

    fun release() {
        clearEffects()
        releaseEqualizer()
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

    fun setSpeed(speed: Float, pitch: Float = 1.0f) {
        player?.playbackParameters = PlaybackParameters(speed, pitch)
    }

    fun setRepeatMode(repeatMode: Int) {
        player?.repeatMode = repeatMode
    }

    fun setVolumeBoost(enabled: Boolean) {
        player?.volume = if (enabled) 1.5f else 1.0f
    }

    fun applyDecodeMode(mode: DecodeMode) {
        decodeMode = mode
        // Software vs hardware decode is device-dependent; ExoPlayer selects decoders automatically.
    }

    fun getAspectRatioValue(): Float {
        return when (aspectRatio) {
            AspectRatio.FIT,
            AspectRatio.FILL,
            AspectRatio.CROP,
            AspectRatio.STRETCH -> 0f
            AspectRatio.RATIO_4_3 -> 4f / 3f
            AspectRatio.RATIO_16_9 -> 16f / 9f
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

    // P1: Video Filters
    private val activeEffects = mutableListOf<androidx.media3.common.Effect>()

    @OptIn(UnstableApi::class)
    fun setBrightness(value: Float) {
        activeEffects.removeAll { it is Brightness }
        activeEffects.add(Brightness(value))
        applyEffects()
    }

    @OptIn(UnstableApi::class)
    fun setContrast(value: Float) {
        activeEffects.removeAll { it is Contrast }
        activeEffects.add(Contrast(value))
        applyEffects()
    }

    @OptIn(UnstableApi::class)
    fun setRotation(degrees: Float) {
        activeEffects.removeAll { it is ScaleAndRotateTransformation }
        activeEffects.add(ScaleAndRotateTransformation.Builder().setRotationDegrees(degrees).build())
        applyEffects()
    }

    @OptIn(UnstableApi::class)
    fun clearEffects() {
        activeEffects.clear()
        player?.setVideoEffects(emptyList())
    }

    @OptIn(UnstableApi::class)
    private fun applyEffects() {
        player?.setVideoEffects(activeEffects.toList())
    }

    // P1: Equalizer
    private var equalizer: Equalizer? = null

    fun initEqualizer(audioSessionId: Int) {
        releaseEqualizer()
        try {
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true
        } catch (e: Exception) {
            equalizer = null
        }
    }

    fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }

    fun setEqualizerPreset(preset: Short) {
        equalizer?.usePreset(preset)
    }

    fun getEqualizerPresets(): List<String> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
    }

    fun setEqualizerBand(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
    }

    // P1: Screenshot
    fun takeScreenshot(surfaceView: SurfaceView, callback: (Boolean, String?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            callback(false, null)
            return
        }
        val bitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(
            surfaceView,
            bitmap,
            { result ->
            if (result == PixelCopy.SUCCESS) {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OpenVideo")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "screenshot_${System.currentTimeMillis()}.jpg")
                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    callback(true, file.absolutePath)
                } catch (e: Exception) {
                    callback(false, null)
                }
            } else {
                callback(false, null)
            }
            bitmap.recycle()
            },
            Handler(Looper.getMainLooper())
        )
    }
}
