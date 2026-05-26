package com.example.openvideo.ui.player

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as Media3UiR
import com.example.openvideo.R
import com.example.openvideo.core.prefs.ContentFrameMode
import com.example.openvideo.core.prefs.PlayerPrefs

@OptIn(UnstableApi::class)
class PlayerSmartCropController(
    private val activity: AppCompatActivity,
    private val playerPrefs: PlayerPrefs,
    private val viewModel: PlayerViewModel,
    private val handler: Handler,
    private val hideControlsRunnable: Runnable,
    private val playerViewProvider: () -> PlayerView,
    private val controlsContainerProvider: () -> View,
    private val contentFrameSourceSizeProvider: (Int?, Int?, Float?, Int?) -> DisplayFrameSize,
    private val videoRenderViewProvider: () -> View?,
    private val onControlsVisibleChanged: (Boolean) -> Unit,
    private val onApplyControlVisibility: () -> Unit,
    private val onResetManualVideoZoom: () -> Unit,
    private val onApplyDisplaySettings: () -> Unit,
    private val onScheduleHideControls: () -> Unit
) {
    var contentFrameMode: ContentFrameMode? = null
        private set
    var transformOverride: PlayerContentFrameTransform? = null
        private set
    var viewportFillFraction: Float? = null
        private set
    var viewportScale: PlayerContentFrameViewportScale? = null
        private set
    var cropExpansionFraction: Float = 0f
        private set

    private var smartCropToast: Toast? = null

    fun handleQuickToggle() {
        val playerView = playerViewProvider()
        logSmartCrop(
            "smartCropClick activeMode=${contentFrameMode?.key} override=$transformOverride " +
                "viewport=${playerView.width}x${playerView.height}"
        )
        if (contentFrameMode != null || transformOverride != null) {
            clearSession()
            onApplyDisplaySettings()
            logSmartCrop("smartCropClick toggledOff")
            showSmartCropToast(R.string.player_smart_crop_off)
            onScheduleHideControls()
            return
        }
        cancelSmartCropToast()
        hideControlsForCapture()
        playerView.postDelayed({
            captureRenderBounds { renderBounds ->
                logSmartCrop("smartCropRenderBounds $renderBounds")
                if (renderBounds != null && applyBounds(renderBounds)) {
                    showSmartCropToast(R.string.player_smart_crop_applied)
                    onScheduleHideControls()
                    return@captureRenderBounds
                }
                captureVisualBounds { bounds ->
                    logSmartCrop("smartCropVisualBounds $bounds")
                    if (bounds != null && applyBounds(bounds)) {
                        showSmartCropToast(R.string.player_smart_crop_applied)
                        onScheduleHideControls()
                        return@captureVisualBounds
                    }
                    captureBlackBorders { blackBorders ->
                        logSmartCrop("smartCropBlackBorders $blackBorders")
                        if (blackBorders == null) {
                            showSmartCropToast(R.string.player_smart_crop_unavailable)
                            return@captureBlackBorders
                        }
                        applyQuickToggle(blackBorders)
                    }
                }
            }
        }, SMART_CROP_CAPTURE_DELAY_MS)
    }

    fun clearSession() {
        contentFrameMode = null
        transformOverride = null
        viewportFillFraction = null
        viewportScale = null
        cropExpansionFraction = 0f
        onResetManualVideoZoom()
    }

    private fun showSmartCropToast(messageRes: Int) {
        cancelSmartCropToast()
        smartCropToast = Toast.makeText(activity, messageRes, Toast.LENGTH_SHORT)
        smartCropToast?.show()
    }

    private fun cancelSmartCropToast() {
        smartCropToast?.cancel()
        smartCropToast = null
    }

    private fun hideControlsForCapture() {
        handler.removeCallbacks(hideControlsRunnable)
        onControlsVisibleChanged(false)
        val controlsContainer = controlsContainerProvider()
        controlsContainer.animate().cancel()
        controlsContainer.alpha = 0f
        controlsContainer.visibility = View.GONE
        onApplyControlVisibility()
    }

    private fun applyBounds(bounds: PlayerSmartCropBlackBorderDetector.ContentBounds): Boolean {
        val playerView = playerViewProvider()
        if (playerView.width <= playerView.height) return false
        val contentFrame = playerView.findViewById<AspectRatioFrameLayout>(Media3UiR.id.exo_content_frame)
            ?: return false
        val playerLocation = IntArray(2)
        val contentFrameLocation = IntArray(2)
        playerView.getLocationOnScreen(playerLocation)
        contentFrame.getLocationOnScreen(contentFrameLocation)
        val contentFrameLeft = (contentFrameLocation[0] - playerLocation[0]).toFloat()
        val contentFrameTop = (contentFrameLocation[1] - playerLocation[1]).toFloat()
        val borders = PlayerSmartCropBlackBorderDetector.detectFromContentRect(
            viewportWidth = playerView.width,
            viewportHeight = playerView.height,
            contentLeft = bounds.left,
            contentTop = bounds.top,
            contentRight = bounds.right,
            contentBottom = bounds.bottom
        )
        if (!borders.hasAllEdges) {
            logSmartCrop(
                "smartCropBoundsRejected bounds=${bounds.left},${bounds.top}-${bounds.right},${bounds.bottom} " +
                    "borders=$borders viewport=${playerView.width}x${playerView.height}"
            )
            return false
        }
        contentFrameMode = null
        viewportFillFraction = PlayerSmartCropPolicy.VIEWPORT_FILL_FRACTION
        viewportScale = PlayerContentFrameViewportScale.FIT_INSIDE
        cropExpansionFraction = 0f
        transformOverride = PlayerContentFramePolicy.transformToFillViewport(
            viewportWidth = playerView.width,
            viewportHeight = playerView.height,
            content = ContentFrameRect(
                left = bounds.left - contentFrameLeft,
                top = bounds.top - contentFrameTop,
                width = bounds.width.toFloat(),
                height = bounds.height.toFloat()
            ),
            viewportFillFraction = PlayerSmartCropPolicy.VIEWPORT_FILL_FRACTION,
            viewportScale = PlayerContentFrameViewportScale.FIT_INSIDE,
            contentFrameLeft = contentFrameLeft,
            contentFrameTop = contentFrameTop
        )
        logSmartCrop(
            "smartCropBoundsApplied bounds=${bounds.left},${bounds.top}-${bounds.right},${bounds.bottom} " +
                "viewport=${playerView.width}x${playerView.height} contentFrame=$contentFrameLeft,$contentFrameTop " +
                "transform=$transformOverride"
        )
        onResetManualVideoZoom()
        onApplyDisplaySettings()
        return true
    }

    private fun logSmartCrop(message: String) {
        if (com.example.openvideo.BuildConfig.DEBUG) {
            android.util.Log.d(TAG_CONTENT_FRAME, message)
        }
    }

    private fun applyQuickToggle(blackBorders: PlayerSmartCropBlackBorders) {
        val playerView = playerViewProvider()
        val frameSize = contentFrameSourceSizeProvider(null, null, null, null)
        val activeSmartCropMode = contentFrameMode
        val decision = PlayerSmartCropPolicy.quickToggleDecision(
            currentMode = activeSmartCropMode ?: ContentFrameMode.OFF,
            currentAspectRatio = playerPrefs.aspectRatio,
            sourceWidth = frameSize.width,
            sourceHeight = frameSize.height,
            viewportWidth = playerView.width,
            viewportHeight = playerView.height,
            blackBorders = blackBorders
        )
        val nextMode = decision.contentFrameMode
        if (nextMode == null) {
            showSmartCropToast(R.string.player_smart_crop_unavailable)
            return
        }

        decision.aspectRatioOverride?.let { ratio ->
            playerPrefs.aspectRatio = ratio
            viewModel.setAspectRatio(ratio)
        }
        contentFrameMode = decision.contentFrameMode.takeIf { it != ContentFrameMode.OFF }
        transformOverride = null
        viewportFillFraction = decision.viewportFillFraction
        viewportScale = decision.viewportScale
        cropExpansionFraction = decision.cropExpansionFraction
        onResetManualVideoZoom()
        onApplyDisplaySettings()
        showSmartCropToast(
            if (nextMode == ContentFrameMode.OFF) {
                R.string.player_smart_crop_off
            } else {
                R.string.player_smart_crop_applied
            }
        )
        onScheduleHideControls()
    }

    private fun captureVisualBounds(
        callback: (PlayerSmartCropBlackBorderDetector.ContentBounds?) -> Unit
    ) {
        val playerView = playerViewProvider()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            playerView.width <= 0 ||
            playerView.height <= 0
        ) {
            callback(null)
            return
        }
        val location = IntArray(2)
        playerView.getLocationInWindow(location)
        val sourceRect = Rect(
            location[0],
            location[1],
            location[0] + playerView.width,
            location[1] + playerView.height
        )
        val bitmap = Bitmap.createBitmap(playerView.width, playerView.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(
            activity.window,
            sourceRect,
            bitmap,
            { result ->
                if (result == PixelCopy.SUCCESS) {
                    callback(detectContentBounds(bitmap))
                } else {
                    callback(null)
                }
                bitmap.recycle()
            },
            Handler(Looper.getMainLooper())
        )
    }

    private fun captureRenderBounds(
        callback: (PlayerSmartCropBlackBorderDetector.ContentBounds?) -> Unit
    ) {
        val videoView = videoRenderViewProvider()
        when (videoView) {
            is TextureView -> {
                val bitmap = videoView.bitmap ?: run {
                    callback(null)
                    return
                }
                val bounds = detectContentBounds(bitmap)?.offsetBoundsFrom(videoView)
                callback(bounds)
                bitmap.recycle()
            }
            is SurfaceView -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    videoView.width <= 0 ||
                    videoView.height <= 0
                ) {
                    callback(null)
                    return
                }
                val bitmap = Bitmap.createBitmap(videoView.width, videoView.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(
                    videoView,
                    bitmap,
                    { result ->
                        val bounds = if (result == PixelCopy.SUCCESS) {
                            detectContentBounds(bitmap)?.offsetBoundsFrom(videoView)
                        } else {
                            null
                        }
                        callback(bounds)
                        bitmap.recycle()
                    },
                    Handler(Looper.getMainLooper())
                )
            }
            else -> callback(null)
        }
    }

    private fun PlayerSmartCropBlackBorderDetector.ContentBounds.offsetBoundsFrom(
        child: View
    ): PlayerSmartCropBlackBorderDetector.ContentBounds {
        val playerView = playerViewProvider()
        val playerLocation = IntArray(2)
        val childLocation = IntArray(2)
        playerView.getLocationOnScreen(playerLocation)
        child.getLocationOnScreen(childLocation)
        val dx = childLocation[0] - playerLocation[0]
        val dy = childLocation[1] - playerLocation[1]
        return PlayerSmartCropBlackBorderDetector.ContentBounds(
            left = left + dx,
            top = top + dy,
            right = right + dx,
            bottom = bottom + dy
        )
    }

    private fun captureBlackBorders(callback: (PlayerSmartCropBlackBorders?) -> Unit) {
        val videoView = videoRenderViewProvider()
        val geometryBorders = detectGeometryBlackBorders(videoView)
        if (geometryBorders?.hasAllEdges == true) {
            callback(geometryBorders)
            return
        }
        when (videoView) {
            is TextureView -> {
                val bitmap = videoView.bitmap ?: run {
                    callback(null)
                    return
                }
                val pixelBorders = detectBlackBorders(bitmap)
                callback(pixelBorders)
                bitmap.recycle()
            }
            is SurfaceView -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    videoView.width <= 0 ||
                    videoView.height <= 0
                ) {
                    callback(null)
                    return
                }
                val bitmap = Bitmap.createBitmap(videoView.width, videoView.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(
                    videoView,
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            val pixelBorders = detectBlackBorders(bitmap)
                            callback(pixelBorders)
                        } else {
                            callback(null)
                        }
                        bitmap.recycle()
                    },
                    Handler(Looper.getMainLooper())
                )
            }
            else -> callback(null)
        }
    }

    private fun detectGeometryBlackBorders(videoView: View?): PlayerSmartCropBlackBorders? {
        val playerView = playerViewProvider()
        if (videoView == null || playerView.width <= 0 || playerView.height <= 0) return null
        val playerLocation = IntArray(2)
        val contentLocation = IntArray(2)
        playerView.getLocationOnScreen(playerLocation)
        videoView.getLocationOnScreen(contentLocation)

        val contentLeft = contentLocation[0] - playerLocation[0]
        val contentTop = contentLocation[1] - playerLocation[1]
        val contentRight = contentLeft + videoView.width
        val contentBottom = contentTop + videoView.height
        return PlayerSmartCropBlackBorderDetector.detectFromContentRect(
            viewportWidth = playerView.width,
            viewportHeight = playerView.height,
            contentLeft = contentLeft,
            contentTop = contentTop,
            contentRight = contentRight,
            contentBottom = contentBottom
        )
    }

    private fun detectBlackBorders(bitmap: Bitmap): PlayerSmartCropBlackBorders {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return PlayerSmartCropBlackBorderDetector.detect(width, height) { x, y ->
            isBlackPixel(pixels[y * width + x])
        }
    }

    private fun detectContentBounds(
        bitmap: Bitmap
    ): PlayerSmartCropBlackBorderDetector.ContentBounds? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return PlayerSmartCropBlackBorderDetector.detectContentBounds(width, height) { x, y ->
            isBlackPixel(pixels[y * width + x])
        }
    }

    private fun isBlackPixel(pixel: Int): Boolean =
        PlayerSmartCropPixelPolicy.isBlackArgb(pixel)

    private companion object {
        private const val TAG_CONTENT_FRAME = "OVContentFrame"
        private const val SMART_CROP_CAPTURE_DELAY_MS = 120L
    }
}
