package com.example.openvideo.ui.player

import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.subtitle.SubtitleCueStyle

data class PlayerSubtitleCueStylePresentation(
    val textSizeSp: Float,
    val textColor: Int,
    val shadowRadius: Float,
    val shadowDx: Float,
    val shadowDy: Float,
    val shadowColor: Int,
    val gravity: Int
)

object PlayerSubtitleCueStylePolicy {
    fun resolve(
        style: SubtitleCueStyle?,
        defaultTextSizeSp: Int,
        defaultTextColor: Int
    ): PlayerSubtitleCueStylePresentation {
        val shadowDepth = style?.shadowDepth ?: 0f
        return PlayerSubtitleCueStylePresentation(
            textSizeSp = style?.fontSizeSp?.coerceIn(MIN_ASS_FONT_SIZE_SP, MAX_ASS_FONT_SIZE_SP)
                ?: defaultTextSizeSp.toFloat(),
            textColor = style?.primaryColor ?: defaultTextColor,
            shadowRadius = style?.outlineWidth ?: 0f,
            shadowDx = shadowDepth,
            shadowDy = shadowDepth,
            shadowColor = style?.outlineColor ?: 0x00000000,
            gravity = gravityForAssAlignment(style?.alignment)
        )
    }

    fun apply(
        textView: TextView,
        style: SubtitleCueStyle?,
        playerPrefs: PlayerPrefs,
        defaultTextSizeSp: Int = playerPrefs.subtitleSize,
        defaultTextColor: Int = playerPrefs.subtitleColor
    ) {
        val presentation = resolve(
            style = style,
            defaultTextSizeSp = defaultTextSizeSp,
            defaultTextColor = defaultTextColor
        )
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, presentation.textSizeSp)
        textView.setTextColor(presentation.textColor)
        textView.setShadowLayer(
            presentation.shadowRadius,
            presentation.shadowDx,
            presentation.shadowDy,
            presentation.shadowColor
        )
        textView.gravity = presentation.gravity
    }

    private fun gravityForAssAlignment(alignment: Int?): Int =
        when (alignment) {
            1, 4, 7 -> Gravity.LEFT or Gravity.CENTER_VERTICAL
            3, 6, 9 -> Gravity.RIGHT or Gravity.CENTER_VERTICAL
            else -> Gravity.CENTER
        }

    private const val MIN_ASS_FONT_SIZE_SP = 10f
    private const val MAX_ASS_FONT_SIZE_SP = 48f
}
