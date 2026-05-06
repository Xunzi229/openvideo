package com.example.openvideo.ui.player

import android.content.pm.ActivityInfo
import com.example.openvideo.R

data class ControlVisibility(
    val chromeVisible: Boolean,
    val lockButtonVisible: Boolean,
    val lockButtonSelected: Boolean
)

data class LockButtonPlacement(
    val startMarginDp: Int,
    val sizeDp: Int,
    val constrainToVerticalCenter: Boolean
)

data class DialogBounds(
    val width: Int,
    val height: Int
)

object PlayerControlState {
    fun playPauseIcon(isPlayingOrRequested: Boolean): Int =
        if (isPlayingOrRequested) R.drawable.ic_pause else R.drawable.ic_play

    fun visibilityFor(isLocked: Boolean, controlsVisible: Boolean): ControlVisibility =
        ControlVisibility(
            chromeVisible = controlsVisible && !isLocked,
            lockButtonVisible = controlsVisible || isLocked,
            lockButtonSelected = isLocked
        )

    fun lockButtonPlacement(): LockButtonPlacement =
        LockButtonPlacement(
            startMarginDp = 18,
            sizeDp = 52,
            constrainToVerticalCenter = true
        )
}

object PlayerOrientationPolicy {
    fun defaultOrientation(): Int = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}

object PlayerSettingsLayoutPolicy {
    fun dialogBounds(screenWidth: Int, screenHeight: Int): DialogBounds =
        DialogBounds(
            width = (screenWidth * 0.75f).toInt(),
            height = (screenHeight * 0.70f).toInt()
        )
}
