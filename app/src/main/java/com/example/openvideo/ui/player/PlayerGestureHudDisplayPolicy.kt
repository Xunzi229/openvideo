package com.example.openvideo.ui.player

object PlayerGestureHudDisplayPolicy {
    const val AUTO_HIDE_DELAY_MS = 800L

    fun indicatorText(hud: PlayerGestureHud): String =
        if (hud.secondaryText.isBlank()) {
            hud.primaryText
        } else {
            "${hud.primaryText}\n${hud.secondaryText}"
        }
}
