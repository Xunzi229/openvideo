package com.example.openvideo.ui.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.openvideo.core.prefs.AspectRatio

object PlayerViewSettings {

    @OptIn(UnstableApi::class)
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

    /** When non-null, [AspectRatioFrameLayout.setAspectRatio] must use this (not only video DAR). */
    fun forcedContentAspectRatio(aspectRatio: AspectRatio): Float? = when (aspectRatio) {
        AspectRatio.RATIO_16_9 -> 16f / 9f
        AspectRatio.RATIO_4_3 -> 4f / 3f
        else -> null
    }
}
