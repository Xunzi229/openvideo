package com.openvideo.app.ui.player

import android.graphics.Color
import com.openvideo.app.core.prefs.SubtitleBgStyle

object PlayerSubtitleStylePolicy {
    fun backgroundColor(style: SubtitleBgStyle): Int =
        when (style) {
            SubtitleBgStyle.NONE -> Color.TRANSPARENT
            SubtitleBgStyle.SEMI_TRANSPARENT -> Color.argb(170, 0, 0, 0)
            SubtitleBgStyle.OPAQUE -> Color.BLACK
        }
}
