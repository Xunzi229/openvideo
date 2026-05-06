package com.example.openvideo.ui.player

import androidx.media3.ui.AspectRatioFrameLayout
import com.example.openvideo.core.prefs.AspectRatio

object PlayerViewSettings {

    fun resizeModeFor(aspectRatio: AspectRatio): Int {
        return when (aspectRatio) {
            AspectRatio.FIT,
            AspectRatio.RATIO_4_3,
            AspectRatio.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            AspectRatio.FILL,
            AspectRatio.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectRatio.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }
}
