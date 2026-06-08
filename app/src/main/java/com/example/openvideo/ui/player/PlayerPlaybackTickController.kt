package com.example.openvideo.ui.player

import android.os.Handler
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs

class PlayerPlaybackTickController(
    private val activity: AppCompatActivity,
    private val handler: Handler,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val seekBarProvider: () -> SeekBar,
    private val currentTimeProvider: () -> TextView,
    private val totalTimeProvider: () -> TextView,
    private val subtitleProvider: () -> TextView,
    private val secondarySubtitleProvider: () -> TextView,
    private val isSeekingProvider: () -> Boolean,
    private val abLoopStateProvider: () -> PlayerAbLoopState,
    private val abLoopPointAProvider: () -> Long,
    private val abLoopPointBProvider: () -> Long,
    private val formatTime: (Long) -> String,
    private val landSpeedLabel: (Float) -> String
) {
    private var hasSkippedIntro = false
    private var hasSkippedOutro = false
    private var lastHistorySavedPositionMs = 0L

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isSeekingProvider()) {
                viewModel.updatePosition()
                val state = viewModel.uiState.value
                val seekBarState = PlayerTimeline.seekBarState(state.currentPosition, state.duration)
                val seekBar = seekBarProvider()
                seekBar.isEnabled = seekBarState.enabled
                seekBar.max = seekBarState.max
                seekBar.progress = seekBarState.progress
                currentTimeProvider().text = formatTime(state.currentPosition)
                totalTimeProvider().text = PlayerTimeline.durationText(state.duration, formatTime)
                activity.findViewById<TextView>(R.id.tv_land_speed)?.text =
                    landSpeedLabel(playerPrefs.speed)
                saveProgressPeriodically(state.currentPosition)

                applyPlaybackTickSeek(state.currentPosition, state.duration)
                applySubtitlePresentation()
            }
            handler.postDelayed(this, PlayerPlaybackTickPolicy.UI_TICK_INTERVAL_MS)
        }
    }

    fun observe() {
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    fun stopObserving() {
        handler.removeCallbacks(updateRunnable)
    }

    fun resetForNewVideo(reset: PlayerVideoSwitchReset) {
        hasSkippedIntro = reset.hasSkippedIntro
        hasSkippedOutro = reset.hasSkippedOutro
        lastHistorySavedPositionMs = PlaybackProgressPolicy.onNewMedia()
    }

    private fun saveProgressPeriodically(positionMs: Long) {
        val decision = PlaybackProgressPolicy.onPositionTick(
            positionMs = positionMs,
            lastSavedPositionMs = lastHistorySavedPositionMs
        )
        lastHistorySavedPositionMs = decision.nextLastSavedPositionMs
        if (decision.shouldSaveHistory) viewModel.saveHistory()
    }

    fun applySubtitlePresentation() {
        val dualSubtitleText = viewModel.getCurrentDualSubtitle()
        val subtitleText = PlayerSubtitlePresentationPolicy.resolveSubtitleText(
            subtitlesEnabled = playerPrefs.subtitlesEnabled,
            currentSubtitle = dualSubtitleText?.primary.orEmpty()
        )
        val presentation = PlayerSubtitlePresentationPolicy.present(
            subtitlesEnabled = playerPrefs.subtitlesEnabled,
            subtitleText = subtitleText
        )
        val subtitle = subtitleProvider()
        subtitle.text = presentation.text
        subtitle.visibility = if (presentation.visible) View.VISIBLE else View.GONE
        PlayerSubtitleCueStylePolicy.apply(
            textView = subtitle,
            style = dualSubtitleText?.primaryStyle,
            playerPrefs = playerPrefs,
            defaultTextSizeSp = playerPrefs.subtitleSize,
            defaultTextColor = playerPrefs.subtitleColor
        )

        val secondaryPresentation = PlayerSubtitlePresentationPolicy.present(
            subtitlesEnabled = playerPrefs.subtitlesEnabled,
            subtitleText = dualSubtitleText?.secondary.orEmpty()
        )
        val secondarySubtitle = secondarySubtitleProvider()
        secondarySubtitle.text = secondaryPresentation.text
        secondarySubtitle.visibility = if (secondaryPresentation.visible) View.VISIBLE else View.GONE
        PlayerSubtitleCueStylePolicy.apply(
            textView = secondarySubtitle,
            style = dualSubtitleText?.secondaryStyle,
            playerPrefs = playerPrefs,
            defaultTextSizeSp = playerPrefs.secondarySubtitleSize,
            defaultTextColor = playerPrefs.secondarySubtitleColor
        )
    }

    fun applyPlaybackTickSeek(currentPositionMs: Long, durationMs: Long) {
        val result = PlayerPlaybackTickPolicy.seekTarget(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            abLoopState = abLoopStateProvider(),
            abLoopPointA = abLoopPointAProvider(),
            abLoopPointB = abLoopPointBProvider(),
            skipIntroOutro = playerPrefs.skipIntroOutro,
            introSeconds = playerPrefs.introSeconds,
            outroSeconds = playerPrefs.outroSeconds,
            hasSkippedIntro = hasSkippedIntro,
            hasSkippedOutro = hasSkippedOutro,
            clipLoopPreview = playerPrefs.clipLoopPreview,
            clipStartMs = playerPrefs.clipStartMs,
            clipEndMs = playerPrefs.clipEndMs
        ) ?: return

        hasSkippedIntro = result.hasSkippedIntro
        hasSkippedOutro = result.hasSkippedOutro
        viewModel.seekTo(result.positionMs)
    }
}
