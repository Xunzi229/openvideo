package com.example.openvideo.ui.player

import android.widget.TextView
import com.example.openvideo.core.prefs.SubtitleBgStyle

/**
 * Applies subtitle style/position to the settings-page preview line.
 */
object PlayerSubtitleSettingsPreviewPolicy {

    fun apply(
        preview: TextView,
        sampleText: CharSequence,
        sizeSp: Int,
        textColor: Int,
        bgStyle: SubtitleBgStyle,
        position: Float
    ) {
        preview.text = sampleText
        preview.textSize = sizeSp.toFloat()
        preview.setTextColor(textColor)
        preview.setBackgroundColor(PlayerSubtitleStylePolicy.backgroundColor(bgStyle))
        preview.translationY = 0f
        preview.post {
            val heightPx = preview.height
            if (heightPx > 0) {
                preview.translationY = PlayerDisplayAdjustment.subtitleTranslationY(heightPx, position)
            }
        }
    }
}
