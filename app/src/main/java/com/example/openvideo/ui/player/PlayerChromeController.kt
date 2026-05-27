package com.example.openvideo.ui.player

import android.os.Handler
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.openvideo.R

class PlayerChromeController(
    private val activity: AppCompatActivity,
    private val handler: Handler,
    private val hideControlsRunnable: Runnable,
    private val controlsContainerProvider: () -> View,
    private val topScrimProvider: () -> View,
    private val bottomScrimProvider: () -> View,
    private val topBarProvider: () -> View,
    private val bottomPanelProvider: () -> View,
    private val toolRowProvider: () -> View,
    private val landRightFloatColumnProvider: () -> View?,
    private val lockButtonProvider: () -> ImageButton,
    private val fullscreenButtonProvider: () -> ImageButton,
    private val isScreenLockedProvider: () -> Boolean,
    private val onScreenLockedChanged: (Boolean) -> Unit,
    private val controlsVisibleProvider: () -> Boolean,
    private val onControlsVisibleChanged: (Boolean) -> Unit,
    private val isSettingsOverlayVisibleProvider: () -> Boolean,
    private val controlsVisibleBeforeSettingsOverlayProvider: () -> Boolean,
    private val onSetLockedGestureOverlay: () -> Unit,
    private val onInitGestures: () -> Unit,
    private val controlsOpacityPercentProvider: () -> Int,
    private val controlsAutoHideSecondsProvider: () -> Int,
    private val controlsChromeMaxAlpha: () -> Float
) {
    fun showControls() {
        applyChromePresentation(
            PlayerChromeVisibilityPolicy.show(
                controlsOpacityPercent = controlsOpacityPercentProvider(),
                autoHideSeconds = controlsAutoHideSecondsProvider()
            )
        )
        applyControlVisibility()
    }

    fun hideControls() {
        val presentation = PlayerChromeVisibilityPolicy.hide()
        onControlsVisibleChanged(presentation.controlsVisible)
        val controlsContainer = controlsContainerProvider()
        controlsContainer.animate().cancel()
        controlsContainer.animate().alpha(presentation.alpha)
            .setDuration(PlayerChromePolicy.CHROME_FADE_DURATION_MS).withEndAction {
                if (!controlsVisibleProvider()) {
                    controlsContainer.visibility = if (presentation.containerVisible) View.VISIBLE else View.GONE
                }
            }.start()
    }

    fun showLockedControls() {
        if (!isScreenLockedProvider()) {
            showControls()
            return
        }
        applyChromePresentation(
            PlayerChromeVisibilityPolicy.showLocked(
                controlsOpacityPercent = controlsOpacityPercentProvider()
            )
        )
        applyControlVisibility()
    }

    fun applyControlVisibility() {
        if (PlayerChromeSettingsOverlayPolicy.hidesAllChromeRegions(isSettingsOverlayVisibleProvider())) {
            topScrimProvider().visibility = View.GONE
            bottomScrimProvider().visibility = View.GONE
            topBarProvider().visibility = View.GONE
            bottomPanelProvider().visibility = View.GONE
            toolRowProvider().visibility = View.GONE
            landRightFloatColumnProvider()?.visibility = View.GONE
            lockButtonProvider().visibility = View.GONE
            fullscreenButtonProvider().visibility = View.GONE
            return
        }
        val isScreenLocked = isScreenLockedProvider()
        val controlsVisible = controlsVisibleProvider()
        val visibility = PlayerLockedControlsPolicy.visibility(isScreenLocked, controlsVisible)
        fun regionVisibility(region: PlayerChromeRegion): Int =
            if (PlayerLockedControlsPolicy.isChromeRegionVisible(region, isScreenLocked, controlsVisible)) {
                View.VISIBLE
            } else {
                View.GONE
            }

        topScrimProvider().visibility = regionVisibility(PlayerChromeRegion.TOP_SCRIM)
        bottomScrimProvider().visibility = regionVisibility(PlayerChromeRegion.BOTTOM_SCRIM)
        topBarProvider().visibility = regionVisibility(PlayerChromeRegion.TOP_BAR)
        bottomPanelProvider().visibility = regionVisibility(PlayerChromeRegion.BOTTOM_PANEL)
        toolRowProvider().visibility = regionVisibility(PlayerChromeRegion.TOOL_ROW)
        landRightFloatColumnProvider()?.visibility = regionVisibility(PlayerChromeRegion.LAND_RIGHT_FLOAT_COLUMN)
        val lockButton = lockButtonProvider()
        lockButton.visibility = if (visibility.lockButtonVisible) View.VISIBLE else View.GONE
        lockButton.isSelected = visibility.lockButtonSelected
        if (PlayerLockButtonStylePolicy.shouldUseAccentTint(visibility.lockButtonSelected)) {
            lockButton.setColorFilter(ContextCompat.getColor(activity, R.color.player_accent))
        } else {
            lockButton.clearColorFilter()
        }
        fullscreenButtonProvider().visibility =
            if (visibility.fullscreenButtonVisible) View.VISIBLE else View.GONE
    }

    fun hideChromeForSettingsOverlay() {
        val controlsContainer = controlsContainerProvider()
        controlsContainer.animate().cancel()
        controlsContainer.alpha = 0f
        controlsContainer.visibility = View.GONE
        applyControlVisibility()
    }

    fun restoreChromeAfterSettingsOverlay() {
        onControlsVisibleChanged(controlsVisibleBeforeSettingsOverlayProvider())
        val controlsContainer = controlsContainerProvider()
        controlsContainer.animate().cancel()
        controlsContainer.alpha = PlayerChromeSettingsOverlayPolicy.restoreContainerAlpha(
            controlsWereVisible = controlsVisibleBeforeSettingsOverlayProvider(),
            maxAlpha = controlsChromeMaxAlpha()
        )
        if (PlayerChromeSettingsOverlayPolicy.restoreContainerVisible(controlsVisibleBeforeSettingsOverlayProvider())) {
            controlsContainer.visibility = View.VISIBLE
        } else {
            controlsContainer.visibility = View.GONE
        }
        applyControlVisibility()
    }

    fun scheduleHideControls() {
        if (PlayerChromeSettingsOverlayPolicy.suppressesControlAutoHide(isSettingsOverlayVisibleProvider())) return
        handler.removeCallbacks(hideControlsRunnable)
        val presentation = PlayerChromeVisibilityPolicy.show(
            controlsOpacityPercent = controlsOpacityPercentProvider(),
            autoHideSeconds = controlsAutoHideSecondsProvider()
        )
        presentation.hideDelayMs?.let { delay ->
            handler.postDelayed(hideControlsRunnable, delay)
        }
    }

    fun applyChromePresentation(presentation: PlayerChromePresentation) {
        onControlsVisibleChanged(presentation.controlsVisible)
        val controlsContainer = controlsContainerProvider()
        controlsContainer.animate().cancel()
        controlsContainer.alpha = presentation.alpha
        controlsContainer.visibility = if (presentation.containerVisible) View.VISIBLE else View.GONE
        handler.removeCallbacks(hideControlsRunnable)
        presentation.hideDelayMs?.let { delay ->
            handler.postDelayed(hideControlsRunnable, delay)
        }
    }

    fun toggleScreenLock() {
        val nextLocked = !isScreenLockedProvider()
        onScreenLockedChanged(nextLocked)
        if (nextLocked) {
            onSetLockedGestureOverlay()
            applyScreenLockChromeReveal()
            Toast.makeText(activity, activity.getString(R.string.player_locked), Toast.LENGTH_SHORT).show()
        } else {
            onInitGestures()
            applyScreenLockChromeReveal()
            scheduleHideControls()
            Toast.makeText(activity, activity.getString(R.string.player_unlocked), Toast.LENGTH_SHORT).show()
        }
    }

    fun applyScreenLockChromeReveal() {
        val reveal = PlayerScreenLockChromePolicy.revealChrome(controlsChromeMaxAlpha())
        onControlsVisibleChanged(reveal.controlsVisible)
        val controlsContainer = controlsContainerProvider()
        controlsContainer.visibility = if (reveal.containerVisible) View.VISIBLE else View.GONE
        controlsContainer.alpha = reveal.alpha
        applyControlVisibility()
    }

    fun unlockPlayerForPause() {
        if (!isScreenLockedProvider()) return
        onScreenLockedChanged(false)
        onInitGestures()
        controlsContainerProvider().animate().cancel()
        applyScreenLockChromeReveal()
        scheduleHideControls()
    }
}
