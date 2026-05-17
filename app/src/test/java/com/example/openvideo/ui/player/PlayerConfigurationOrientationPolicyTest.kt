package com.example.openvideo.ui.player

import android.content.res.Configuration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerConfigurationOrientationPolicyTest {

    @Test
    fun detectsLandscape() {
        assertTrue(
            PlayerConfigurationOrientationPolicy.isLandscape(Configuration.ORIENTATION_LANDSCAPE)
        )
        assertFalse(
            PlayerConfigurationOrientationPolicy.isLandscape(Configuration.ORIENTATION_PORTRAIT)
        )
    }
}
