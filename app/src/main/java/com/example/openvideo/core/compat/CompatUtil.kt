package com.example.openvideo.core.compat

import android.os.Build

object CompatUtil {

    fun isAtLeastApi(api: Int): Boolean = Build.VERSION.SDK_INT >= api

    // API 26 (O) - Picture-in-Picture
    fun isPiPSupported(): Boolean = isAtLeastApi(26)

    // API 28 (P) - Display Cutout
    fun isCutoutApiAvailable(): Boolean = isAtLeastApi(28)

    // API 29 (Q) - Foreground service type, Video effects
    fun isForegroundServiceTypeNeeded(): Boolean = isAtLeastApi(29)
    fun isVideoEffectsSupported(): Boolean = isAtLeastApi(29)

    // API 30 (R) - WindowInsets.Type
    fun isWindowInsetsApiAvailable(): Boolean = isAtLeastApi(30)

    // API 33 (T) - READ_MEDIA_VIDEO, POST_NOTIFICATIONS
    fun isReadMediaVideoNeeded(): Boolean = isAtLeastApi(33)
    fun isNotificationPermissionNeeded(): Boolean = isAtLeastApi(33)

    // API 18 (JB MR2) - Screen rotation
    fun isAutoRotationSupported(): Boolean = isAtLeastApi(18)
}
