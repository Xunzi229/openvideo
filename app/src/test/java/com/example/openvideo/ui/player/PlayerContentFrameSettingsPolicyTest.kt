package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerContentFrameSettingsPolicyTest {

    @Test
    fun aspectRatioSelectionClearsActiveCenterBand() {
        val selection = PlayerContentFrameSettingsPolicy.onAspectRatioSelected(
            aspectRatio = AspectRatio.FIT,
            currentContentFrameMode = ContentFrameMode.CENTER_16_9
        )
        assertEquals(AspectRatio.FIT, selection.aspectRatio)
        assertEquals(ContentFrameMode.OFF, selection.contentFrameOverride)
    }

    @Test
    fun aspectRatioSelectionKeepsContentFrameOff() {
        val selection = PlayerContentFrameSettingsPolicy.onAspectRatioSelected(
            aspectRatio = AspectRatio.FIT,
            currentContentFrameMode = ContentFrameMode.OFF
        )
        assertEquals(AspectRatio.FIT, selection.aspectRatio)
        assertNull(selection.contentFrameOverride)
    }

    @Test
    fun offModeDoesNotOverrideAspectRatio() {
        val selection = PlayerContentFrameSettingsPolicy.onModeSelected(
            mode = ContentFrameMode.OFF,
            currentAspectRatio = AspectRatio.FILL
        )
        assertEquals(ContentFrameMode.OFF, selection.mode)
        assertNull(selection.aspectRatioOverride)
    }

    @Test
    fun centerBandOnFillSwitchesAspectToFit() {
        val selection = PlayerContentFrameSettingsPolicy.onModeSelected(
            mode = ContentFrameMode.CENTER_16_9,
            currentAspectRatio = AspectRatio.FILL
        )
        assertEquals(ContentFrameMode.CENTER_16_9, selection.mode)
        assertEquals(AspectRatio.FIT, selection.aspectRatioOverride)
    }

    @Test
    fun centerBandOnFitKeepsAspectRatio() {
        val selection = PlayerContentFrameSettingsPolicy.onModeSelected(
            mode = ContentFrameMode.CENTER_16_9,
            currentAspectRatio = AspectRatio.FIT
        )
        assertEquals(ContentFrameMode.CENTER_16_9, selection.mode)
        assertNull(selection.aspectRatioOverride)
    }
}
