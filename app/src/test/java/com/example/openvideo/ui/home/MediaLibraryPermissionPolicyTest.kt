package com.example.openvideo.ui.home

import android.Manifest
import android.os.Build
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryPermissionPolicyTest {

    @Test
    fun requiresModernVideoPermissionsOnAndroid14() {
        val permissions = MediaLibraryPermissionPolicy.requiredPermissions(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            permissions
        )
    }

    @Test
    fun grantsReadAccessWhenAnyRequiredPermissionIsGranted() {
        val granted = MediaLibraryPermissionPolicy.hasReadAccess(
            isPermissionGranted = { permission ->
                permission == Manifest.permission.READ_MEDIA_VIDEO
            },
            sdkInt = Build.VERSION_CODES.TIRAMISU
        )

        assertTrue(granted)
    }

    @Test
    fun deniesReadAccessWhenNoRequiredPermissionIsGranted() {
        val granted = MediaLibraryPermissionPolicy.hasReadAccess(
            isPermissionGranted = { false },
            sdkInt = Build.VERSION_CODES.TIRAMISU
        )

        assertFalse(granted)
    }
}
