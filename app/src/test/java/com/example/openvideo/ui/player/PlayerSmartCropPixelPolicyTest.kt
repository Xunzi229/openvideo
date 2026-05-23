package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSmartCropPixelPolicyTest {

    @Test
    fun transparentBlackCountsAsBlackBorder() {
        assertTrue(PlayerSmartCropPixelPolicy.isBlackArgb(0x00000000))
    }

    @Test
    fun opaqueNearBlackCountsAsBlackBorder() {
        assertTrue(PlayerSmartCropPixelPolicy.isBlackArgb(0xff101010.toInt()))
    }

    @Test
    fun brightPixelCountsAsContent() {
        assertFalse(PlayerSmartCropPixelPolicy.isBlackArgb(0xff808080.toInt()))
    }
}
