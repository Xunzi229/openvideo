package com.example.openvideo.ui.player

import androidx.media3.common.Player
import com.example.openvideo.core.prefs.LoopMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerPlaybackSettingsTest {

    @Test
    fun mapsLoopModeToPlayerRepeatMode() {
        assertEquals(Player.REPEAT_MODE_OFF, PlayerPlaybackSettings.repeatModeFor(LoopMode.OFF))
        assertEquals(Player.REPEAT_MODE_ONE, PlayerPlaybackSettings.repeatModeFor(LoopMode.SINGLE))
        assertEquals(Player.REPEAT_MODE_ALL, PlayerPlaybackSettings.repeatModeFor(LoopMode.LIST))
    }

    @Test
    fun usesNormalPitchWhenPreservingPitch() {
        assertEquals(1.0f, PlayerPlaybackSettings.pitchFor(speed = 1.5f, preservePitch = true))
    }

    @Test
    fun followsSpeedAsPitchWhenNotPreservingPitch() {
        assertEquals(1.5f, PlayerPlaybackSettings.pitchFor(speed = 1.5f, preservePitch = false))
    }
}
