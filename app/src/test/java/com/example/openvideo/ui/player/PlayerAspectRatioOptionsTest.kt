package com.example.openvideo.ui.player

import com.example.openvideo.R
import com.example.openvideo.core.prefs.AspectRatio
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerAspectRatioOptionsTest {

    @Test
    fun playerAspectRatioOptionsMatchSettingsOrder() {
        assertEquals(
            listOf(
                AspectRatio.FIT,
                AspectRatio.FILL,
                AspectRatio.RATIO_16_9,
                AspectRatio.RATIO_4_3,
                AspectRatio.CROP,
                AspectRatio.STRETCH
            ),
            PlayerAspectRatioOptions.entries.map { it.ratio }
        )
    }

    @Test
    fun playerAspectRatioOptionsUsePlayerSettingsLabels() {
        assertEquals(
            listOf(
                R.string.player_sheet_original_ratio,
                R.string.player_sheet_fill_screen,
                R.string.settings_ratio_16_9,
                R.string.settings_ratio_4_3,
                R.string.settings_ratio_crop,
                R.string.settings_ratio_stretch
            ),
            PlayerAspectRatioOptions.entries.map { it.labelRes }
        )
    }
}
