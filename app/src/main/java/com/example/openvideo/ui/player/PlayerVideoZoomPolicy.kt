package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import kotlin.math.abs

object PlayerVideoZoomPolicy {

    const val MIN_SCALE = 1f
    const val MAX_SCALE = 4f
    private const val ACTIVE_EPSILON = 0.01f

    fun allowsManualZoom(aspectRatio: AspectRatio): Boolean =
        PlayerContentFramePolicy.allowsContentFrameAdjustment(aspectRatio)

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    fun isActive(manual: PlayerVideoZoomState): Boolean =
        manual.scale > MIN_SCALE + ACTIVE_EPSILON ||
            abs(manual.panX) > ACTIVE_EPSILON ||
            abs(manual.panY) > ACTIVE_EPSILON

    fun applyScaleFactor(currentScale: Float, scaleFactor: Float): Float =
        clampScale(currentScale * scaleFactor)

    /**
     * With pivot at (0,0), scaling pushes content toward bottom-right. This offset keeps the
     * content center anchored to the viewport center as [manualScale] changes.
     */
    fun centerAnchorTranslation(
        baseScale: Float,
        manualScale: Float,
        frameWidth: Int,
        frameHeight: Int
    ): Pair<Float, Float> {
        if (manualScale <= MIN_SCALE + ACTIVE_EPSILON) return 0f to 0f
        if (frameWidth <= 0 || frameHeight <= 0) return 0f to 0f
        val compX = frameWidth * baseScale * (1f - manualScale) / 2f
        val compY = frameHeight * baseScale * (1f - manualScale) / 2f
        return compX to compY
    }

    fun clampPanOffset(
        panX: Float,
        panY: Float,
        baseScale: Float,
        manualScale: Float,
        frameWidth: Int,
        frameHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int
    ): Pair<Float, Float> {
        if (viewportWidth <= 0 || viewportHeight <= 0) return panX to panY
        if (manualScale <= MIN_SCALE + ACTIVE_EPSILON) return 0f to 0f
        val totalScale = baseScale * manualScale
        val maxPanX = ((frameWidth * totalScale - viewportWidth) / 2f).coerceAtLeast(0f)
        val maxPanY = ((frameHeight * totalScale - viewportHeight) / 2f).coerceAtLeast(0f)
        return panX.coerceIn(-maxPanX, maxPanX) to panY.coerceIn(-maxPanY, maxPanY)
    }

    fun panFromDrag(
        anchorPanX: Float,
        anchorPanY: Float,
        dragDx: Float,
        dragDy: Float,
        baseScale: Float,
        manualScale: Float,
        frameWidth: Int,
        frameHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int
    ): Pair<Float, Float> =
        clampPanOffset(
            panX = anchorPanX + dragDx,
            panY = anchorPanY + dragDy,
            baseScale = baseScale,
            manualScale = manualScale,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        )

    fun composeTransform(
        base: PlayerContentFrameTransform,
        manual: PlayerVideoZoomState,
        frameWidth: Int,
        frameHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int
    ): PlayerContentFrameTransform {
        val manualScale = clampScale(manual.scale)
        val (centerX, centerY) = centerAnchorTranslation(
            baseScale = base.scale,
            manualScale = manualScale,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
        val (panX, panY) = clampPanOffset(
            panX = manual.panX,
            panY = manual.panY,
            baseScale = base.scale,
            manualScale = manualScale,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        )
        return PlayerContentFrameTransform(
            scale = base.scale * manualScale,
            translationX = base.translationX + centerX + panX,
            translationY = base.translationY + centerY + panY
        )
    }
}
