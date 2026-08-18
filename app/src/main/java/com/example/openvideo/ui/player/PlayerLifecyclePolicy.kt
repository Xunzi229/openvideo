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

data class PlayerDestroyLifecycleDecision(
    val keepPlaybackSession: Boolean,
    val releasePlayer: Boolean,
    val dismissPlaybackNotification: Boolean
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

    fun onDestroy(
        activityIsFinishing: Boolean,
        exitRequested: Boolean,
        backgroundAudio: Boolean,
        hasPlayer: Boolean,
        playWhenReady: Boolean
    ): PlayerDestroyLifecycleDecision {
        val keepSession = !activityIsFinishing && !exitRequested && backgroundAudio && hasPlayer && playWhenReady
        return PlayerDestroyLifecycleDecision(
            keepPlaybackSession = keepSession,
            releasePlayer = !keepSession,
            dismissPlaybackNotification = !keepSession
        )
    }
}
