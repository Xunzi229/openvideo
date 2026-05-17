package com.example.openvideo.ui.player

object PlayerSessionResumePolicy {
    fun shouldRestorePlaybackPosition(rememberProgress: Boolean): Boolean = rememberProgress
}
