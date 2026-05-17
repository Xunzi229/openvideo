package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.ContentFrameMode

data class ContentFrameModeSelection(
    val mode: ContentFrameMode,
    val aspectRatioOverride: AspectRatio? = null
)

data class AspectRatioSelection(
    val aspectRatio: AspectRatio,
    val contentFrameOverride: ContentFrameMode? = null
)

/**
 * Display-settings side effects when the user picks a center-window preset.
 */
object PlayerContentFrameSettingsPolicy {

    fun onAspectRatioSelected(
        aspectRatio: AspectRatio,
        currentContentFrameMode: ContentFrameMode
    ): AspectRatioSelection {
        if (currentContentFrameMode == ContentFrameMode.OFF) {
            return AspectRatioSelection(aspectRatio)
        }
        return AspectRatioSelection(
            aspectRatio = aspectRatio,
            contentFrameOverride = ContentFrameMode.OFF
        )
    }

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
