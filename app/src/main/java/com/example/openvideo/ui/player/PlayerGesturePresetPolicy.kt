package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.DoubleTapAction
import com.example.openvideo.core.prefs.GestureAction
import com.example.openvideo.core.prefs.LongPressAction

enum class PlayerGesturePreset {
    CLASSIC,
    MINIMAL,
    BINGE,
    POWER_USER
}

data class PlayerGesturePresetSettings(
    val leftVerticalGesture: GestureAction,
    val rightVerticalGesture: GestureAction,
    val horizontalSwipeAction: GestureAction,
    val doubleTapAction: DoubleTapAction,
    val longPressAction: LongPressAction,
    val gestureSensitivity: Int,
    val edgeSwipeBack: Boolean
)

object PlayerGesturePresetPolicy {
    fun settingsFor(preset: PlayerGesturePreset): PlayerGesturePresetSettings = when (preset) {
        PlayerGesturePreset.CLASSIC -> PlayerGesturePresetSettings(
            leftVerticalGesture = GestureAction.BRIGHTNESS,
            rightVerticalGesture = GestureAction.VOLUME,
            horizontalSwipeAction = GestureAction.SEEK,
            doubleTapAction = DoubleTapAction.PLAY_PAUSE,
            longPressAction = LongPressAction.SPEED,
            gestureSensitivity = 2,
            edgeSwipeBack = false
        )
        PlayerGesturePreset.MINIMAL -> PlayerGesturePresetSettings(
            leftVerticalGesture = GestureAction.NONE,
            rightVerticalGesture = GestureAction.NONE,
            horizontalSwipeAction = GestureAction.NONE,
            doubleTapAction = DoubleTapAction.PLAY_PAUSE,
            longPressAction = LongPressAction.NONE,
            gestureSensitivity = 1,
            edgeSwipeBack = true
        )
        PlayerGesturePreset.BINGE -> PlayerGesturePresetSettings(
            leftVerticalGesture = GestureAction.BRIGHTNESS,
            rightVerticalGesture = GestureAction.VOLUME,
            horizontalSwipeAction = GestureAction.SEEK,
            doubleTapAction = DoubleTapAction.PLAY_PAUSE,
            longPressAction = LongPressAction.SPEED,
            gestureSensitivity = 1,
            edgeSwipeBack = false
        )
        PlayerGesturePreset.POWER_USER -> PlayerGesturePresetSettings(
            leftVerticalGesture = GestureAction.BRIGHTNESS,
            rightVerticalGesture = GestureAction.VOLUME,
            horizontalSwipeAction = GestureAction.SEEK,
            doubleTapAction = DoubleTapAction.FORWARD,
            longPressAction = LongPressAction.SPEED,
            gestureSensitivity = 3,
            edgeSwipeBack = true
        )
    }
}
