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

object PlayerSmartCropPolicy {

    const val VIEWPORT_FILL_FRACTION = 0.88f
    const val CROP_EXPANSION_FRACTION = 0.25f
    private const val PORTRAIT_CANVAS_RATIO_MAX = 0.75f

    fun quickToggleDecision(
        currentMode: ContentFrameMode,
        currentAspectRatio: AspectRatio,
        sourceWidth: Int,
        sourceHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int
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

        val sourceAspect = sourceWidth.toFloat() / sourceHeight
        val suggestedMode = when {
            sourceAspect <= PORTRAIT_CANVAS_RATIO_MAX -> ContentFrameMode.CENTER_16_9
            else -> null
        } ?: return PlayerSmartCropDecision(contentFrameMode = null)

        return PlayerSmartCropDecision(
            contentFrameMode = suggestedMode,
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
    ): PlayerSmartCropDecision {
        if (restoredMode != ContentFrameMode.CENTER_16_9) {
            return PlayerSmartCropDecision(contentFrameMode = null)
        }
        if (sourceWidth <= 0 || sourceHeight <= 0 || viewportWidth <= viewportHeight) {
            return PlayerSmartCropDecision(contentFrameMode = null)
        }

        val sourceAspect = sourceWidth.toFloat() / sourceHeight
        if (sourceAspect > PORTRAIT_CANVAS_RATIO_MAX) {
            return PlayerSmartCropDecision(contentFrameMode = null)
        }

        return PlayerSmartCropDecision(
            contentFrameMode = restoredMode,
            viewportFillFraction = VIEWPORT_FILL_FRACTION,
            viewportScale = PlayerContentFrameViewportScale.FIT_INSIDE,
            cropExpansionFraction = CROP_EXPANSION_FRACTION
        )
    }
}
