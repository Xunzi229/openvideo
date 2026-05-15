package com.example.openvideo.ui.player

data class PlayPauseToggleDecision(
    val targetPlayWhenReady: Boolean,
    val iconRes: Int
)

object PlayerPlayPausePolicy {
    fun iconFor(isPlaying: Boolean, playWhenReady: Boolean): Int =
        PlayerControlState.playPauseIcon(isPlayingOrRequested = isPlaying || playWhenReady)

    fun toggleDecision(isPlaying: Boolean, playWhenReady: Boolean): PlayPauseToggleDecision {
        val targetPlayWhenReady = !(isPlaying || playWhenReady)
        return PlayPauseToggleDecision(
            targetPlayWhenReady = targetPlayWhenReady,
            iconRes = PlayerControlState.playPauseIcon(isPlayingOrRequested = targetPlayWhenReady)
        )
    }
}
