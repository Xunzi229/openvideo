package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerMediaTracksPolicyTest {

    @Test
    fun detectsVideoTrackType() {
        assertTrue(PlayerMediaTracksPolicy.hasVideoTrack(listOf(1, 2), videoTrackType = 2))
        assertFalse(PlayerMediaTracksPolicy.hasVideoTrack(listOf(1, 3), videoTrackType = 2))
    }
}
