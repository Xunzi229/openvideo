package com.example.openvideo.ui.player

data class PlayerChromePresentation(
    val controlsVisible: Boolean,
    val containerVisible: Boolean,
    val alpha: Float,
    val hideDelayMs: Long?
)

object PlayerChromeVisibilityPolicy {

    fun show(controlsOpacityPercent: Int, autoHideSeconds: Int): PlayerChromePresentation {
        val delay = PlayerChromePolicy.autoHideDelayMs(autoHideSeconds)
        return PlayerChromePresentation(
            controlsVisible = true,
            containerVisible = true,
            alpha = PlayerChromePolicy.maxChromeAlpha(controlsOpacityPercent),
            hideDelayMs = delay.takeIf(PlayerChromePolicy::shouldScheduleAutoHide)
        )
    }

    fun hide(): PlayerChromePresentation =
        PlayerChromePresentation(
            controlsVisible = false,
            containerVisible = false,
            alpha = 0f,
            hideDelayMs = null
        )

    fun showLocked(controlsOpacityPercent: Int): PlayerChromePresentation =
        PlayerChromePresentation(
            controlsVisible = true,
            containerVisible = true,
            alpha = PlayerChromePolicy.maxChromeAlpha(controlsOpacityPercent),
            hideDelayMs = PlayerChromePolicy.lockedControlsHideDelayMs()
        )

    fun pictureInPicture(): PlayerChromePresentation = hide()
}
