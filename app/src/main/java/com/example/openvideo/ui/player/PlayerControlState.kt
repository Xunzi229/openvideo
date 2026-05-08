package com.example.openvideo.ui.player

import android.content.pm.ActivityInfo
import android.view.Gravity
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

    fun initialOrientationForVideo(
        width: Int,
        height: Int,
        autoOrientationByVideo: Boolean
    ): Int {
        if (!autoOrientationByVideo) return defaultOrientation()
        return orientationForVideo(width, height)
    }

    fun orientationForVideo(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return defaultOrientation()

        val ratio = width.toFloat() / height.toFloat()
        return when {
            ratio >= 1.2f -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ratio <= 0.8f -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }
}

object PlayerSettingsLayoutPolicy {
    private const val LANDSCAPE_MARGIN_PX = 32
    private const val LANDSCAPE_MAX_WIDTH_PX = 520
    private const val LANDSCAPE_WIDTH_RATIO = 0.34f

    fun dialogBounds(screenWidth: Int, screenHeight: Int): DialogBounds =
        panelBounds(screenWidth, screenHeight)

    fun panelBounds(screenWidth: Int, screenHeight: Int): DialogBounds =
        if (screenWidth > screenHeight) {
            val marginPx = landscapeMarginPx(screenWidth, screenHeight)
            DialogBounds(
                width = (screenWidth * LANDSCAPE_WIDTH_RATIO).toInt().coerceAtMost(LANDSCAPE_MAX_WIDTH_PX),
                height = screenHeight - marginPx * 2
            )
        } else {
            DialogBounds(
                width = screenWidth,
                height = (screenHeight * 0.60f).toInt()
            )
        }

    fun panelGravity(screenWidth: Int, screenHeight: Int): Int =
        if (screenWidth > screenHeight) Gravity.END or Gravity.CENTER_VERTICAL else Gravity.BOTTOM

    fun landscapeMarginPx(screenWidth: Int, screenHeight: Int): Int =
        if (screenWidth > screenHeight) LANDSCAPE_MARGIN_PX else 0

    fun navigationNeedsScroll(
        availableHeightDp: Int,
        itemCount: Int,
        itemHeightDp: Int = 42,
        itemSpacingDp: Int = 4,
        titleHeightDp: Int = 40,
        resetHeightDp: Int = 44,
        verticalPaddingDp: Int = 32
    ): Boolean {
        val navHeight = itemCount * itemHeightDp + (itemCount - 1).coerceAtLeast(0) * itemSpacingDp
        return titleHeightDp + navHeight + resetHeightDp + verticalPaddingDp > availableHeightDp
    }
}
