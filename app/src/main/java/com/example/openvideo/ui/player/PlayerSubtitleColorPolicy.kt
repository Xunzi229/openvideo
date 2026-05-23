package com.example.openvideo.ui.player

import com.example.openvideo.R

object PlayerSubtitleColorPolicy {

    data class Option(val labelRes: Int, val color: Int)

    val options: List<Option> = listOf(
        Option(R.string.player_settings_subtitle_color_white, 0xFFFFFFFF.toInt()),
        Option(R.string.player_settings_subtitle_color_yellow, 0xFFFFEB3B.toInt()),
        Option(R.string.player_settings_subtitle_color_green, 0xFF8BC34A.toInt()),
        Option(R.string.player_settings_subtitle_color_blue, 0xFF64B5F6.toInt())
    )

    /** Matches [R.color.player_accent] — kept here so swatch stroke logic stays JVM-testable. */
    const val SWATCH_STROKE_SELECTED = 0xFF4F8CFF.toInt()
    private const val SWATCH_STROKE_MUTED = 0x66000000.toInt()
    private const val SWATCH_STROKE_ON_WHITE = 0x99000000.toInt()

    fun optionFor(color: Int): Option =
        options.firstOrNull { it.color == color } ?: options.first()

    fun indexOf(color: Int): Int =
        options.indexOfFirst { it.color == color }.takeIf { it >= 0 } ?: 0

    fun nextIndex(currentIndex: Int): Int =
        (currentIndex + 1) % options.size

    fun swatchStrokeWidthPx(selected: Boolean, density: Float): Int {
        val dp = if (selected) 2f else 1f
        return (dp * density).toInt().coerceAtLeast(1)
    }

    fun swatchStrokeColor(color: Int, selected: Boolean): Int =
        when {
            selected -> SWATCH_STROKE_SELECTED
            color == options.first().color -> SWATCH_STROKE_ON_WHITE
            else -> SWATCH_STROKE_MUTED
        }
}
