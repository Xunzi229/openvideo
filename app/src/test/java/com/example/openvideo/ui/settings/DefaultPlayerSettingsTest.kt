package com.example.openvideo.ui.settings

import com.example.openvideo.core.prefs.AspectRatio
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultPlayerSettingsTest {

    @Test
    fun keepsSupportedPlaybackSpeeds() {
        assertEquals(0.5f, DefaultPlayerSettings.supportedSpeedOrDefault(0.5f))
        assertEquals(1.25f, DefaultPlayerSettings.supportedSpeedOrDefault(1.25f))
        assertEquals(1.0f, DefaultPlayerSettings.supportedSpeedOrDefault(1.1f))
    }

    @Test
    fun keepsAllAspectRatiosAsPlayerDefaults() {
        AspectRatio.entries.forEach { ratio ->
            assertEquals(ratio, DefaultPlayerSettings.aspectRatioOrDefault(ratio))
        }
    }
}
