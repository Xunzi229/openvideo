package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode

data class PlayerSmartCropDecision(
    val contentFrameMode: ContentFrameMode?,
    val aspectRatioOverride: AspectRatio? = null,
    val viewportFillFraction: Float? = null,
    val viewportScale: PlayerContentFrameViewportScale? = null,
    val cropExpansionFraction: Float = 0f
)

data class PlayerSmartCropBlackBorders(
    val left: Boolean,
    val top: Boolean,
    val right: Boolean,
    val bottom: Boolean
) {
    val hasAllEdges: Boolean
        get() = left && top && right && bottom
}

object PlayerSmartCropPolicy {

    const val VIEWPORT_FILL_FRACTION = 0.88f
    const val CROP_EXPANSION_FRACTION = 0.25f

    fun quickToggleDecision(
        currentMode: ContentFrameMode,
        currentAspectRatio: AspectRatio,
        sourceWidth: Int,
        sourceHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int,
        blackBorders: PlayerSmartCropBlackBorders? = null
    ): PlayerSmartCropDecision {
        if (currentMode != ContentFrameMode.OFF) {
            return PlayerSmartCropDecision(contentFrameMode = ContentFrameMode.OFF)
        }
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return PlayerSmartCropDecision(contentFrameMode = null)
        }
        if (viewportWidth <= viewportHeight) {
            return PlayerSmartCropDecision(contentFrameMode = null)
        }
        if (blackBorders?.hasAllEdges != true) {
            return PlayerSmartCropDecision(contentFrameMode = null)
        }

        return PlayerSmartCropDecision(
            contentFrameMode = ContentFrameMode.CENTER_16_9,
            aspectRatioOverride = if (PlayerContentFramePolicy.allowsContentFrameAdjustment(currentAspectRatio)) {
                null
            } else {
                AspectRatio.FIT
            },
            viewportFillFraction = VIEWPORT_FILL_FRACTION,
            viewportScale = PlayerContentFrameViewportScale.FIT_INSIDE,
            cropExpansionFraction = CROP_EXPANSION_FRACTION
        )
    }

    fun restoredViewportDecision(
        restoredMode: ContentFrameMode,
        sourceWidth: Int,
        sourceHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int
    ): PlayerSmartCropDecision = PlayerSmartCropDecision(contentFrameMode = null)
}
