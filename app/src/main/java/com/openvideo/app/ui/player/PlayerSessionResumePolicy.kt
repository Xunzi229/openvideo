package com.openvideo.app.ui.player

object PlayerSessionResumePolicy {
    fun shouldRestorePlaybackPosition(rememberProgress: Boolean): Boolean = rememberProgress
}
