package com.example.openvideo.ui

import android.content.res.Configuration

object MainActivityTvModePolicy {

    fun isTvMode(uiMode: Int, hasLeanbackFeature: Boolean): Boolean {
        val uiModeType = uiMode and Configuration.UI_MODE_TYPE_MASK
        return uiModeType == Configuration.UI_MODE_TYPE_TELEVISION || hasLeanbackFeature
    }
}
