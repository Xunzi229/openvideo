package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode
import kotlin.math.max

/**
 * Normalized crop rectangle in **display-oriented** source space (0..1).
 * Used for portrait canvases with a centered landscape band, or the inverse.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f) {
            "Crop rect components must stay within [0, 1]."
        }
        require(right > left && bottom > top) {
            "Crop rect must have positive width and height."
        }
    }

    val widthFraction: Float get() = right - left
    val heightFraction: Float get() = bottom - top

    companion object {
        val FULL = NormalizedRect(0f, 0f, 1f, 1f)
    }
}

/** Where the full decoded frame is letterboxed inside the player viewport under FIT. */
data class FittedVideoRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/** Active content region inside the viewport (pixels), before optional zoom. */
data class ContentFrameRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/**
 * Extra scale/translation applied on top of [androidx.media3.ui.PlayerView] FIT layout
 * to magnify a source crop window into the viewport on [exo_content_frame].
 */
data class PlayerContentFrameTransform(
    val scale: Float,
    val translationX: Float,
    val translationY: Float
) {
    companion object {
        val IDENTITY = PlayerContentFrameTransform(scale = 1f, translationX = 0f, translationY = 0f)
    }
}

enum class PlayerContentFrameMode {
    /** Standard FIT — no extra transform. */
    FIT_FULL,
    /** Zoom the [NormalizedRect] crop so it fills the viewport. */
    ZOOM_CROP
}

/** Resolved crop + transform for one playback frame. */
data class PlayerContentFramePlaybackPlan(
    val frameMode: ContentFrameMode,
    val crop: NormalizedRect,
    val transformMode: PlayerContentFrameMode
)

/**
 * Pure math for P9-1b: map a source-frame crop window to viewport coordinates and
 * compute scale/translation to fill the player surface.
 */
object PlayerContentFramePolicy {

    private const val FULL_FRAME_EPSILON = 0.001f

    /**
     * Content-frame zoom only composes with FIT-style resize; FILL/CROP/STRETCH own the matrix.
     */
    fun effectiveMode(mode: ContentFrameMode, aspectRatio: AspectRatio): ContentFrameMode =
        if (mode == ContentFrameMode.OFF) {
            ContentFrameMode.OFF
        } else if (allowsContentFrameAdjustment(aspectRatio)) {
            mode
        } else {
            ContentFrameMode.OFF
        }

    fun cropForMode(
        mode: ContentFrameMode,
        sourceWidth: Int,
        sourceHeight: Int
    ): NormalizedRect {
        val aspect = mode.targetAspectRatio ?: return NormalizedRect.FULL
        return centerAspectBandCropRect(sourceWidth, sourceHeight, aspect)
    }

    fun playbackPlan(
        contentFrameMode: ContentFrameMode,
        aspectRatio: AspectRatio,
        sourceWidth: Int,
        sourceHeight: Int
    ): PlayerContentFramePlaybackPlan {
        val effective = effectiveMode(contentFrameMode, aspectRatio)
        if (effective == ContentFrameMode.OFF) {
            return PlayerContentFramePlaybackPlan(
                frameMode = ContentFrameMode.OFF,
                crop = NormalizedRect.FULL,
                transformMode = PlayerContentFrameMode.FIT_FULL
            )
        }
        val crop = cropForMode(effective, sourceWidth, sourceHeight)
        val transformMode = if (isFullFrameCrop(crop)) {
            PlayerContentFrameMode.FIT_FULL
        } else {
            PlayerContentFrameMode.ZOOM_CROP
        }
        return PlayerContentFramePlaybackPlan(
            frameMode = effective,
            crop = crop,
            transformMode = transformMode
        )
    }

    fun allowsContentFrameAdjustment(aspectRatio: AspectRatio): Boolean =
        when (aspectRatio) {
            AspectRatio.FIT,
            AspectRatio.RATIO_4_3,
            AspectRatio.RATIO_16_9 -> true
            AspectRatio.FILL,
            AspectRatio.CROP,
            AspectRatio.STRETCH -> false
        }

    fun isFullFrameCrop(crop: NormalizedRect): Boolean =
        crop.left <= FULL_FRAME_EPSILON &&
            crop.top <= FULL_FRAME_EPSILON &&
            crop.right >= 1f - FULL_FRAME_EPSILON &&
            crop.bottom >= 1f - FULL_FRAME_EPSILON

    /**
     * Heuristic center band for nested content (e.g. 16:9 picture inside 9:16 canvas).
     *
     * @param contentAspectRatio display aspect ratio (width / height) of the inner picture.
     */
    fun centerAspectBandCropRect(
        sourceWidth: Int,
        sourceHeight: Int,
        contentAspectRatio: Float
    ): NormalizedRect {
        if (sourceWidth <= 0 || sourceHeight <= 0) return NormalizedRect.FULL
        if (contentAspectRatio <= 0f || !contentAspectRatio.isFinite()) return NormalizedRect.FULL

        val dar = sourceWidth.toFloat() / sourceHeight
        return if (dar >= 1f) {
            val contentWidthFraction = (contentAspectRatio / dar).coerceIn(0f, 1f)
            if (contentWidthFraction >= 1f - FULL_FRAME_EPSILON) return NormalizedRect.FULL
            val pad = (1f - contentWidthFraction) / 2f
            NormalizedRect(left = pad, top = 0f, right = 1f - pad, bottom = 1f)
        } else {
            val contentHeightFraction = (dar / contentAspectRatio).coerceIn(0f, 1f)
            if (contentHeightFraction >= 1f - FULL_FRAME_EPSILON) return NormalizedRect.FULL
            val pad = (1f - contentHeightFraction) / 2f
            NormalizedRect(left = 0f, top = pad, right = 1f, bottom = 1f - pad)
        }
    }

    /** FIT letterbox of the full source frame inside the viewport. */
    fun fittedVideoRect(
        sourceWidth: Int,
        sourceHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int
    ): FittedVideoRect {
        if (sourceWidth <= 0 || sourceHeight <= 0 || viewportWidth <= 0 || viewportHeight <= 0) {
            return FittedVideoRect(
                left = 0f,
                top = 0f,
                width = viewportWidth.coerceAtLeast(0).toFloat(),
                height = viewportHeight.coerceAtLeast(0).toFloat()
            )
        }

        val sourceAspect = sourceWidth.toFloat() / sourceHeight
        val viewportAspect = viewportWidth.toFloat() / viewportHeight

        return if (sourceAspect > viewportAspect) {
            val width = viewportWidth.toFloat()
            val height = width / sourceAspect
            FittedVideoRect(
                left = 0f,
                top = (viewportHeight - height) / 2f,
                width = width,
                height = height
            )
        } else {
            val height = viewportHeight.toFloat()
            val width = height * sourceAspect
            FittedVideoRect(
                left = (viewportWidth - width) / 2f,
                top = 0f,
                width = width,
                height = height
            )
        }
    }

    /** Maps a normalized source crop into viewport pixel coordinates under FIT. */
    fun contentFrameInViewport(
        fitted: FittedVideoRect,
        crop: NormalizedRect
    ): ContentFrameRect {
        val left = fitted.left + crop.left * fitted.width
        val top = fitted.top + crop.top * fitted.height
        val width = crop.widthFraction * fitted.width
        val height = crop.heightFraction * fitted.height
        return ContentFrameRect(left = left, top = top, width = width, height = height)
    }

    /**
     * Scale + translate so [content] fills [viewportWidth]×[viewportHeight] (center-aligned).
     */
    fun transformToFillViewport(
        viewportWidth: Int,
        viewportHeight: Int,
        content: ContentFrameRect
    ): PlayerContentFrameTransform {
        if (viewportWidth <= 0 || viewportHeight <= 0) return PlayerContentFrameTransform.IDENTITY
        if (content.width <= 0f || content.height <= 0f) return PlayerContentFrameTransform.IDENTITY

        val scale = max(
            viewportWidth / content.width,
            viewportHeight / content.height
        )
        val contentCenterX = content.left + content.width / 2f
        val contentCenterY = content.top + content.height / 2f
        val viewportCenterX = viewportWidth / 2f
        val viewportCenterY = viewportHeight / 2f

        return PlayerContentFrameTransform(
            scale = scale,
            translationX = viewportCenterX - scale * contentCenterX,
            translationY = viewportCenterY - scale * contentCenterY
        )
    }

    fun resolveTransform(
        mode: PlayerContentFrameMode,
        sourceWidth: Int,
        sourceHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int,
        crop: NormalizedRect
    ): PlayerContentFrameTransform {
        if (mode == PlayerContentFrameMode.FIT_FULL || isFullFrameCrop(crop)) {
            return PlayerContentFrameTransform.IDENTITY
        }
        val fitted = fittedVideoRect(sourceWidth, sourceHeight, viewportWidth, viewportHeight)
        val content = contentFrameInViewport(fitted, crop)
        return transformToFillViewport(viewportWidth, viewportHeight, content)
    }
}
