package com.example.openvideo.ui.player

import com.example.openvideo.ui.settings.DefaultPlayerSettings

/**
 * 倍速文本格式化的纯策略。
 *
 * 输入的速度值先经过 [DefaultPlayerSettings.supportedSpeedOrDefault] 归一化，避免
 * 显示出未支持的奇异值；整数倍速显示成 `Nx`，其他显示成 `N.Nx`（不补零）。
 */
object PlayerSpeedLabel {

    fun format(speed: Float): String {
        val normalized = DefaultPlayerSettings.supportedSpeedOrDefault(speed)
        val numeric = if (normalized % 1f == 0f) {
            normalized.toInt().toString()
        } else {
            normalized.toString()
        }
        return "${numeric}x"
    }
}
