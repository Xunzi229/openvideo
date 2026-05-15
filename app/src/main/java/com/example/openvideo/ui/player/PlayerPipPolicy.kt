package com.example.openvideo.ui.player

import android.os.Build

data class PlayerPipAspectRatio(
    val numerator: Int,
    val denominator: Int
)

data class PlayerPipDecision(
    val shouldEnter: Boolean,
    val aspectRatio: PlayerPipAspectRatio? = null
)

object PlayerPipPolicy {
    private val FALLBACK_ASPECT_RATIO = PlayerPipAspectRatio(16, 9)
    private const val RATIO_SCALE = 1000

    fun enterDecision(
        sdkInt: Int,
        supportsPictureInPicture: Boolean,
        isAlreadyInPictureInPicture: Boolean,
        videoWidth: Int,
        videoHeight: Int,
        pixelWidthHeightRatio: Float = 1f,
        unappliedRotationDegrees: Int = 0
    ): PlayerPipDecision {
        if (sdkInt < Build.VERSION_CODES.O) {
            return PlayerPipDecision(shouldEnter = false)
        }
        if (!supportsPictureInPicture || isAlreadyInPictureInPicture) {
            return PlayerPipDecision(shouldEnter = false)
        }

        return PlayerPipDecision(
            shouldEnter = true,
            aspectRatio = aspectRatioForVideo(
                width = videoWidth,
                height = videoHeight,
                pixelWidthHeightRatio = pixelWidthHeightRatio,
                unappliedRotationDegrees = unappliedRotationDegrees
            )
        )
    }

    fun aspectRatioForVideo(
        width: Int,
        height: Int,
        pixelWidthHeightRatio: Float = 1f,
        unappliedRotationDegrees: Int = 0
    ): PlayerPipAspectRatio {
        val displayAspectRatio = PlayerVideoLayoutPolicy.displayAspectRatio(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
        if (displayAspectRatio <= 0f || !displayAspectRatio.isFinite()) {
            return FALLBACK_ASPECT_RATIO
        }

        return PlayerPipAspectRatio(
            numerator = (displayAspectRatio * RATIO_SCALE).toInt().coerceAtLeast(1),
            denominator = RATIO_SCALE
        )
    }
}
