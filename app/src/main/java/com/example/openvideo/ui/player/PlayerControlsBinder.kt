package com.example.openvideo.ui.player

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.openvideo.R
import com.example.openvideo.core.player.PlayerManager

class PlayerControlsBinder(
    private val activity: AppCompatActivity,
    private val viewModel: PlayerViewModel,
    private val playerManager: PlayerManager,
    private val playButtonProvider: () -> ImageButton,
    private val backButtonProvider: () -> ImageButton,
    private val prevButtonProvider: () -> ImageButton,
    private val nextButtonProvider: () -> ImageButton,
    private val videoListButtonProvider: () -> View?,
    private val screenshotButtonProvider: () -> ImageButton?,
    private val abLoopButtonProvider: () -> ImageButton?,
    private val fullscreenButtonProvider: () -> ImageButton,
    private val pipButtonProvider: () -> ImageButton?,
    private val lockButtonProvider: () -> ImageButton,
    private val controlsContainerProvider: () -> View,
    private val isScreenLockedProvider: () -> Boolean,
    private val controlsVisibleProvider: () -> Boolean,
    private val onTogglePlayPause: () -> Unit,
    private val onFinishPlayer: () -> Unit,
    private val onShowSessionVideoListPanel: () -> Unit,
    private val onOpenPlayerSettingsDialog: () -> Unit,
    private val onShowSpeedPickerDialog: () -> Unit,
    private val onHandleSmartCropQuickToggle: () -> Unit,
    private val onShowAspectRatioQuickDialog: () -> Unit,
    private val onShowSubtitleQuickDialog: () -> Unit,
    private val onScheduleHideControls: () -> Unit,
    private val onEnterPipModeIfSupported: () -> Unit,
    private val onShowAudioTrackQuickDialog: () -> Unit,
    private val onToggleAbLoop: (Long) -> Unit,
    private val onUserOverrodeOrientationChanged: (Boolean) -> Unit,
    private val onRequestedOrientationChanged: (Int) -> Unit,
    private val currentOrientationProvider: () -> Int,
    private val videoRenderViewProvider: () -> View?,
    private val onToggleScreenLock: () -> Unit,
    private val onHideControls: () -> Unit,
    private val onShowControls: () -> Unit
) {
    fun bind() {
        playButtonProvider().setGuardedClick(PlayerLockedInteraction.TRANSPORT) { onTogglePlayPause() }
        backButtonProvider().setGuardedClick(PlayerLockedInteraction.BACK) { onFinishPlayer() }

        prevButtonProvider().setGuardedClick(PlayerLockedInteraction.TRANSPORT) { viewModel.seekBackward() }
        nextButtonProvider().setGuardedClick(PlayerLockedInteraction.TRANSPORT) { viewModel.seekForward() }

        videoListButtonProvider()?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onShowSessionVideoListPanel()
        }

        activity.findViewById<View>(R.id.btn_settings)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onOpenPlayerSettingsDialog()
        }

        activity.findViewById<View>(R.id.portrait_btn_more)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onOpenPlayerSettingsDialog()
        }

        activity.findViewById<View>(R.id.portrait_btn_speed)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onShowSpeedPickerDialog()
        }

        activity.findViewById<View>(R.id.btn_land_seek_back)?.setGuardedClick(PlayerLockedInteraction.TRANSPORT) {
            viewModel.seekBackward()
        }

        activity.findViewById<View>(R.id.btn_land_seek_forward)?.setGuardedClick(PlayerLockedInteraction.TRANSPORT) {
            viewModel.seekForward()
        }

        activity.findViewById<TextView>(R.id.tv_land_speed)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onShowSpeedPickerDialog()
        }

        activity.findViewById<View>(R.id.btn_land_smart_crop)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onHandleSmartCropQuickToggle()
        }

        activity.findViewById<View>(R.id.btn_land_aspect)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onShowAspectRatioQuickDialog()
        }

        activity.findViewById<View>(R.id.btn_land_subtitles)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onShowSubtitleQuickDialog()
            onScheduleHideControls()
        }

        activity.findViewById<View>(R.id.btn_land_pip_float)?.setGuardedClick(PlayerLockedInteraction.TRANSPORT) {
            onEnterPipModeIfSupported()
        }

        activity.findViewById<View>(R.id.btn_land_cast)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            Toast.makeText(activity, R.string.player_land_cast, Toast.LENGTH_SHORT).show()
        }

        activity.findViewById<View>(R.id.portrait_btn_quality)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onShowAudioTrackQuickDialog()
            onScheduleHideControls()
        }

        activity.findViewById<View>(R.id.portrait_btn_episodes)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onShowSessionVideoListPanel()
        }

        activity.findViewById<View>(R.id.portrait_btn_subtitles)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onShowSubtitleQuickDialog()
            onScheduleHideControls()
        }

        screenshotButtonProvider()?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            takeScreenshot()
        }

        abLoopButtonProvider()?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {
            onToggleAbLoop(playerManager.currentPosition)
        }

        fullscreenButtonProvider().setGuardedClick(PlayerLockedInteraction.TRANSPORT) {
            onUserOverrodeOrientationChanged(true)
            onRequestedOrientationChanged(
                PlayerOrientationTogglePolicy.nextRequestedOrientation(currentOrientationProvider())
            )
        }

        pipButtonProvider()?.setGuardedClick(PlayerLockedInteraction.TRANSPORT) {
            onEnterPipModeIfSupported()
        }

        lockButtonProvider().setGuardedClick(PlayerLockedInteraction.LOCK_TOGGLE) {
            onToggleScreenLock()
        }

        controlsContainerProvider().setOnClickListener {
            if (!PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.CHROME_TOGGLE, isScreenLockedProvider())) {
                return@setOnClickListener
            }
            if (controlsVisibleProvider()) onHideControls() else onShowControls()
        }
    }

    private fun View.setGuardedClick(interaction: PlayerLockedInteraction, onClick: () -> Unit) {
        setOnClickListener {
            if (!PlayerLockedControlsPolicy.allows(interaction, isScreenLockedProvider())) return@setOnClickListener
            onClick()
        }
    }

    private fun takeScreenshot() {
        val videoView = videoRenderViewProvider() ?: return
        playerManager.takeScreenshot(videoView) { success, path ->
            activity.runOnUiThread {
                if (success) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.player_screenshot_saved, path),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.player_screenshot_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
