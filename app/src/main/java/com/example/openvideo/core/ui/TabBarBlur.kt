package com.example.openvideo.core.ui

import android.os.Build
import android.view.View

object TabBarBlur {
    fun bind(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        // Public SDK only exposes Window.setBackgroundBlurRadius (behind the activity).
        // Tab bar frost is the translucent ov_tab_bar_fill in values-v31.
        view.elevation = 0f
    }
}
