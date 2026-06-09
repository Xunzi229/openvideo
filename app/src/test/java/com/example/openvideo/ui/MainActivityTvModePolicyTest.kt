package com.example.openvideo.ui

import android.content.res.Configuration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityTvModePolicyTest {

    @Test
    fun enablesTvModeForTelevisionUiMode() {
        assertTrue(
            MainActivityTvModePolicy.isTvMode(
                uiMode = Configuration.UI_MODE_TYPE_TELEVISION,
                hasLeanbackFeature = false
            )
        )
    }

    @Test
    fun enablesTvModeForLeanbackDevices() {
        assertTrue(
            MainActivityTvModePolicy.isTvMode(
                uiMode = Configuration.UI_MODE_TYPE_NORMAL,
                hasLeanbackFeature = true
            )
        )
    }

    @Test
    fun keepsPhoneModeForNormalNonLeanbackDevices() {
        assertFalse(
            MainActivityTvModePolicy.isTvMode(
                uiMode = Configuration.UI_MODE_TYPE_NORMAL,
                hasLeanbackFeature = false
            )
        )
    }
}
