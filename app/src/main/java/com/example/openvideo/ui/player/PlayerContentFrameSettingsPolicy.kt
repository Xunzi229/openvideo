package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode

data class ContentFrameModeSelection(
    val mode: ContentFrameMode,
    val aspectRatioOverride: AspectRatio? = null
)

/**
 * Display-settings side effects when the user picks a center-window preset.
 */
object PlayerContentFrameSettingsPolicy {

    fun onModeSelected(
        mode: ContentFrameMode,
        currentAspectRatio: AspectRatio
    ): ContentFrameModeSelection {
        if (mode == ContentFrameMode.OFF) {
            return ContentFrameModeSelection(mode)
        }
        if (!PlayerContentFramePolicy.allowsContentFrameAdjustment(currentAspectRatio)) {
            return ContentFrameModeSelection(mode, AspectRatio.FIT)
        }
        return ContentFrameModeSelection(mode)
    }
}
