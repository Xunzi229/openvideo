package com.openvideo.app.ui.player

import com.openvideo.app.core.player.DecodeMode

object PlayerDecodeModePolicy {
    fun decodeMode(softwareAudioDecoder: Boolean): DecodeMode =
        if (softwareAudioDecoder) DecodeMode.SOFT else DecodeMode.HARD
}
