package com.example.openvideo.ui.player

import androidx.annotation.StringRes
import com.example.openvideo.R
import com.example.openvideo.core.prefs.AspectRatio

data class PlayerAspectRatioOption(
    val ratio: AspectRatio,
    @param:StringRes val labelRes: Int
)

object PlayerAspectRatioOptions {
    val entries: List<PlayerAspectRatioOption> = listOf(
        PlayerAspectRatioOption(AspectRatio.FIT, R.string.player_sheet_original_ratio),
        PlayerAspectRatioOption(AspectRatio.FILL, R.string.player_sheet_fill_screen),
        PlayerAspectRatioOption(AspectRatio.RATIO_16_9, R.string.settings_ratio_16_9),
        PlayerAspectRatioOption(AspectRatio.RATIO_4_3, R.string.settings_ratio_4_3),
        PlayerAspectRatioOption(AspectRatio.CROP, R.string.settings_ratio_crop),
        PlayerAspectRatioOption(AspectRatio.STRETCH, R.string.settings_ratio_stretch)
    )
}
