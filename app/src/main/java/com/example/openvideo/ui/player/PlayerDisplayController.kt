package com.example.openvideo.ui.player

import android.content.Context
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.openvideo.R
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.PlayerPrefs

class PlayerDisplayController(
    private val activity: AppCompatActivity,
    private val window: Window,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val playerViewProvider: () -> PlayerView,
    private val bottomPanelProvider: () -> View,
    private val controlsContainerProvider: () -> View,
    private val subtitleProvider: () -> TextView,
    private val brightnessProgressProvider: () -> ProgressBar,
    private val volumeProgressProvider: () -> ProgressBar,
    private val topBarProvider: () -> View,
    private val lockButtonProvider: () -> ImageButton,
    private val landRightFloatColumnProvider: () -> View?,
    private val controlsVisibleProvider: () -> Boolean,
    private val onCurrentBrightnessChanged: (Float) -> Unit,
    private val onCurrentVolumeChanged: (Float) -> Unit,
    private val onResetManualVideoZoom: () -> Unit,
    private val controlsChromeMaxAlpha: () -> Float,
    private val onApplyContentAspectRatio: () -> Unit,
    private val onApplyContentFrameTransform: () -> Unit,
    private val onSetWindowBrightness: (Float) -> Unit
) {
    fun applyPlayerSettings() {
        viewModel.setAspectRatio(playerPrefs.aspectRatio)
        viewModel.setDecodeMode(PlayerDecodeModePolicy.decodeMode(playerPrefs.softwareAudioDecoder))
        viewModel.setSpeed(
            playerPrefs.speed,
            PlayerPlaybackSettings.pitchFor(playerPrefs.speed, playerPrefs.speedPreservePitch)
        )
        viewModel.setRepeatMode(PlayerPlaybackSettings.repeatModeFor(playerPrefs.loopMode))
        viewModel.setVolumeBoost(playerPrefs.volumeBoost)
        playerManager.setMuted(playerPrefs.audioMuted)
        val colorAdjustments = PlayerVideoColorAdjustmentPolicy.fromPercent(
            contrastPercent = playerPrefs.contrastAdjustment,
            saturationPercent = playerPrefs.saturationAdjustment
        )
        playerManager.applyVideoAdjustments(
            0f,
            colorAdjustments.contrast,
            colorAdjustments.saturation
        )
        applyScreenBrightness(playerPrefs.brightnessAdjustment)
        val playerView = playerViewProvider()
        playerView.alpha = PlayerDisplayVisibilityPolicy.videoLayerAlpha(playerPrefs.videoDisplayEnabled)
        bottomPanelProvider().alpha = 1f
        if (controlsVisibleProvider()) {
            val controlsContainer = controlsContainerProvider()
            controlsContainer.animate().cancel()
            controlsContainer.alpha = controlsChromeMaxAlpha()
        }

        if (PlayerScreenOnPolicy.shouldKeepScreenOn(playerPrefs.keepScreenOn)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        applyDisplaySettings()

        val subtitle = subtitleProvider()
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, playerPrefs.subtitleSize.toFloat())
        subtitle.setTextColor(playerPrefs.subtitleColor)
        subtitle.setBackgroundColor(PlayerSubtitleStylePolicy.backgroundColor(playerPrefs.subtitleBgStyle))
        subtitle.post {
            subtitle.translationY = PlayerDisplayAdjustment.subtitleTranslationY(
                playerViewHeightPx = playerView.height,
                position = playerPrefs.subtitlePosition
            )
        }
    }

    fun applyDisplaySettings() {
        if (!PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio)) {
            onResetManualVideoZoom()
        }
        setPlayerResizeMode()
        onApplyContentAspectRatio()
        onApplyContentFrameTransform()
        val playerView = playerViewProvider()
        playerView.rotation = playerPrefs.rotation.toFloat()
        playerView.scaleX = PlayerDisplayAdjustment.mirrorScaleX(playerPrefs.mirror)
    }

    fun initBrightnessAndVolume() {
        val windowBrightness = window.attributes.screenBrightness
        val systemBrightness = try {
            Settings.System.getInt(
                activity.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            ) / 255f
        } catch (_: Exception) {
            null
        }
        val currentBrightness = PlayerWindowBrightnessPolicy.initialBrightness(
            windowBrightness = windowBrightness,
            systemBrightnessNormalized = systemBrightness
        )
        onCurrentBrightnessChanged(currentBrightness)

        val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val currentVolume = PlayerWindowBrightnessPolicy.initialVolumeLevel(currentVol, maxVolume)
        onCurrentVolumeChanged(currentVolume)

        brightnessProgressProvider().progress = PlayerWindowBrightnessPolicy.levelToProgressPercent(currentBrightness)
        volumeProgressProvider().progress = PlayerWindowBrightnessPolicy.levelToProgressPercent(currentVolume)
    }

    fun applyScreenBrightness(adjustmentPercent: Int) {
        val brightness = PlayerDisplayAdjustment.screenBrightnessFor(adjustmentPercent)
        onSetWindowBrightness(brightness)
        if (brightness >= 0f) {
            onCurrentBrightnessChanged(brightness)
            brightnessProgressProvider().progress = PlayerWindowBrightnessPolicy.levelToProgressPercent(brightness)
        }
    }

    fun applyLandscapePlayerGeometry() {
        if (!PlayerConfigurationOrientationPolicy.isLandscape(activity.resources.configuration.orientation)) return
        val controlsContainer = controlsContainerProvider()
        val geometry = PlayerLandscapeGeometryPolicy.compute(
            widthPx = controlsContainer.width,
            heightPx = controlsContainer.height,
            density = activity.resources.displayMetrics.density
        ) ?: return

        topBarProvider().updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = geometry.containerHorizontalMarginPx
            marginEnd = geometry.containerHorizontalMarginPx
            topMargin = geometry.topBarTopMarginPx
        }
        bottomPanelProvider().updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = geometry.containerHorizontalMarginPx
            marginEnd = geometry.containerHorizontalMarginPx
            bottomMargin = geometry.bottomPanelBottomMarginPx
        }
        lockButtonProvider().updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = geometry.lockMarginStartPx
            topMargin = 0
            bottomMargin = 0
            verticalBias = 0.5f
        }
        landRightFloatColumnProvider()?.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginEnd = geometry.containerHorizontalMarginPx
        }

        listOf(
            R.id.btn_land_seek_back,
            R.id.btn_prev,
            R.id.btn_next,
            R.id.btn_land_seek_forward
        ).forEach { id ->
            activity.findViewById<View>(id)?.updateLayoutParams<LinearLayout.LayoutParams> {
                width = geometry.iconSidePx
                height = geometry.iconSidePx
            }
        }
        activity.findViewById<View>(R.id.btn_fullscreen)?.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = geometry.iconSidePx
            height = geometry.iconSidePx
        }
        activity.findViewById<View>(R.id.btn_play)?.updateLayoutParams<LinearLayout.LayoutParams> {
            width = geometry.playSidePx
            height = geometry.playSidePx
            marginStart = geometry.transportGapPx
            marginEnd = geometry.transportGapPx
        }
        activity.findViewById<View>(R.id.btn_prev)?.updateLayoutParams<LinearLayout.LayoutParams> {
            marginStart = geometry.innerGapPx
        }
        activity.findViewById<View>(R.id.btn_next)?.updateLayoutParams<LinearLayout.LayoutParams> {
            marginEnd = geometry.innerGapPx
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun setPlayerResizeMode() {
        playerViewProvider().resizeMode = PlayerViewSettings.resizeModeFor(playerPrefs.aspectRatio)
    }
}
