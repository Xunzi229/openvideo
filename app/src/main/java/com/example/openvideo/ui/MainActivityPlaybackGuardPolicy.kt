package com.example.openvideo.ui

data class MainActivityPlaybackGuardDecision(
    val pausePlayer: Boolean,
    val stopPlaybackService: Boolean
)

object MainActivityPlaybackGuardPolicy {

    fun onResume(
        backgroundAudio: Boolean,
        playerExists: Boolean,
        isPlayingOrRequested: Boolean
    ): MainActivityPlaybackGuardDecision {
        val shouldPause = playerExists && isPlayingOrRequested && !backgroundAudio
        return MainActivityPlaybackGuardDecision(
            pausePlayer = shouldPause,
            stopPlaybackService = shouldPause
        )
    }
}
