package com.openvideo.app.ui.player

object PlayerNotificationRefreshPolicy {
    fun shouldRefreshBackgroundPlayback(
        isFinishing: Boolean,
        backgroundAudio: Boolean,
        notificationEnabled: Boolean,
        isActivityForeground: Boolean
    ): Boolean =
        !isFinishing &&
            backgroundAudio &&
            notificationEnabled &&
            !isActivityForeground
}
