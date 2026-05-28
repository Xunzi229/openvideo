package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.PlayerPrefs

class PlayerLifecycleController(
    private val viewModel: PlayerViewModel,
    private val playerPrefs: PlayerPrefs,
    private val isInPictureInPictureProvider: () -> Boolean,
    private val isFinishingProvider: () -> Boolean,
    private val onStopPlaybackService: () -> Unit,
    private val onReattachPlayerSurface: () -> Unit,
    private val onApplyDisplaySettings: () -> Unit,
    private val onObserveState: () -> Unit,
    private val onStopObservingState: () -> Unit,
    private val onUnlockPlayerForPause: () -> Unit,
    private val onDismissPlaybackNotification: () -> Unit,
    private val onStartPlaybackServiceIfNeeded: (Boolean) -> Unit,
    private val onActivityForegroundChanged: (Boolean) -> Unit
) {
    fun onResume() {
        onActivityForegroundChanged(true)
        val decision = PlayerLifecyclePolicy.onResume()
        if (decision.stopPlaybackService) onStopPlaybackService()
        onReattachPlayerSurface()
        onApplyDisplaySettings()
        if (decision.observeState) onObserveState()
    }

    fun onPause() {
        onActivityForegroundChanged(false)
        onStopObservingState()
        val decision = PlayerLifecyclePolicy.onPause(
            isInPictureInPicture = isInPictureInPictureProvider(),
            pauseOnExit = playerPrefs.pauseOnExit,
            backgroundAudio = playerPrefs.bgAudio,
            isPlaying = viewModel.player?.isPlaying == true
        )
        if (decision.saveHistory) viewModel.saveHistory()
        if (decision.pausePlayer) {
            if (decision.unlockBeforePause) onUnlockPlayerForPause()
            viewModel.player?.pause()
        }
        if (isFinishingProvider()) {
            onDismissPlaybackNotification()
        } else {
            if (decision.stopPlaybackService) onStopPlaybackService()
            if (decision.startPlaybackService) onStartPlaybackServiceIfNeeded(true)
        }
    }
}
