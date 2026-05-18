package com.example.openvideo.ui.player

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerNotificationPermissionPolicyTest {

    @Test
    fun android13RequiresRuntimeNotificationPermission() {
        assertFalse(PlayerNotificationPermissionPolicy.requiresRuntimePermission(Build.VERSION_CODES.S_V2))
        assertTrue(PlayerNotificationPermissionPolicy.requiresRuntimePermission(Build.VERSION_CODES.TIRAMISU))
    }

    @Test
    fun requestsOnlyWhenRuntimePermissionIsNeededAndNeverAskedBefore() {
        assertTrue(
            PlayerNotificationPermissionPolicy.shouldRequestPermission(
                requiresPermission = true,
                granted = false,
                requestedBefore = false,
                notificationEnabled = true
            )
        )

        assertFalse(
            PlayerNotificationPermissionPolicy.shouldRequestPermission(
                requiresPermission = true,
                granted = false,
                requestedBefore = true,
                notificationEnabled = true
            )
        )
    }

    @Test
    fun doesNotRequestWhenPermissionIsGrantedOrNotificationToggleIsOff() {
        assertFalse(
            PlayerNotificationPermissionPolicy.shouldRequestPermission(
                requiresPermission = true,
                granted = true,
                requestedBefore = false,
                notificationEnabled = true
            )
        )

        assertFalse(
            PlayerNotificationPermissionPolicy.shouldRequestPermission(
                requiresPermission = true,
                granted = false,
                requestedBefore = false,
                notificationEnabled = false
            )
        )
    }
}
