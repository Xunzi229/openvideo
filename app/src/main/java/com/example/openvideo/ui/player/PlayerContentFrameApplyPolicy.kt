package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode

/**
 * Resolves the transform to apply on [androidx.media3.ui.AspectRatioFrameLayout] (exo_content_frame).
 */
object PlayerContentFrameApplyPolicy {

    fun resolveTransform(
        contentFrameMode: ContentFrameMode,
        aspectRatio: AspectRatio,
        sourceWidth: Int,
        sourceHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int,
        viewportFillFraction: Float = 1f,
        viewportScale: PlayerContentFrameViewportScale = PlayerContentFrameViewportScale.FILL,
        cropExpansionFraction: Float = 0f
    ): PlayerContentFrameTransform {
        if (viewportWidth <= viewportHeight) {
            return PlayerContentFrameTransform.IDENTITY
        }
        val plan = PlayerContentFramePolicy.playbackPlan(
            contentFrameMode = contentFrameMode,
            aspectRatio = aspectRatio,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight
        )
        return PlayerContentFramePolicy.resolveTransform(
            mode = plan.transformMode,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            crop = plan.crop,
            viewportFillFraction = viewportFillFraction,
            viewportScale = viewportScale,
            cropExpansionFraction = cropExpansionFraction
        )
    }

    fun resolveTransformWithManualZoom(
        contentFrameMode: ContentFrameMode,
        aspectRatio: AspectRatio,
        sourceWidth: Int,
        sourceHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int,
        manualZoom: PlayerVideoZoomState,
        frameWidth: Int,
        frameHeight: Int,
        viewportFillFraction: Float = 1f,
        viewportScale: PlayerContentFrameViewportScale = PlayerContentFrameViewportScale.FILL,
        cropExpansionFraction: Float = 0f
    ): PlayerContentFrameTransform {
        val base = resolveTransform(
            contentFrameMode = contentFrameMode,
            aspectRatio = aspectRatio,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            viewportFillFraction = viewportFillFraction,
            viewportScale = viewportScale,
            cropExpansionFraction = cropExpansionFraction
        )
        if (!PlayerVideoZoomPolicy.allowsManualZoom(aspectRatio) ||
            !PlayerVideoZoomPolicy.isActive(manualZoom)
        ) {
            return base
        }
        return PlayerVideoZoomPolicy.composeTransform(
            base = base,
            manual = manualZoom,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        )
    }
}
