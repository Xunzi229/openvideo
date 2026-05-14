package com.example.openvideo.ui.player

data class PlayerPauseLifecycleDecision(
    val saveHistory: Boolean,
    val pausePlayer: Boolean,
    val unlockBeforePause: Boolean,
    val stopPlaybackService: Boolean,
    val startPlaybackService: Boolean
)

data class PlayerResumeLifecycleDecision(
    val stopPlaybackService: Boolean,
    val observeState: Boolean
)

object PlayerLifecyclePolicy {

    fun onPause(
        isInPictureInPicture: Boolean,
        pauseOnExit: Boolean,
        backgroundAudio: Boolean,
        isPlaying: Boolean
    ): PlayerPauseLifecycleDecision {
        if (isInPictureInPicture) {
            return PlayerPauseLifecycleDecision(
                saveHistory = false,
                pausePlayer = false,
                unlockBeforePause = false,
                stopPlaybackService = false,
                startPlaybackService = false
            )
        }

        val shouldPause = pauseOnExit || !backgroundAudio
        return PlayerPauseLifecycleDecision(
            saveHistory = true,
            pausePlayer = shouldPause,
            unlockBeforePause = shouldPause,
            stopPlaybackService = shouldPause,
            startPlaybackService = !shouldPause && backgroundAudio && isPlaying
        )
    }

    fun onResume(): PlayerResumeLifecycleDecision =
        PlayerResumeLifecycleDecision(
            stopPlaybackService = true,
            observeState = true
        )
}
