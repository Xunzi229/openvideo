package com.example.openvideo.ui.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration

/**
 * 全屏按钮点击时，根据当前 [Configuration.orientation] 决定下一个 `requestedOrientation` 值。
 *
 * - 竖屏 → 申请横屏；
 * - 横屏 / 未定义 → 申请竖屏；
 *
 * 纯函数，便于在 JVM 单测里覆盖。
 */
object PlayerOrientationTogglePolicy {

    fun nextRequestedOrientation(currentOrientation: Int): Int =
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
}
