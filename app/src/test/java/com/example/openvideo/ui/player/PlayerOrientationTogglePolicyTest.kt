package com.example.openvideo.ui.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerOrientationTogglePolicyTest {

    @Test
    fun portraitRequestsLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            PlayerOrientationTogglePolicy.nextRequestedOrientation(Configuration.ORIENTATION_PORTRAIT)
        )
    }

    @Test
    fun landscapeRequestsPortrait() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            PlayerOrientationTogglePolicy.nextRequestedOrientation(Configuration.ORIENTATION_LANDSCAPE)
        )
    }

    @Test
    fun undefinedDefaultsToPortrait() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            PlayerOrientationTogglePolicy.nextRequestedOrientation(Configuration.ORIENTATION_UNDEFINED)
        )
    }
}
