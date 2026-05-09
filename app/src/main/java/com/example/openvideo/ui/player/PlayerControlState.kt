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

object PlayerDisplayAdjustment {
    fun screenBrightnessFor(adjustmentPercent: Int): Float {
        if (adjustmentPercent == 0) return -1f
        return (adjustmentPercent.coerceIn(1, 100) / 100f)
            .coerceIn(0.01f, 1f)
    }
}

object PlayerSettingsLayoutPolicy {
    private const val LANDSCAPE_MARGIN_DP = 3
    private const val LANDSCAPE_GRID_COLUMNS = 4
    private const val LANDSCAPE_GRID_CELL_WIDTH_DP = 104
    private const val LANDSCAPE_GRID_COLUMN_GAP_DP = 0.1
    private const val LANDSCAPE_HORIZONTAL_PADDING_DP = 0.3

    fun dialogBounds(screenWidth: Int, screenHeight: Int): DialogBounds =
        panelBounds(screenWidth, screenHeight)

    fun panelBounds(screenWidth: Int, screenHeight: Int, density: Float = 1f): DialogBounds =
        if (screenWidth > screenHeight) {
            val marginPx = landscapeMarginPx(screenWidth, screenHeight, density)
            val gridWidth = landscapeContentWidth(
                columnCount = LANDSCAPE_GRID_COLUMNS,
                cellWidthDp = LANDSCAPE_GRID_CELL_WIDTH_DP,
                columnGapDp = LANDSCAPE_GRID_COLUMN_GAP_DP,
                horizontalPaddingDp = LANDSCAPE_HORIZONTAL_PADDING_DP,
                density = density
            )
            val availableWidth = screenWidth - marginPx * 2
            DialogBounds(
                width = gridWidth.coerceAtMost(availableWidth),
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

    fun landscapeMarginPx(screenWidth: Int, screenHeight: Int, density: Float = 1f): Int =
        if (screenWidth > screenHeight) dpToPx(LANDSCAPE_MARGIN_DP, density) else 0

    fun landscapeContentWidth(
        columnCount: Int,
        cellWidthDp: Number,
        columnGapDp: Number,
        horizontalPaddingDp: Number,
        density: Float
    ): Int {
        val safeColumnCount = columnCount.coerceAtLeast(1)
        val cellWidthPx = dpToPx(cellWidthDp, density)
        val columnGapPx = dpToPx(columnGapDp, density)
        val horizontalPaddingPx = dpToPx(horizontalPaddingDp, density)
        val cellsWidth = safeColumnCount * cellWidthPx
        val gapsWidth = (safeColumnCount - 1) * columnGapPx
        return cellsWidth + gapsWidth + horizontalPaddingPx * 2
    }

    private fun dpToPx(value: Number, density: Float): Int =
        (value.toFloat() * density.coerceAtLeast(1f)).toInt()

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
