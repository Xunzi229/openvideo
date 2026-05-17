package com.example.openvideo.ui.player

object PlayerChromeSettingsOverlayPolicy {
    fun suppressesControlAutoHide(isSettingsOverlayVisible: Boolean): Boolean =
        isSettingsOverlayVisible

    fun hidesAllChromeRegions(isSettingsOverlayVisible: Boolean): Boolean =
        isSettingsOverlayVisible

    fun restoreContainerAlpha(controlsWereVisible: Boolean, maxAlpha: Float): Float =
        if (controlsWereVisible) maxAlpha else 0f

    fun restoreContainerVisible(controlsWereVisible: Boolean): Boolean = controlsWereVisible
}
