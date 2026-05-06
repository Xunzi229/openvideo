package com.example.openvideo.ui.player

import androidx.media3.common.Player
import com.example.openvideo.core.prefs.LoopMode

object PlayerPlaybackSettings {

    fun repeatModeFor(loopMode: LoopMode): Int {
        return when (loopMode) {
            LoopMode.OFF -> Player.REPEAT_MODE_OFF
            LoopMode.SINGLE -> Player.REPEAT_MODE_ONE
            LoopMode.LIST -> Player.REPEAT_MODE_ALL
        }
    }

    fun pitchFor(speed: Float, preservePitch: Boolean): Float {
        return if (preservePitch) 1.0f else speed
    }
}
