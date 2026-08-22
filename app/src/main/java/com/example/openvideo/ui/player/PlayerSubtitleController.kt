package com.example.openvideo.ui.player

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.subtitle.SubtitleCandidate
import com.example.openvideo.core.subtitle.SubtitleLoader
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionSheet
import com.example.openvideo.core.ui.AppleHud
import java.io.File

class PlayerSubtitleController(
    private val activity: AppCompatActivity,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val subtitleLoader: SubtitleLoader,
    private val startupTrace: PlayerStartupTrace,
    private val onApplyScreenBrightness: (Int) -> Unit,
    private val onApplyPlayerSettings: () -> Unit,
    private val onScheduleHideControls: () -> Unit
) {
    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener

    var currentVideoUriString: String = ""
        private set
    var currentVideoPath: String = ""
        private set

    fun registerPickSubtitleLauncher(): ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            onSubtitlePicked(uri)
        }

    fun setCurrentVideo(uriString: String, path: String) {
        currentVideoUriString = uriString
        currentVideoPath = path
    }

    fun loadSubtitlesAsync(uriString: String, videoPath: String, showToast: Boolean = false) {
        viewModel.loadSubtitles(
            uriString = uriString,
            videoPath = videoPath,
            showToast = showToast,
            onFinished = { decision ->
                PlayerSubtitleLoadToastPolicy.messageRes(decision.toastKind)?.let { messageRes ->
                    AppleHud.show(activity, messageRes)
                }
                startupTrace.record(PlayerStartupTrace.Events.SUBTITLE_SCAN_FINISHED)
            },
            onCandidateChoiceRequired = { candidates ->
                showSubtitleCandidateChoiceDialog(candidates)
            }
        )
    }

    private fun showSubtitleCandidateChoiceDialog(candidates: List<SubtitleCandidate>) {
        if (candidates.isEmpty()) return
        AppleActionSheet.show(
            context = activity,
            title = activity.getString(R.string.player_subtitle_candidate_choice_title),
            actions = candidates.map { candidate ->
                val label = File(candidate.path).name.ifBlank { candidate.path }
                AppleAction(label) {
                    playerPrefs.externalSubtitleUri = candidate.path
                }
            },
            defaultFocusCancel = false
        )
    }

    fun registerPrefsListener() {
        settingsPrefs = activity.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == PlayerPrefs.KEY_EXTERNAL_SUBTITLE) {
                val uri = prefs.getString(key, "") ?: ""
                if (uri.isNotBlank()) {
                    loadSubtitlesAsync(uri, currentVideoPath, showToast = true)
                }
            }
            if (key == PlayerPrefs.KEY_BRIGHTNESS_ADJUSTMENT) {
                onApplyScreenBrightness(playerPrefs.brightnessAdjustment)
                onScheduleHideControls()
            }
            if (PlayerPrefs.requiresImmediatePlayerApply(key)) {
                onApplyPlayerSettings()
                onScheduleHideControls()
            }
        }
        settingsPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    fun unregisterPrefsListener() {
        if (this::settingsPrefs.isInitialized) {
            try {
                settingsPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            } catch (_: Exception) {
            }
        }
    }

    private fun onSubtitlePicked(uri: Uri?) {
        if (uri == null) return
        val subtitles = subtitleLoader.loadFromUri(uri)
        if (subtitles.isNotEmpty()) {
            viewModel.setSubtitles(subtitles)
            AppleHud.show(activity, R.string.player_subtitle_loaded)
        } else {
            AppleHud.show(activity, R.string.player_subtitle_load_failed)
        }
    }
}
