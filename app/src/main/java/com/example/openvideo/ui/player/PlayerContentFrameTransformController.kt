package com.example.openvideo.ui.player

import android.content.Intent
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as Media3UiR
import com.example.openvideo.core.prefs.ContentFrameMode
import com.example.openvideo.core.prefs.PlayerPrefs

@OptIn(UnstableApi::class)
class PlayerContentFrameTransformController(
    private val playerPrefs: PlayerPrefs,
    private val viewModel: PlayerViewModel,
    private val playerViewProvider: () -> PlayerView,
    private val intentProvider: () -> Intent,
    private val manualVideoZoomProvider: () -> PlayerVideoZoomState,
    private val onManualVideoZoomChanged: (PlayerVideoZoomState) -> Unit,
    private val smartCropStateProvider: () -> PlayerSmartCropTransformState
) {
    private var transformRetryPosted = false
    var baseTransform: PlayerContentFrameTransform = PlayerContentFrameTransform.IDENTITY
        private set

    @OptIn(UnstableApi::class)
    fun applyAspectRatio(
        width: Int? = null,
        height: Int? = null,
        pixelWidthHeightRatio: Float? = null,
        unappliedRotationDegrees: Int? = null
    ) {
        val playerView = playerViewProvider()
        val contentFrame = playerView.findViewById<AspectRatioFrameLayout>(Media3UiR.id.exo_content_frame)
            ?: return
        val frameSize = sourceSize(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
        val ratio = PlayerVideoLayoutPolicy.contentAspectRatio(
            preferredAspectRatio = playerPrefs.aspectRatio,
            width = frameSize.width,
            height = frameSize.height,
            pixelWidthHeightRatio = pixelWidthHeightRatio ?: viewModel.player?.videoSize?.pixelWidthHeightRatio ?: 1f,
            unappliedRotationDegrees = unappliedRotationDegrees
                ?: viewModel.player?.videoSize?.unappliedRotationDegrees
                ?: 0
        )
        contentFrame.setAspectRatio(ratio)
    }

    @OptIn(UnstableApi::class)
    @Suppress("DEPRECATION")
    fun applyTransform(
        width: Int? = null,
        height: Int? = null,
        pixelWidthHeightRatio: Float? = null,
        unappliedRotationDegrees: Int? = null
    ) {
        val playerView = playerViewProvider()
        if (playerView.width <= 0 || playerView.height <= 0) {
            playerView.post {
                applyTransform(
                    width = width,
                    height = height,
                    pixelWidthHeightRatio = pixelWidthHeightRatio,
                    unappliedRotationDegrees = unappliedRotationDegrees
                )
            }
            return
        }
        val contentFrame = playerView.findViewById<AspectRatioFrameLayout>(Media3UiR.id.exo_content_frame)
            ?: return
        val frameSize = sourceSize(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
        if (frameSize.width <= 0 || frameSize.height <= 0) {
            resetTransform(contentFrame)
            scheduleTransformRetry(
                width = width,
                height = height,
                pixelWidthHeightRatio = pixelWidthHeightRatio,
                unappliedRotationDegrees = unappliedRotationDegrees
            )
            return
        }
        transformRetryPosted = false
        val landscapeViewport = playerView.width > playerView.height
        val smartCropState = smartCropStateProvider()
        val smartCropActive =
            (smartCropState.viewportScale != null || smartCropState.transformOverride != null) && landscapeViewport
        val activeSmartCropTransformOverride = smartCropState.transformOverride.takeIf { smartCropActive }
        val transformContentFrameMode = if (!landscapeViewport) {
            ContentFrameMode.OFF
        } else if (smartCropState.contentFrameMode == null && frameSize.width < frameSize.height) {
            ContentFrameMode.OFF
        } else {
            smartCropState.contentFrameMode ?: playerPrefs.contentFrameMode
        }
        val restoredSmartCropDecision = if (!smartCropActive && landscapeViewport) {
            PlayerSmartCropPolicy.restoredViewportDecision(
                restoredMode = transformContentFrameMode,
                sourceWidth = frameSize.width,
                sourceHeight = frameSize.height,
                viewportWidth = playerView.width,
                viewportHeight = playerView.height
            )
        } else {
            PlayerSmartCropDecision(contentFrameMode = null)
        }
        val transformViewportFillFraction = if (smartCropActive) {
            smartCropState.viewportFillFraction ?: PlayerSmartCropPolicy.VIEWPORT_FILL_FRACTION
        } else {
            restoredSmartCropDecision.viewportFillFraction ?: 1f
        }
        val transformViewportScale = if (smartCropActive) {
            smartCropState.viewportScale ?: PlayerContentFrameViewportScale.FILL
        } else {
            restoredSmartCropDecision.viewportScale ?: PlayerContentFrameViewportScale.FILL
        }
        val transformCropExpansionFraction = if (smartCropActive) {
            smartCropState.cropExpansionFraction
        } else {
            restoredSmartCropDecision.cropExpansionFraction
        }
        baseTransform = activeSmartCropTransformOverride
            ?: PlayerContentFrameApplyPolicy.resolveTransform(
                contentFrameMode = transformContentFrameMode,
                aspectRatio = playerPrefs.aspectRatio,
                sourceWidth = frameSize.width,
                sourceHeight = frameSize.height,
                viewportWidth = playerView.width,
                viewportHeight = playerView.height,
                viewportFillFraction = transformViewportFillFraction,
                viewportScale = transformViewportScale,
                cropExpansionFraction = transformCropExpansionFraction
            )
        var manualVideoZoom = manualVideoZoomProvider()
        if (PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio)) {
            val (panX, panY) = PlayerVideoZoomPolicy.clampPanOffset(
                panX = manualVideoZoom.panX,
                panY = manualVideoZoom.panY,
                baseScale = baseTransform.scale,
                manualScale = manualVideoZoom.scale,
                frameWidth = contentFrame.width,
                frameHeight = contentFrame.height,
                viewportWidth = playerView.width,
                viewportHeight = playerView.height
            )
            manualVideoZoom = manualVideoZoom.copy(panX = panX, panY = panY)
            onManualVideoZoomChanged(manualVideoZoom)
        }
        val transform = activeSmartCropTransformOverride
            ?: PlayerContentFrameApplyPolicy.resolveTransformWithManualZoom(
                contentFrameMode = transformContentFrameMode,
                aspectRatio = playerPrefs.aspectRatio,
                sourceWidth = frameSize.width,
                sourceHeight = frameSize.height,
                viewportWidth = playerView.width,
                viewportHeight = playerView.height,
                manualZoom = manualVideoZoom,
                frameWidth = contentFrame.width,
                frameHeight = contentFrame.height,
                viewportFillFraction = transformViewportFillFraction,
                viewportScale = transformViewportScale,
                cropExpansionFraction = transformCropExpansionFraction
            )
        applyTransformToFrame(
            contentFrame = contentFrame,
            frameSize = frameSize,
            playerView = playerView,
            transform = transform,
            transformContentFrameMode = transformContentFrameMode,
            transformViewportFillFraction = transformViewportFillFraction,
            transformViewportScale = transformViewportScale,
            transformCropExpansionFraction = transformCropExpansionFraction
        )
    }

    fun sourceSize(
        width: Int?,
        height: Int?,
        pixelWidthHeightRatio: Float?,
        unappliedRotationDegrees: Int?
    ): DisplayFrameSize {
        val vs = viewModel.player?.videoSize
        val intent = intentProvider()
        val rawWidth = width?.takeIf { it > 0 }
            ?: vs?.width?.takeIf { it > 0 }
            ?: intent.getIntExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, 0)
        val rawHeight = height?.takeIf { it > 0 }
            ?: vs?.height?.takeIf { it > 0 }
            ?: intent.getIntExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, 0)
        return PlayerVideoLayoutPolicy.displayFrameSize(
            width = rawWidth,
            height = rawHeight,
            pixelWidthHeightRatio = pixelWidthHeightRatio ?: vs?.pixelWidthHeightRatio ?: 1f,
            unappliedRotationDegrees = unappliedRotationDegrees ?: vs?.unappliedRotationDegrees ?: 0
        )
    }

    fun videoRenderView(): View? =
        playerViewProvider().videoSurfaceView

    private fun scheduleTransformRetry(
        width: Int?,
        height: Int?,
        pixelWidthHeightRatio: Float?,
        unappliedRotationDegrees: Int?
    ) {
        if (transformRetryPosted) return
        transformRetryPosted = true
        playerViewProvider().post {
            transformRetryPosted = false
            applyTransform(
                width = width,
                height = height,
                pixelWidthHeightRatio = pixelWidthHeightRatio,
                unappliedRotationDegrees = unappliedRotationDegrees
            )
        }
    }

    private fun resetTransform(contentFrame: AspectRatioFrameLayout) {
        contentFrame.pivotX = PlayerContentFrameResetPolicy.PIVOT
        contentFrame.pivotY = PlayerContentFrameResetPolicy.PIVOT
        contentFrame.scaleX = PlayerContentFrameResetPolicy.SCALE
        contentFrame.scaleY = PlayerContentFrameResetPolicy.SCALE
        contentFrame.translationX = PlayerContentFrameResetPolicy.TRANSLATION
        contentFrame.translationY = PlayerContentFrameResetPolicy.TRANSLATION
    }

    private fun applyTransformToFrame(
        contentFrame: AspectRatioFrameLayout,
        frameSize: DisplayFrameSize,
        playerView: PlayerView,
        transform: PlayerContentFrameTransform,
        transformContentFrameMode: ContentFrameMode,
        transformViewportFillFraction: Float,
        transformViewportScale: PlayerContentFrameViewportScale,
        transformCropExpansionFraction: Float
    ) {
        contentFrame.pivotX = PlayerContentFrameResetPolicy.PIVOT
        contentFrame.pivotY = PlayerContentFrameResetPolicy.PIVOT
        contentFrame.scaleX = transform.scale
        contentFrame.scaleY = transform.scale
        contentFrame.translationX = transform.translationX
        contentFrame.translationY = transform.translationY
        if (com.example.openvideo.BuildConfig.DEBUG) {
            android.util.Log.d(
                TAG_CONTENT_FRAME,
                "mode=${transformContentFrameMode.key} prefMode=${playerPrefs.contentFrameMode.key} aspect=${playerPrefs.aspectRatio.key} " +
                    "frame=${frameSize.width}x${frameSize.height} viewport=${playerView.width}x${playerView.height} " +
                    "viewportFill=$transformViewportFillFraction viewportScale=$transformViewportScale cropExpand=$transformCropExpansionFraction " +
                    "scale=${transform.scale} tx=${transform.translationX} ty=${transform.translationY}"
            )
        }
    }

    private companion object {
        private const val TAG_CONTENT_FRAME = "OVContentFrame"
    }
}

data class PlayerSmartCropTransformState(
    val contentFrameMode: ContentFrameMode?,
    val transformOverride: PlayerContentFrameTransform?,
    val viewportFillFraction: Float?,
    val viewportScale: PlayerContentFrameViewportScale?,
    val cropExpansionFraction: Float
)
