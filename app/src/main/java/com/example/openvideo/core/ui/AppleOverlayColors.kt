package com.example.openvideo.core.ui

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.example.openvideo.R

data class AppleOverlayColors(
    val card: Int,
    val alert: Int,
    val title: Int,
    val message: Int,
    val hairline: Int,
    val accent: Int,
    val danger: Int,
    val input: Int,
    val dimAmount: Float
) {
    companion object {
        fun from(context: Context): AppleOverlayColors {
            val currentNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val playerSurface = isPlayerSurface(context)
            val nightTokens = currentNight || playerSurface
            val tokens = if (nightTokens && !currentNight) {
                context.createConfigurationContext(
                    Configuration(context.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            Configuration.UI_MODE_NIGHT_YES
                    }
                )
            } else {
                context
            }
            return AppleOverlayColors(
                card = ContextCompat.getColor(tokens, R.color.ov_overlay_card),
                alert = ContextCompat.getColor(tokens, R.color.ov_overlay_alert),
                title = ContextCompat.getColor(tokens, R.color.ov_overlay_title),
                message = ContextCompat.getColor(tokens, R.color.ov_overlay_message),
                hairline = ContextCompat.getColor(tokens, R.color.ov_overlay_hairline),
                accent = ContextCompat.getColor(tokens, R.color.ov_accent_blue),
                danger = ContextCompat.getColor(tokens, R.color.ov_danger),
                input = ContextCompat.getColor(tokens, R.color.ov_overlay_input),
                dimAmount = if (nightTokens) 0.58f else 0.52f
            )
        }

        private fun isPlayerSurface(context: Context): Boolean {
            val typed = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            return try {
                typed.getColor(0, 0) == ContextCompat.getColor(context, R.color.player_bg)
            } finally {
                typed.recycle()
            }
        }
    }

    fun colorFor(style: AppleActionStyle): Int = when (style) {
        AppleActionStyle.DESTRUCTIVE -> danger
        AppleActionStyle.DEFAULT, AppleActionStyle.CANCEL -> accent
    }
}
