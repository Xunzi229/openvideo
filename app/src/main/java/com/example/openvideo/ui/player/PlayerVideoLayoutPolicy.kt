package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio

object PlayerVideoLayoutPolicy {

    fun orientationForVideo(
        width: Int,
        height: Int,
        pixelWidthHeightRatio: Float = 1f,
        unappliedRotationDegrees: Int = 0
    ): Int {
        val ratio = displayAspectRatio(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
        if (ratio <= 0f) return PlayerOrientationPolicy.defaultOrientation()

        return when {
            ratio >= 1.2f -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ratio <= 0.8f -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    fun contentAspectRatio(
        preferredAspectRatio: AspectRatio,
        width: Int,
        height: Int,
        pixelWidthHeightRatio: Float = 1f,
        unappliedRotationDegrees: Int = 0
    ): Float {
        val forcedAspectRatio = PlayerViewSettings.forcedContentAspectRatio(preferredAspectRatio)
        if (forcedAspectRatio != null) return forcedAspectRatio

        return displayAspectRatio(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
    }

    fun displayAspectRatio(
        width: Int,
        height: Int,
        pixelWidthHeightRatio: Float = 1f,
        unappliedRotationDegrees: Int = 0
    ): Float {
        if (width <= 0 || height <= 0) return 0f

        val normalizedPixelRatio = pixelWidthHeightRatio
            .takeIf { it.isFinite() && it > 0f }
            ?: 1f
        val isQuarterTurn = normalizedRotation(unappliedRotationDegrees) % 180 != 0
        val displayWidth = if (isQuarterTurn) height.toFloat() else width * normalizedPixelRatio
        val displayHeight = if (isQuarterTurn) width * normalizedPixelRatio else height.toFloat()
        if (displayWidth <= 0f || displayHeight <= 0f) return 0f
        return displayWidth / displayHeight
    }

    private fun normalizedRotation(unappliedRotationDegrees: Int): Int =
        ((unappliedRotationDegrees % 360) + 360) % 360
}
