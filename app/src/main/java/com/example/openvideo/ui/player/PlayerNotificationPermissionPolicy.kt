package com.example.openvideo.ui.player

import android.os.Build

object PlayerNotificationPermissionPolicy {
    fun requiresRuntimePermission(sdkInt: Int): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU

    fun shouldRequestPermission(
        requiresPermission: Boolean,
        granted: Boolean,
        requestedBefore: Boolean,
        notificationEnabled: Boolean
    ): Boolean =
        requiresPermission && !granted && !requestedBefore && notificationEnabled
}
