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
        viewportHeight: Int
    ): PlayerContentFrameTransform {
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
            crop = plan.crop
        )
    }
}
