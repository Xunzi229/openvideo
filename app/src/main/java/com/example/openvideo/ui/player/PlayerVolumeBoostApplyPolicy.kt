package com.example.openvideo.ui.player

object PlayerVolumeBoostApplyPolicy {
    fun shouldReapplyOnAudioSessionChange(volumeBoostEnabled: Boolean): Boolean =
        volumeBoostEnabled
}
