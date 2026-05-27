package com.example.openvideo.ui.player

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.openvideo.core.prefs.PlayerPrefs

class PlayerSeekBarController(
    private val activity: AppCompatActivity,
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val seekBarProvider: () -> SeekBar,
    private val currentTimeProvider: () -> TextView,
    private val thumbnailContainerProvider: () -> FrameLayout,
    private val thumbnailImageProvider: () -> ImageView,
    private val thumbnailTimeProvider: () -> TextView,
    private val bottomPanelProvider: () -> View,
    private val progressRowProvider: () -> View,
    private val controlsContainerProvider: () -> View,
    private val isScreenLockedProvider: () -> Boolean,
    private val onSeekingChanged: (Boolean) -> Unit,
    private val onRemoveHideControlsCallbacks: () -> Unit,
    private val onScheduleHideControls: () -> Unit,
    private val formatTime: (Long) -> String
) {
    private var seekThumbnailLoader: PlayerSeekThumbnailLoader? = null

    fun attach() {
        seekBarProvider().setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                currentTimeProvider().text = formatTime(progress.toLong())
                updateSeekThumbnail(sb, progress)
            }

            override fun onStartTrackingTouch(sb: SeekBar) {
                if (!PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SEEK_BAR, isScreenLockedProvider())) {
                    return
                }
                onSeekingChanged(true)
                onRemoveHideControlsCallbacks()
                maybeStartSeekThumbnail()
            }

            override fun onStopTrackingTouch(sb: SeekBar) {
                if (!PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SEEK_BAR, isScreenLockedProvider())) {
                    return
                }
                viewModel.seekTo(
                    PlayerTimeline.positionFromSeekBar(
                        progress = sb.progress,
                        max = sb.max,
                        durationMs = viewModel.uiState.value.duration
                    )
                )
                onSeekingChanged(false)
                onScheduleHideControls()
                stopSeekThumbnail()
            }
        })
    }

    fun release() {
        stopSeekThumbnail()
    }

    private fun maybeStartSeekThumbnail() {
        if (!playerPrefs.seekThumbnailEnabled) return
        val currentVideoUri = viewModel.currentVideoUri ?: return
        if (PlayerSeekThumbnailPolicy.shouldSkipThumbnail(currentVideoUri.scheme)) return
        seekThumbnailLoader = PlayerSeekThumbnailLoader(activity)
        thumbnailContainerProvider().visibility = View.VISIBLE
    }

    private fun stopSeekThumbnail() {
        thumbnailContainerProvider().visibility = View.GONE
        seekThumbnailLoader?.release()
        seekThumbnailLoader = null
    }

    private fun updateSeekThumbnail(sb: SeekBar, progress: Int) {
        val loader = seekThumbnailLoader ?: return
        val currentVideoUri = viewModel.currentVideoUri ?: return
        val duration = viewModel.uiState.value.duration
        val positionMs = PlayerSeekThumbnailPolicy.thumbnailPositionMs(progress, sb.max, duration)

        thumbnailTimeProvider().text = formatTime(positionMs)
        loader.loadThumbnail(currentVideoUri, positionMs) { bitmap ->
            if (bitmap != null) {
                thumbnailImageProvider().setImageBitmap(bitmap)
            }
        }

        val container = thumbnailContainerProvider()
        val usableWidth = sb.width - sb.paddingLeft - sb.paddingRight
        val pct = if (sb.max > 0) progress.toFloat() / sb.max else 0f
        val absoluteSeekBarLeft = bottomPanelProvider().left + progressRowProvider().left + sb.left
        val thumbXInParent = absoluteSeekBarLeft + sb.paddingLeft + (usableWidth * pct) - (container.width / 2f)
        container.translationX = thumbXInParent.coerceIn(
            0f,
            (controlsContainerProvider().width - container.width).toFloat()
        )
    }
}
