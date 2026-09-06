package com.openvideo.app.ui.player

object PlayerVolumeBoostApplyPolicy {
    fun shouldReapplyOnAudioSessionChange(volumeBoostEnabled: Boolean): Boolean =
        volumeBoostEnabled
}
