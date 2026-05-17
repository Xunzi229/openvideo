package com.example.openvideo.ui.player

import com.example.openvideo.core.player.DecodeMode

object PlayerDecodeModePolicy {
    fun decodeMode(softwareAudioDecoder: Boolean): DecodeMode =
        if (softwareAudioDecoder) DecodeMode.SOFT else DecodeMode.HARD
}
