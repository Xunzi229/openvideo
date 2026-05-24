package com.example.openvideo.core.player

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
internal class PlayerAudioEffectsController(
    private val playerProvider: () -> ExoPlayer?
) {
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    fun setVolumeBoost(enabled: Boolean) {
        if (enabled) {
            enableLoudnessEnhancer()
        } else {
            releaseLoudnessEnhancer()
        }
    }

    fun initEqualizer(audioSessionId: Int) {
        releaseEqualizer()
        try {
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true
        } catch (_: Exception) {
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

    fun releaseAll() {
        releaseLoudnessEnhancer()
        releaseEqualizer()
    }

    private fun enableLoudnessEnhancer() {
        val audioSessionId = playerProvider()?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

        releaseLoudnessEnhancer()
        loudnessEnhancer = runCatching {
            LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(VOLUME_BOOST_MILLIBELS)
                enabled = true
            }
        }.getOrNull()
    }

    private fun releaseLoudnessEnhancer() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }

    private companion object {
        const val VOLUME_BOOST_MILLIBELS = 600
    }
}
