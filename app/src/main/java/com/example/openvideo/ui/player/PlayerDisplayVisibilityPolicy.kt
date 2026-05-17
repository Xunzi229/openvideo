package com.example.openvideo.ui.player

object PlayerDisplayVisibilityPolicy {
    fun videoLayerAlpha(videoDisplayEnabled: Boolean): Float = if (videoDisplayEnabled) 1f else 0f
}
