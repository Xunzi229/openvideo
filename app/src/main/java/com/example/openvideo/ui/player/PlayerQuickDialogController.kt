package com.example.openvideo.ui.player

import android.app.Dialog
import android.os.Handler
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.openvideo.R
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.settings.DefaultPlayerSettings
import kotlin.math.abs

class PlayerQuickDialogController(
    private val activity: AppCompatActivity,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val pickSubtitleLauncher: ActivityResultLauncher<Array<String>>,
    private val handler: Handler,
    private val hideControlsRunnable: Runnable,
    private val controlsVisibleProvider: () -> Boolean,
    private val onControlsVisibleBeforeSettingsOverlayChanged: (Boolean) -> Unit,
    private val isSettingsOverlayVisibleProvider: () -> Boolean,
    private val onSettingsOverlayVisibleChanged: (Boolean) -> Unit,
    private val onHideControls: () -> Unit,
    private val onHideChromeForSettingsOverlay: () -> Unit,
    private val onRestoreChromeAfterSettingsOverlay: () -> Unit,
    private val onScheduleHideControls: () -> Unit,
    private val onApplyScreenBrightness: (Int) -> Unit,
    private val onAspectRatioChanged: () -> Unit,
    private val onApplyPlayerSettings: () -> Unit,
    private val onApplySubtitlePresentation: () -> Unit,
    private val onSessionVideoPicked: (VideoItem) -> Unit
) {
    private var activePlayerDialog: Dialog? = null

    fun showSessionVideoListPanel() {
        val queue = viewModel.sessionQueue.value
        if (!PlayerSessionQueueChromePolicy.canOpenSessionListPanel(queue.size)) return
        showExclusivePlayerDialog { onDismiss ->
            handler.removeCallbacks(hideControlsRunnable)
            PlayerVideoListDialog(
                context = activity,
                videos = queue,
                playingVideoId = viewModel.playingVideoId,
                onPick = onSessionVideoPicked
            ).apply {
                setOnDismissListener {
                    onDismiss()
                    onScheduleHideControls()
                }
            }.also { dialog ->
                dialog.show()
            }
        }
    }

    fun openPlayerSettingsDialog() {
        showExclusivePlayerDialog { onDismiss ->
            handler.removeCallbacks(hideControlsRunnable)
            onControlsVisibleBeforeSettingsOverlayChanged(controlsVisibleProvider())
            onSettingsOverlayVisibleChanged(true)
            onHideChromeForSettingsOverlay()
            val dialog = PlayerSettingsDialog(
                context = activity,
                playerManager = playerManager,
                viewModel = viewModel,
                playerPrefs = playerPrefs,
                onScreenBrightnessChanged = onApplyScreenBrightness,
                onRequestPickSubtitle = {
                    pickSubtitleLauncher.launch(arrayOf("*/*"))
                },
                onAspectRatioChanged = onAspectRatioChanged,
                onPlayerPrefsReset = onApplyPlayerSettings
            )
            dialog.setOnDismissListener {
                onDismiss()
                onSettingsOverlayVisibleChanged(false)
                onRestoreChromeAfterSettingsOverlay()
                onApplyPlayerSettings()
                onScheduleHideControls()
            }
            dialog.show()
            dialog
        }
    }

    fun showAspectRatioQuickDialog() {
        showExclusivePlayerDialog { onDismiss ->
            PlayerGlassSheetDialog.showSingleChoice(
                context = activity,
                layoutInflater = activity.layoutInflater,
                titleRes = R.string.player_sheet_aspect_ratio,
                choices = PlayerAspectRatioOptions.entries.map { option ->
                    PlayerGlassSheetChoice(
                        value = option.ratio,
                        label = activity.getString(option.labelRes),
                        selected = option.ratio == playerPrefs.aspectRatio
                    )
                },
                chrome = quickChoiceChrome(),
                playerPrefs = playerPrefs,
                onDismiss = {
                    onDismiss()
                    onScheduleHideControls()
                }
            ) { ratio ->
                val selection = PlayerContentFrameSettingsPolicy.onAspectRatioSelected(
                    aspectRatio = ratio,
                    currentContentFrameMode = playerPrefs.contentFrameMode
                )
                playerPrefs.aspectRatio = selection.aspectRatio
                selection.contentFrameOverride?.let { playerPrefs.contentFrameMode = it }
                viewModel.setAspectRatio(selection.aspectRatio)
                onAspectRatioChanged()
            }
        }
    }

    fun showSpeedPickerDialog() {
        val speeds = DefaultPlayerSettings.supportedSpeeds
        showExclusivePlayerDialog { onDismiss ->
            handler.removeCallbacks(hideControlsRunnable)
            onHideControls()
            PlayerGlassSheetDialog.showSingleChoice(
                context = activity,
                layoutInflater = activity.layoutInflater,
                titleRes = R.string.player_pick_speed,
                choices = speeds.map { speed ->
                    PlayerGlassSheetChoice(
                        value = speed,
                        label = "${speed}x",
                        selected = speed == playerPrefs.speed
                    )
                },
                chrome = quickChoiceChrome(),
                playerPrefs = playerPrefs,
                onDismiss = {
                    onDismiss()
                    onScheduleHideControls()
                }
            ) { speed ->
                playerPrefs.speed = speed
                viewModel.setSpeed(
                    speed,
                    PlayerPlaybackSettings.pitchFor(speed, playerPrefs.speedPreservePitch)
                )
                activity.findViewById<TextView>(R.id.tv_land_speed)?.text = PlayerSpeedLabel.format(speed)
            }
        }
    }

    fun showAudioTrackQuickDialog() {
        val state = PlayerQuickEntryPolicy.audioEntry(
            tracks = viewModel.audioTracks(),
            audioMuted = playerPrefs.audioMuted,
            trackLabel = { track ->
                PlayerAudioDiagnosticsPolicy.quickTrackSummary(
                    track = track,
                    streamLabel = activity.getString(R.string.player_settings_info_stream, track.groupIndex + 1)
                )
            }
        )
        val items = state.items.map { item ->
            when (item.action) {
                PlayerQuickEntryAction.DisableAudio ->
                    item.copy(label = activity.getString(R.string.player_sheet_disable))
                PlayerQuickEntryAction.None ->
                    item.copy(label = activity.getString(R.string.player_settings_audio_track_none))
                else -> item
            }
        }
        showQuickEntryDialog(R.string.player_sheet_audio_track, items) { action ->
            when (action) {
                is PlayerQuickEntryAction.SelectAudioTrack -> {
                    val track = viewModel.audioTracks().firstOrNull {
                        it.groupIndex == action.groupIndex && it.trackIndex == action.trackIndex
                    } ?: return@showQuickEntryDialog
                    viewModel.selectAudioTrack(track)
                }
                PlayerQuickEntryAction.DisableAudio -> viewModel.disableAudioTrack()
                else -> Unit
            }
        }
    }

    fun showSubtitleQuickDialog() {
        val state = PlayerQuickEntryPolicy.subtitleEntry(
            hasLoadedSubtitles = viewModel.uiState.value.subtitles.isNotEmpty(),
            subtitlesEnabled = playerPrefs.subtitlesEnabled,
            subtitleDelayMs = playerPrefs.subtitleDelayMs
        )
        val items = state.items.map { item ->
            when (val action = item.action) {
                is PlayerQuickEntryAction.SetSubtitlesEnabled ->
                    item.copy(label = activity.getString(if (action.enabled) R.string.player_sheet_enable else R.string.settings_subtitle_track_off))
                is PlayerQuickEntryAction.SubtitleDelayStatus ->
                    item.copy(label = activity.getString(R.string.player_quick_subtitle_delay_current, action.delayMs))
                is PlayerQuickEntryAction.AdjustSubtitleDelay ->
                    item.copy(
                        label = activity.getString(
                            if (PlayerQuickEntryPolicy.subtitleDelayAdjustIsDecrease(action.deltaMs)) {
                                R.string.player_quick_subtitle_delay_minus_in_dialog
                            } else {
                                R.string.player_quick_subtitle_delay_plus_in_dialog
                            },
                            abs(action.deltaMs),
                            playerPrefs.subtitleDelayMs
                        )
                    )
                PlayerQuickEntryAction.ResetSubtitleDelay ->
                    item.copy(label = activity.getString(R.string.player_quick_subtitle_delay_reset))
                PlayerQuickEntryAction.PickSubtitleFile ->
                    item.copy(label = activity.getString(R.string.player_sheet_select_subtitle_file))
                PlayerQuickEntryAction.OpenSubtitleSettings ->
                    item.copy(label = activity.getString(R.string.player_quick_subtitle_more_settings))
                PlayerQuickEntryAction.None ->
                    item.copy(label = activity.getString(R.string.player_quick_subtitle_none))
                else -> item
            }
        }
        showQuickEntryDialog(R.string.player_sheet_subtitles, items) { action ->
            when (action) {
                is PlayerQuickEntryAction.SetSubtitlesEnabled -> {
                    playerPrefs.subtitlesEnabled = action.enabled
                    onApplySubtitlePresentation()
                }
                is PlayerQuickEntryAction.AdjustSubtitleDelay -> {
                    playerPrefs.subtitleDelayMs += action.deltaMs
                    onApplySubtitlePresentation()
                }
                PlayerQuickEntryAction.ResetSubtitleDelay -> {
                    playerPrefs.subtitleDelayMs = 0
                    onApplySubtitlePresentation()
                }
                PlayerQuickEntryAction.PickSubtitleFile ->
                    pickSubtitleLauncher.launch(arrayOf("*/*"))
                PlayerQuickEntryAction.OpenSubtitleSettings ->
                    openSubtitleSettingsSheet()
                else -> Unit
            }
        }
    }

    fun openSubtitleSettingsSheet() {
        if (activity.supportFragmentManager.findFragmentByTag(SUBTITLE_SETTINGS_SHEET_TAG) != null) return
        handler.removeCallbacks(hideControlsRunnable)
        onControlsVisibleBeforeSettingsOverlayChanged(controlsVisibleProvider())
        onSettingsOverlayVisibleChanged(true)
        onHideChromeForSettingsOverlay()
        PlayerSubtitleSettingsSheet().apply {
            onDismissListener = ::onSubtitleSettingsSheetDismissed
        }.show(activity.supportFragmentManager, SUBTITLE_SETTINGS_SHEET_TAG)
    }

    fun onSubtitleSettingsSheetDismissed() {
        if (!isSettingsOverlayVisibleProvider()) return
        onSettingsOverlayVisibleChanged(false)
        onRestoreChromeAfterSettingsOverlay()
        onApplyPlayerSettings()
        onScheduleHideControls()
    }

    fun dismissSubtitleSettingsSheet() {
        (activity.supportFragmentManager.findFragmentByTag(SUBTITLE_SETTINGS_SHEET_TAG) as? PlayerSubtitleSettingsSheet)
            ?.dismissAllowingStateLoss()
    }

    private fun showQuickEntryDialog(
        titleRes: Int,
        items: List<PlayerQuickEntryItem>,
        onSelected: (PlayerQuickEntryAction) -> Unit
    ) {
        showExclusivePlayerDialog { onDismiss ->
            handler.removeCallbacks(hideControlsRunnable)
            onHideControls()
            PlayerGlassSheetDialog.showSingleChoice(
                context = activity,
                layoutInflater = activity.layoutInflater,
                titleRes = titleRes,
                choices = items.map { item ->
                    PlayerGlassSheetChoice(
                        value = item.action,
                        label = item.label,
                        selected = item.selected,
                        enabled = item.enabled
                    )
                },
                chrome = quickChoiceChrome(),
                playerPrefs = playerPrefs,
                onDismiss = {
                    onDismiss()
                    onScheduleHideControls()
                },
                onSelected = onSelected
            )
        }
    }

    private fun showExclusivePlayerDialog(
        showDialog: (onDismiss: () -> Unit) -> Dialog
    ) {
        val current = activePlayerDialog
        if (current?.isShowing == true) return
        var dialog: Dialog? = null
        val clearActiveDialog = {
            if (activePlayerDialog === dialog) {
                activePlayerDialog = null
            }
        }
        dialog = showDialog(clearActiveDialog)
        activePlayerDialog = dialog
    }

    private fun quickChoiceChrome(): PlayerGlassSheetChrome =
        if (PlayerConfigurationOrientationPolicy.isLandscape(activity.resources.configuration.orientation)) {
            PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL
        } else {
            PlayerGlassSheetChrome.PLAYER_BOTTOM
        }

    private companion object {
        const val SUBTITLE_SETTINGS_SHEET_TAG = "player_subtitle_settings_sheet"
    }
}
