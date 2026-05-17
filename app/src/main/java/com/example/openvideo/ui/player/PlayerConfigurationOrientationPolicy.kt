package com.example.openvideo.ui.player

import android.content.res.Configuration

object PlayerConfigurationOrientationPolicy {
    fun isLandscape(orientation: Int): Boolean =
        orientation == Configuration.ORIENTATION_LANDSCAPE
}
