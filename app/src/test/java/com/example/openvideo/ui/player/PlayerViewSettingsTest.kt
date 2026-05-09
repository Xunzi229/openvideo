package com.example.openvideo.ui.player

import androidx.media3.ui.AspectRatioFrameLayout
import com.example.openvideo.core.prefs.AspectRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerViewSettingsTest {

    @Test
    fun mapsPreferredAspectRatiosToPlayerResizeModes() {
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, PlayerViewSettings.resizeModeFor(AspectRatio.FIT))
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FILL, PlayerViewSettings.resizeModeFor(AspectRatio.FILL))
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, PlayerViewSettings.resizeModeFor(AspectRatio.CROP))
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FILL, PlayerViewSettings.resizeModeFor(AspectRatio.STRETCH))
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, PlayerViewSettings.resizeModeFor(AspectRatio.RATIO_4_3))
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, PlayerViewSettings.resizeModeFor(AspectRatio.RATIO_16_9))
    }

    @Test
    fun forcedContentAspectRatioOnlyForFixedTargets() {
        assertNull(PlayerViewSettings.forcedContentAspectRatio(AspectRatio.FIT))
        assertNull(PlayerViewSettings.forcedContentAspectRatio(AspectRatio.FILL))
        assertNull(PlayerViewSettings.forcedContentAspectRatio(AspectRatio.CROP))
        assertNull(PlayerViewSettings.forcedContentAspectRatio(AspectRatio.STRETCH))
        assertEquals(4f / 3f, PlayerViewSettings.forcedContentAspectRatio(AspectRatio.RATIO_4_3)!!, 0.0001f)
        assertEquals(16f / 9f, PlayerViewSettings.forcedContentAspectRatio(AspectRatio.RATIO_16_9)!!, 0.0001f)
    }
}
