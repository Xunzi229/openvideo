package com.example.openvideo.ui.player

import android.os.Build

object PlayerNotificationPermissionPolicy {
    fun requiresRuntimePermission(sdkInt: Int): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU

    fun shouldRequestPermission(requiresPermission: Boolean, granted: Boolean): Boolean =
        requiresPermission && !granted
}
