package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLandscapeBadgePolicyTest {

    @Test
    fun fourKThresholdIsExactlyThirtyEightForty() {
        assertEquals(3840, PlayerLandscapeBadgePolicy.UHD_4K_MIN_WIDTH)
    }

    @Test
    fun widthAtOrAboveThresholdIs4k() {
        assertTrue(PlayerLandscapeBadgePolicy.is4kVideo(3840))
        assertTrue(PlayerLandscapeBadgePolicy.is4kVideo(4096))
        assertTrue(PlayerLandscapeBadgePolicy.is4kVideo(7680))
    }

    @Test
    fun smallerWidthsAreNot4k() {
        assertFalse(PlayerLandscapeBadgePolicy.is4kVideo(0))
        assertFalse(PlayerLandscapeBadgePolicy.is4kVideo(-1))
        assertFalse(PlayerLandscapeBadgePolicy.is4kVideo(1920))
        assertFalse(PlayerLandscapeBadgePolicy.is4kVideo(2560))
        assertFalse(PlayerLandscapeBadgePolicy.is4kVideo(3839))
    }
}
