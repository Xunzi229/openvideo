package com.example.openvideo.ui.player

data class PlayerBackgroundServiceStartDecision(
    val shouldStart: Boolean
)

object PlayerBackgroundServicePolicy {

    fun startDecision(
        backgroundAudio: Boolean,
        isPlaying: Boolean,
        isActivityForeground: Boolean,
        isInPictureInPicture: Boolean
    ): PlayerBackgroundServiceStartDecision =
        PlayerBackgroundServiceStartDecision(
            shouldStart = backgroundAudio &&
                isPlaying &&
                !isActivityForeground &&
                !isInPictureInPicture
        )
}
