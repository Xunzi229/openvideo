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
            val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return AppleOverlayColors(
                card = ContextCompat.getColor(context, R.color.ov_overlay_card),
                alert = ContextCompat.getColor(context, R.color.ov_overlay_alert),
                title = ContextCompat.getColor(context, R.color.ov_overlay_title),
                message = ContextCompat.getColor(context, R.color.ov_overlay_message),
                hairline = ContextCompat.getColor(context, R.color.ov_overlay_hairline),
                accent = ContextCompat.getColor(context, R.color.ov_accent_blue),
                danger = ContextCompat.getColor(context, R.color.ov_danger),
                input = ContextCompat.getColor(context, R.color.ov_overlay_input),
                dimAmount = if (night == Configuration.UI_MODE_NIGHT_YES) 0.58f else 0.52f
            )
        }
    }

    fun colorFor(style: AppleActionStyle): Int = when (style) {
        AppleActionStyle.DESTRUCTIVE -> danger
        AppleActionStyle.DEFAULT, AppleActionStyle.CANCEL -> accent
    }
}
