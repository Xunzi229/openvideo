package com.example.openvideo.core.ui

import android.os.Build
import android.view.View

object TabBarBlur {
    fun bind(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val radiusPx = (25f * view.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        view.setBackgroundBlurRadius(radiusPx)
    }
}
