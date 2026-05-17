package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSubtitleSettingsPreviewPolicyTest {

    @Test
    fun previewPositionUsesSameTravelAsPlayer() {
        val heightPx = 200
        assertEquals(0f, PlayerDisplayAdjustment.subtitleTranslationY(heightPx, 1f), 0.0001f)
        assertEquals(-120f, PlayerDisplayAdjustment.subtitleTranslationY(heightPx, 0f), 0.0001f)
    }
}
