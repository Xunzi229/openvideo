package com.example.openvideo.ui.player

data class PlayerScreenLockChromeReveal(
    val controlsVisible: Boolean,
    val containerVisible: Boolean,
    val alpha: Float
)

object PlayerScreenLockChromePolicy {
    fun revealChrome(maxAlpha: Float): PlayerScreenLockChromeReveal =
        PlayerScreenLockChromeReveal(
            controlsVisible = true,
            containerVisible = true,
            alpha = maxAlpha
        )
}
