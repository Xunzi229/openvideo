package com.example.openvideo.ui.player

import android.os.Build

data class PlayerSettingsSheetStyle(
    val panelAlpha: Float,
    val dimAmount: Float,
    val backdropBlurRadiusPx: Int
)

object PlayerSettingsSheetStylePolicy {
    private const val MAX_BLUR_DP = 64

    fun supportsBackdropBlur(sdkInt: Int): Boolean =
        sdkInt >= Build.VERSION_CODES.S

    fun compute(
        panelOpacityPercent: Int,
        backdropDimPercent: Int,
        backdropBlurDp: Int,
        density: Float
    ): PlayerSettingsSheetStyle {
        val blurDp = backdropBlurDp.coerceIn(0, MAX_BLUR_DP)
        return PlayerSettingsSheetStyle(
            panelAlpha = PlayerChromePolicy.percentToAlpha(panelOpacityPercent),
            dimAmount = PlayerChromePolicy.percentToAlpha(backdropDimPercent),
            backdropBlurRadiusPx = if (blurDp > 0) {
                (blurDp * density).toInt()
            } else {
                0
            }
        )
    }
}
