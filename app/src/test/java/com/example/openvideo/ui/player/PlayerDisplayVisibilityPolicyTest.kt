package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerDisplayVisibilityPolicyTest {

    @Test
    fun videoLayerAlphaReflectsToggle() {
        assertEquals(1f, PlayerDisplayVisibilityPolicy.videoLayerAlpha(true))
        assertEquals(0f, PlayerDisplayVisibilityPolicy.videoLayerAlpha(false))
    }
}
