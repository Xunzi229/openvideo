package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.DoubleTapAction
import com.example.openvideo.core.prefs.GestureAction
import com.example.openvideo.core.prefs.LongPressAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGesturePresetPolicyTest {

    @Test
    fun classicPresetMatchesDefaultErgonomicPlayerGestures() {
        val preset = PlayerGesturePresetPolicy.settingsFor(PlayerGesturePreset.CLASSIC)

        assertEquals(GestureAction.BRIGHTNESS, preset.leftVerticalGesture)
        assertEquals(GestureAction.VOLUME, preset.rightVerticalGesture)
        assertEquals(GestureAction.SEEK, preset.horizontalSwipeAction)
        assertEquals(DoubleTapAction.PLAY_PAUSE, preset.doubleTapAction)
        assertEquals(LongPressAction.SPEED, preset.longPressAction)
        assertEquals(2, preset.gestureSensitivity)
        assertFalse(preset.edgeSwipeBack)
    }

    @Test
    fun minimalPresetDisablesAccidentalPlaybackChangingGestures() {
        val preset = PlayerGesturePresetPolicy.settingsFor(PlayerGesturePreset.MINIMAL)

        assertEquals(GestureAction.NONE, preset.leftVerticalGesture)
        assertEquals(GestureAction.NONE, preset.rightVerticalGesture)
        assertEquals(GestureAction.NONE, preset.horizontalSwipeAction)
        assertEquals(DoubleTapAction.PLAY_PAUSE, preset.doubleTapAction)
        assertEquals(LongPressAction.NONE, preset.longPressAction)
        assertEquals(1, preset.gestureSensitivity)
        assertTrue(preset.edgeSwipeBack)
    }

    @Test
    fun bingePresetKeepsVolumeAndBrightnessButAvoidsSurpriseDoubleTapSeeking() {
        val preset = PlayerGesturePresetPolicy.settingsFor(PlayerGesturePreset.BINGE)

        assertEquals(GestureAction.BRIGHTNESS, preset.leftVerticalGesture)
        assertEquals(GestureAction.VOLUME, preset.rightVerticalGesture)
        assertEquals(GestureAction.SEEK, preset.horizontalSwipeAction)
        assertEquals(DoubleTapAction.PLAY_PAUSE, preset.doubleTapAction)
        assertEquals(LongPressAction.SPEED, preset.longPressAction)
        assertEquals(1, preset.gestureSensitivity)
        assertFalse(preset.edgeSwipeBack)
    }

    @Test
    fun powerUserPresetEnablesFastSeekHeavyControls() {
        val preset = PlayerGesturePresetPolicy.settingsFor(PlayerGesturePreset.POWER_USER)

        assertEquals(GestureAction.BRIGHTNESS, preset.leftVerticalGesture)
        assertEquals(GestureAction.VOLUME, preset.rightVerticalGesture)
        assertEquals(GestureAction.SEEK, preset.horizontalSwipeAction)
        assertEquals(DoubleTapAction.FORWARD, preset.doubleTapAction)
        assertEquals(LongPressAction.SPEED, preset.longPressAction)
        assertEquals(3, preset.gestureSensitivity)
        assertTrue(preset.edgeSwipeBack)
    }
}
