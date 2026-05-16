package com.example.openvideo.ui.home

import android.Manifest
import android.os.Build

object MediaLibraryPermissionPolicy {

    fun hasReadAccess(
        isPermissionGranted: (String) -> Boolean,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Boolean = requiredPermissions(sdkInt).any(isPermissionGranted)

    fun requiredPermissions(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> = when {
        sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        sdkInt >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
