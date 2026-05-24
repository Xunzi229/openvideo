package com.example.openvideo.core.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
internal class PlayerVideoEffectsController(
    private val playerProvider: () -> ExoPlayer?
) {
    private val activeEffects = mutableListOf<androidx.media3.common.Effect>()
    private var lastVideoAdjustments: VideoAdjustments? = null

    fun setBrightness(value: Float) {
        lastVideoAdjustments = null
        activeEffects.removeAll { it is Brightness }
        activeEffects.add(Brightness(value))
        applyEffects()
    }

    fun setContrast(value: Float) {
        lastVideoAdjustments = null
        activeEffects.removeAll { it is Contrast }
        activeEffects.add(Contrast(value))
        applyEffects()
    }

    fun applyVideoAdjustments(
        brightness: Float,
        contrast: Float,
        saturation: Float
    ) {
        val nextAdjustments = VideoAdjustments(brightness, contrast, saturation)
        if (lastVideoAdjustments == nextAdjustments) return
        lastVideoAdjustments = nextAdjustments
        activeEffects.removeAll { it is Brightness || it is Contrast || it is RgbMatrix }
        if (brightness != 0f) {
            activeEffects.add(Brightness(brightness))
        }
        if (contrast != 0f) {
            activeEffects.add(Contrast(contrast))
        }
        if (saturation != 0f) {
            activeEffects.add(saturationMatrix(saturation))
        }
        applyEffects()
    }

    fun setRotation(degrees: Float) {
        lastVideoAdjustments = null
        activeEffects.removeAll { it is ScaleAndRotateTransformation }
        activeEffects.add(ScaleAndRotateTransformation.Builder().setRotationDegrees(degrees).build())
        applyEffects()
    }

    fun clearEffects() {
        lastVideoAdjustments = null
        activeEffects.clear()
        playerProvider()?.setVideoEffects(emptyList())
    }

    private fun applyEffects() {
        playerProvider()?.setVideoEffects(activeEffects.toList())
    }

    private fun saturationMatrix(value: Float): RgbMatrix {
        val scale = (1f + value).coerceAtLeast(0f)
        val inverse = 1f - scale
        val red = 0.213f * inverse
        val green = 0.715f * inverse
        val blue = 0.072f * inverse
        val matrix = floatArrayOf(
            red + scale, green, blue, 0f,
            red, green + scale, blue, 0f,
            red, green, blue + scale, 0f,
            0f, 0f, 0f, 1f
        )
        return object : RgbMatrix {
            override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix
        }
    }

    private data class VideoAdjustments(
        val brightness: Float,
        val contrast: Float,
        val saturation: Float
    )
}
