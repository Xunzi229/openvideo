package com.example.openvideo.ui.player

import androidx.annotation.StringRes
import androidx.media3.common.Player
import com.example.openvideo.R

object PlayerNetworkStatusPolicy {
    data class Presentation(
        val visible: Boolean,
        @param:StringRes val labelRes: Int = 0
    )

    fun present(
        playbackState: Int,
        isNetworkUri: Boolean,
        isLive: Boolean,
        durationMs: Long,
        autoRetryPending: Boolean
    ): Presentation {
        if (!isNetworkUri) return Presentation(visible = false)
        if (autoRetryPending) {
            return Presentation(visible = true, labelRes = R.string.player_network_status_reconnecting)
        }
        if (playbackState == Player.STATE_BUFFERING) {
            return Presentation(visible = true, labelRes = R.string.player_network_status_buffering)
        }
        if (playbackState == Player.STATE_READY && (isLive || !PlayerTimeline.hasSeekableDuration(durationMs))) {
            return Presentation(visible = true, labelRes = R.string.player_network_status_live)
        }
        return Presentation(visible = false)
    }
}
