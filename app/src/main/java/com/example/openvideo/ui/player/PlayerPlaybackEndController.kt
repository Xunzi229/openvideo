package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.data.model.VideoItem

class PlayerPlaybackEndController(
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val abLoopStateProvider: () -> PlayerAbLoopState,
    private val abLoopPointAProvider: () -> Long,
    private val onSwitchSessionVideo: (VideoItem, () -> Unit) -> Unit,
    private val onCurrentVideoChanged: (VideoItem) -> Unit,
    private val onLoadSubtitles: (String, String) -> Unit,
    private val onApplyPlayerSettings: () -> Unit,
    private val onScheduleHideControls: () -> Unit,
    private val onShowControls: () -> Unit,
    private val onFinishPlayer: () -> Unit
) {
    private var isSwitchingQueueAfterEnded = false

    fun handleEnded() {
        if (isSwitchingQueueAfterEnded) return
        val queue = viewModel.sessionQueue.value
        val currentIndex = queue.indexOfFirst { it.id == viewModel.playingVideoId }
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = currentIndex,
            queueSize = queue.size,
            autoPlayNext = playerPrefs.autoPlayNext,
            loopMode = playerPrefs.loopMode,
            abLoopState = abLoopStateProvider(),
            abLoopPointA = abLoopPointAProvider(),
            endBehavior = playerPrefs.playbackEndBehavior
        )

        when (decision.action) {
            PlayerPlaybackEndAction.PLAY_NEXT -> playNextQueueVideoAfterEnded(queue, decision.nextIndex)
            PlayerPlaybackEndAction.REPLAY_CURRENT -> {
                viewModel.seekTo(decision.seekPositionMs ?: 0L)
                viewModel.player?.play()
                onScheduleHideControls()
            }
            PlayerPlaybackEndAction.STOP_AT_END -> {
                viewModel.saveHistory()
                onShowControls()
            }
            PlayerPlaybackEndAction.RETURN_TO_LIST -> {
                viewModel.saveHistory()
                onFinishPlayer()
            }
        }
    }

    private fun playNextQueueVideoAfterEnded(queue: List<VideoItem>, nextIndex: Int?) {
        if (nextIndex == null) return

        isSwitchingQueueAfterEnded = true
        val item = queue[nextIndex]
        onSwitchSessionVideo(item) {
            isSwitchingQueueAfterEnded = false
            onCurrentVideoChanged(item)
            onLoadSubtitles(
                playerPrefs.externalSubtitleUri.ifBlank { item.uri.toString() },
                item.path
            )
            onApplyPlayerSettings()
            onScheduleHideControls()
        }
    }
}
