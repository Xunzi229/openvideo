package com.example.openvideo.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.example.openvideo.core.prefs.DoubleTapAction
import com.example.openvideo.core.prefs.GestureAction
import com.example.openvideo.core.prefs.PlayerPrefs
import kotlin.math.abs

class PlayerGestureController(
    private val activity: PlayerActivity,
    private val playerPrefs: PlayerPrefs,
    private val viewModel: PlayerViewModel,
    private val handler: Handler,
    private val gestureOverlayProvider: () -> View,
    private val seekIndicatorProvider: () -> TextView,
    private val brightnessIndicatorProvider: () -> View,
    private val brightnessProgressProvider: () -> ProgressBar,
    private val volumeIndicatorProvider: () -> View,
    private val volumeProgressProvider: () -> ProgressBar,
    private val currentBrightnessProvider: () -> Float,
    private val onCurrentBrightnessChanged: (Float) -> Unit,
    private val currentVolumeProvider: () -> Float,
    private val onCurrentVolumeChanged: (Float) -> Unit,
    private val manualVideoZoomProvider: () -> PlayerVideoZoomState,
    private val onManualVideoZoomChanged: (PlayerVideoZoomState) -> Unit,
    private val baseContentFrameScaleProvider: () -> Float,
    private val playerViewportWidthProvider: () -> Int,
    private val playerViewportHeightProvider: () -> Int,
    private val isScreenLockedProvider: () -> Boolean,
    private val controlsVisibleProvider: () -> Boolean,
    private val onShowControls: () -> Unit,
    private val onHideControls: () -> Unit,
    private val onShowLockedControls: () -> Unit,
    private val onApplyContentFrameTransform: () -> Unit,
    private val onSetWindowBrightness: (Float) -> Unit,
    private val onFinishPlayer: () -> Unit
) {
    private lateinit var gestureDetector: GestureDetector
    private lateinit var pinchZoomDetector: ScaleGestureDetector
    private var pendingSeekTarget: Long? = null
    private var seekGestureAnchorPositionMs: Long? = null
    private var isLongPressing = false
    private var doubleTapSeekState: DoubleTapSeekState? = null
    private var doubleTapSeekAnchorPositionMs: Long? = null
    private var keepGestureHudAfterActionUp = false
    private var brightnessGestureAnchor = 0.5f
    private var volumeGestureAnchor = 0.5f
    private val hideGestureHudRunnable = Runnable { hideGestureHud() }

    @SuppressLint("ClickableViewAccessibility")
    fun init() {
        pinchZoomDetector = ScaleGestureDetector(
            activity,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    return PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio)
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val manualVideoZoom = manualVideoZoomProvider().copy(
                        scale = PlayerVideoZoomPolicy.applyScaleFactor(
                            currentScale = manualVideoZoomProvider().scale,
                            scaleFactor = detector.scaleFactor
                        )
                    )
                    onManualVideoZoomChanged(manualVideoZoom)
                    onApplyContentFrameTransform()
                    showGestureHud(PlayerGestureHudPolicy.zoom(manualVideoZoom.scale), autoHide = false)
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    handler.postDelayed(hideGestureHudRunnable, PlayerGestureHudDisplayPolicy.AUTO_HIDE_DELAY_MS)
                }
            }
        )

        gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isScreenLockedProvider()) {
                    if (PlayerLockedControlsPolicy.allows(
                            PlayerLockedInteraction.REVEAL_LOCKED_CHROME,
                            isScreenLockedProvider()
                        )
                    ) {
                        onShowLockedControls()
                    }
                    return true
                }
                if (controlsVisibleProvider()) onHideControls() else onShowControls()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!PlayerLockedControlsPolicy.allows(
                        PlayerLockedInteraction.GESTURE_PLAYBACK,
                        isScreenLockedProvider()
                    )
                ) {
                    return true
                }
                val manualVideoZoom = manualVideoZoomProvider()
                if (PlayerVideoZoomGesturePolicy.doubleTapResetsZoom(
                        zoomAllowed = PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio),
                        manual = manualVideoZoom
                    )
                ) {
                    resetManualVideoZoom(showHud = true)
                    return true
                }
                when (playerPrefs.doubleTapAction) {
                    DoubleTapAction.PLAY_PAUSE -> viewModel.togglePlayPause()
                    DoubleTapAction.FORWARD -> handleDoubleTapSeek(PlayerSwipeSide.RIGHT)
                    DoubleTapAction.BACKWARD -> handleDoubleTapSeek(PlayerSwipeSide.LEFT)
                    DoubleTapAction.NONE -> {}
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!PlayerLockedControlsPolicy.allows(
                        PlayerLockedInteraction.GESTURE_PLAYBACK,
                        isScreenLockedProvider()
                    )
                ) {
                    return
                }
                val decision = PlayerLongPressPolicy.onPress(
                    action = playerPrefs.longPressAction,
                    requestedSpeed = playerPrefs.longPressSpeed,
                    restoreSpeed = playerPrefs.speed
                )
                isLongPressing = decision.startLongPress
                decision.targetSpeed?.let { speed ->
                    viewModel.setSpeed(speed)
                    showGestureHud(PlayerGestureHudPolicy.speed(speed), autoHide = false)
                }
            }
        })

        attachTouchListener()
    }

    fun resetForNewVideo(reset: PlayerVideoSwitchReset) {
        pendingSeekTarget = reset.pendingSeekTarget
        seekGestureAnchorPositionMs = reset.seekGestureAnchorPositionMs
        doubleTapSeekState = reset.doubleTapSeekState
        doubleTapSeekAnchorPositionMs = reset.doubleTapSeekAnchorPositionMs
        keepGestureHudAfterActionUp = reset.keepGestureHudAfterActionUp
        hideGestureHud()
    }

    fun resetManualVideoZoom(showHud: Boolean = false) {
        onManualVideoZoomChanged(PlayerVideoZoomState.IDENTITY)
        onApplyContentFrameTransform()
        if (showHud) {
            showGestureHud(PlayerGestureHudPolicy.zoom(PlayerVideoZoomPolicy.MIN_SCALE))
        }
    }

    fun hideGestureHud() {
        handler.removeCallbacks(hideGestureHudRunnable)
        seekIndicatorProvider().visibility = View.GONE
        brightnessIndicatorProvider().visibility = View.GONE
        volumeIndicatorProvider().visibility = View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachTouchListener() {
        var startX = 0f
        var startY = 0f
        var isHorizontalSwipe = false
        var isVerticalSwipe = false
        var isEdgeSwipe = false
        var swipeSide = PlayerSwipeSide.NONE
        var zoomPanStartX = 0f
        var zoomPanStartY = 0f
        var zoomPanAnchorX = 0f
        var zoomPanAnchorY = 0f
        var zoomPanMoved = false
        var verticalLevelGestureAllowed = false
        val gestureSlop = gestureSlopPx()
        val zoomAllowed = { PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio) }

        gestureOverlayProvider().setOnTouchListener { _, event ->
            if (isScreenLockedProvider()) {
                val decision = PlayerLockGesturePolicy.onTouch(
                    isLocked = true,
                    action = PlayerTouchActionPolicy.fromMotionActionMasked(event.actionMasked)
                )
                if (decision.revealLockedControls) onShowLockedControls()
                return@setOnTouchListener decision.consumeTouch
            }

            if (zoomAllowed()) {
                pinchZoomDetector.onTouchEvent(event)
            }
            if (zoomAllowed() && (event.pointerCount >= 2 || pinchZoomDetector.isInProgress)) {
                if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                    isHorizontalSwipe = false
                    isVerticalSwipe = false
                    pendingSeekTarget = null
                    seekGestureAnchorPositionMs = null
                }
                return@setOnTouchListener true
            }

            val manualVideoZoom = manualVideoZoomProvider()
            if (PlayerVideoZoomGesturePolicy.interceptsSingleFingerGestures(zoomAllowed(), manualVideoZoom)) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        zoomPanStartX = event.x
                        zoomPanStartY = event.y
                        zoomPanAnchorX = manualVideoZoom.panX
                        zoomPanAnchorY = manualVideoZoom.panY
                        zoomPanMoved = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - zoomPanStartX
                        val dy = event.y - zoomPanStartY
                        if (abs(dx) > gestureSlop || abs(dy) > gestureSlop) {
                            zoomPanMoved = true
                            val (panX, panY) = PlayerVideoZoomPolicy.panFromDrag(
                                anchorPanX = zoomPanAnchorX,
                                anchorPanY = zoomPanAnchorY,
                                dragDx = dx,
                                dragDy = dy,
                                baseScale = baseContentFrameScaleProvider(),
                                manualScale = manualVideoZoomProvider().scale,
                                frameWidth = playerViewportWidthProvider(),
                                frameHeight = playerViewportHeightProvider(),
                                viewportWidth = playerViewportWidthProvider(),
                                viewportHeight = playerViewportHeightProvider()
                            )
                            onManualVideoZoomChanged(manualVideoZoomProvider().copy(panX = panX, panY = panY))
                            onApplyContentFrameTransform()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!zoomPanMoved) {
                            gestureDetector.onTouchEvent(event)
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> Unit
                }
                return@setOnTouchListener true
            }

            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isHorizontalSwipe = false
                    isVerticalSwipe = false
                    brightnessGestureAnchor = currentBrightnessProvider()
                    volumeGestureAnchor = currentVolumeProvider()
                    pendingSeekTarget = null
                    seekGestureAnchorPositionMs = viewModel.uiState.value.currentPosition
                    isEdgeSwipe = PlayerGesturePolicy.isEdgeSwipe(event.x, activity.resources.displayMetrics.widthPixels)
                    swipeSide = PlayerGesturePolicy.swipeSide(event.x, activity.resources.displayMetrics.widthPixels)
                    verticalLevelGestureAllowed = PlayerGesturePolicy.allowsVerticalLevelGesture(
                        yPx = event.y,
                        screenHeightPx = activity.resources.displayMetrics.heightPixels
                    )
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - startX
                    val dy = event.y - startY

                    if (!isHorizontalSwipe && !isVerticalSwipe) {
                        when (PlayerGesturePolicy.dominantAxis(dx, dy, gestureSlop)) {
                            PlayerSwipeAxis.HORIZONTAL -> isHorizontalSwipe = true
                            PlayerSwipeAxis.VERTICAL -> {
                                if (verticalLevelGestureAllowed) {
                                    isVerticalSwipe = true
                                }
                            }
                            PlayerSwipeAxis.NONE -> {}
                        }
                    }

                    if (isHorizontalSwipe) {
                        handleHorizontalSwipeAction(dx)
                    } else if (isVerticalSwipe) {
                        when (resolveVerticalGestureAction(swipeSide)) {
                            GestureAction.BRIGHTNESS -> handleBrightnessGesture(dy)
                            GestureAction.VOLUME -> handleVolumeGesture(dy)
                            GestureAction.SEEK -> handleVerticalSeekGesture(dy)
                            GestureAction.NONE -> {}
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - startX
                    if (PlayerEdgeSwipeBackPolicy.shouldFinish(
                            edgeSwipeBackEnabled = playerPrefs.edgeSwipeBack,
                            isEdgeSwipe = isEdgeSwipe,
                            isHorizontalSwipe = isHorizontalSwipe,
                            dragDxPx = dx
                        )
                    ) {
                        onFinishPlayer()
                        return@setOnTouchListener true
                    }

                    if (PlayerGesturePolicy.shouldApplyHorizontalSeekOnRelease(
                            isHorizontalSwipe = isHorizontalSwipe,
                            horizontalSwipeAction = playerPrefs.horizontalSwipeAction
                        )
                    ) {
                        applySeekGesture()
                    } else if (isVerticalSwipe) {
                        val verticalAction = resolveVerticalGestureAction(swipeSide)
                        if (PlayerGesturePolicy.shouldApplyVerticalSeekOnRelease(isVerticalSwipe, verticalAction)) {
                            applySeekGesture()
                        }
                    }
                    if (keepGestureHudAfterActionUp) {
                        keepGestureHudAfterActionUp = false
                    } else {
                        hideGestureHud()
                    }

                    releaseLongPressSpeed()
                    seekGestureAnchorPositionMs = null
                }
                MotionEvent.ACTION_CANCEL -> {
                    pendingSeekTarget = null
                    seekGestureAnchorPositionMs = null
                    hideGestureHud()
                    releaseLongPressSpeed()
                }
            }
            true
        }
    }

    private fun releaseLongPressSpeed() {
        val release = PlayerLongPressPolicy.onRelease(
            isLongPressing = isLongPressing,
            restoreSpeed = playerPrefs.speed
        )
        isLongPressing = false
        release.restoreSpeed?.let(viewModel::setSpeed)
    }

    private fun gestureSlopPx(): Int =
        PlayerGesturePolicy.gestureSlopPx(playerPrefs.gestureSensitivity)

    private fun handleHorizontalSwipeAction(dx: Float) {
        PlayerGestureDispatchPolicy.onHorizontalSwipe(playerPrefs.horizontalSwipeAction) {
            handleSeekGesture(dx)
        }
    }

    private fun resolveVerticalGestureAction(side: PlayerSwipeSide): GestureAction =
        PlayerGesturePolicy.verticalGestureAction(
            side = side,
            leftAction = playerPrefs.leftVerticalGesture,
            rightAction = playerPrefs.rightVerticalGesture
        )

    private fun handleDoubleTapSeek(side: PlayerSwipeSide) {
        if (!PlayerGesturePolicy.isValidDoubleTapSeekSide(side)) return

        val intervalMs = PlayerDoubleTapSeekPolicy.intervalMs(playerPrefs.seekInterval)
        val state = viewModel.uiState.value
        val preview = PlayerDoubleTapSeekPolicy.preview(
            previous = doubleTapSeekState,
            tapSide = side,
            intervalMs = intervalMs,
            nowMs = SystemClock.uptimeMillis(),
            anchorPositionMs = doubleTapSeekAnchorPositionMs,
            currentPositionMs = state.currentPosition,
            durationMs = state.duration
        )
        doubleTapSeekState = preview.nextState
        keepGestureHudAfterActionUp = true
        doubleTapSeekAnchorPositionMs = preview.anchorPositionMs

        if (preview.seekable) {
            viewModel.seekTo(preview.targetMs)
        }
        showGestureHud(PlayerGestureHudPolicy.seek(preview.targetMs, state.duration, preview.deltaMs))
    }

    private fun handleSeekGesture(dx: Float) {
        val state = viewModel.uiState.value
        val preview = PlayerSeekGesturePolicy.horizontalPreview(
            anchorPositionMs = seekGestureAnchorPositionMs ?: state.currentPosition,
            dx = dx,
            screenWidthPx = activity.resources.displayMetrics.widthPixels,
            durationMs = state.duration,
            sensitivity = playerPrefs.gestureSensitivity
        )
        pendingSeekTarget = preview.targetMs.takeIf { preview.seekable }
        showGestureHud(PlayerGestureHudPolicy.seek(preview.targetMs, state.duration, preview.deltaMs), autoHide = false)
    }

    private fun handleVerticalSeekGesture(dy: Float) {
        val state = viewModel.uiState.value
        val preview = PlayerSeekGesturePolicy.verticalPreview(
            anchorPositionMs = seekGestureAnchorPositionMs ?: state.currentPosition,
            dy = dy,
            screenHeightPx = activity.resources.displayMetrics.heightPixels,
            durationMs = state.duration,
            sensitivity = playerPrefs.gestureSensitivity
        )
        pendingSeekTarget = preview.targetMs.takeIf { preview.seekable }
        showGestureHud(PlayerGestureHudPolicy.seek(preview.targetMs, state.duration, preview.deltaMs), autoHide = false)
    }

    private fun applySeekGesture() {
        val state = viewModel.uiState.value
        pendingSeekTarget?.let { target ->
            viewModel.seekTo(PlayerTimeline.safeSeekTarget(0, target, state.duration))
            pendingSeekTarget = null
            seekGestureAnchorPositionMs = null
        }
    }

    private fun showGestureHud(hud: PlayerGestureHud, autoHide: Boolean = true) {
        handler.removeCallbacks(hideGestureHudRunnable)
        val seekIndicator = seekIndicatorProvider()
        seekIndicator.text = PlayerGestureHudDisplayPolicy.indicatorText(hud)
        seekIndicator.visibility = View.VISIBLE
        if (autoHide) {
            handler.postDelayed(hideGestureHudRunnable, PlayerGestureHudDisplayPolicy.AUTO_HIDE_DELAY_MS)
        }
    }

    private fun handleBrightnessGesture(dy: Float) {
        val adjustment = PlayerLevelAdjustmentPolicy.verticalBrightness(
            anchor = brightnessGestureAnchor,
            dy = dy,
            screenHeightPx = activity.resources.displayMetrics.heightPixels
        )
        onCurrentBrightnessChanged(adjustment.level)
        onSetWindowBrightness(adjustment.level)
        brightnessProgressProvider().progress = adjustment.progressPercent
        brightnessIndicatorProvider().visibility = View.VISIBLE
        showGestureHud(PlayerGestureHudPolicy.level(PlayerGestureHudKind.BRIGHTNESS, adjustment.level), autoHide = false)
    }

    private fun handleVolumeGesture(dy: Float) {
        val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val adjustment = PlayerLevelAdjustmentPolicy.verticalVolume(
            anchor = volumeGestureAnchor,
            dy = dy,
            screenHeightPx = activity.resources.displayMetrics.heightPixels,
            maxVolume = maxVolume
        )
        onCurrentVolumeChanged(adjustment.level)
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, adjustment.streamVolume ?: 0, 0)
        volumeProgressProvider().progress = adjustment.progressPercent
        volumeIndicatorProvider().visibility = View.VISIBLE
        showGestureHud(PlayerGestureHudPolicy.level(PlayerGestureHudKind.VOLUME, adjustment.level), autoHide = false)
    }
}
