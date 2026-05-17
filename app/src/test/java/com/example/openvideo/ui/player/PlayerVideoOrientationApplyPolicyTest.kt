package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVideoOrientationApplyPolicyTest {

    @Test
    fun appliesWhenAutoEnabledAndDimensionsValid() {
        assertTrue(
            PlayerVideoOrientationApplyPolicy.shouldApply(
                autoOrientationByVideo = true,
                userOverrodeOrientation = false,
                width = 1920,
                height = 1080
            )
        )
    }

    @Test
    fun blocksWhenUserOverrodeOrAutoDisabled() {
        assertFalse(
            PlayerVideoOrientationApplyPolicy.shouldApply(
                autoOrientationByVideo = true,
                userOverrodeOrientation = true,
                width = 1920,
                height = 1080
            )
        )
        assertFalse(
            PlayerVideoOrientationApplyPolicy.shouldApply(
                autoOrientationByVideo = false,
                userOverrodeOrientation = false,
                width = 1920,
                height = 1080
            )
        )
    }
}
