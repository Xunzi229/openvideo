package com.example.openvideo.ui.player

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs

object PlayerSubtitleColorSwatchBinder {

    private val swatchViewIds = intArrayOf(
        R.id.swatch_subtitle_color_0,
        R.id.swatch_subtitle_color_1,
        R.id.swatch_subtitle_color_2,
        R.id.swatch_subtitle_color_3
    )

    fun createSwatchView(context: Context, sizePx: Int): View =
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
            isClickable = true
            isFocusable = true
        }

    fun bind(
        root: View,
        playerPrefs: PlayerPrefs,
        density: Float,
        onColorChanged: () -> Unit
    ) {
        bindSwatches(
            swatches = swatchViewIds.map { root.findViewById(it) },
            context = root.context,
            playerPrefs = playerPrefs,
            density = density,
            onColorChanged = onColorChanged
        )
    }

    fun bindSwatches(
        swatches: List<View>,
        context: Context,
        playerPrefs: PlayerPrefs,
        density: Float,
        onColorChanged: () -> Unit
    ) {
        fun renderSelection() {
            val selectedColor = playerPrefs.subtitleColor
            PlayerSubtitleColorPolicy.options.forEachIndexed { index, option ->
                swatches[index].background = swatchDrawable(
                    color = option.color,
                    selected = option.color == selectedColor,
                    density = density
                )
            }
        }
        PlayerSubtitleColorPolicy.options.forEachIndexed { index, option ->
            val swatch = swatches[index]
            swatch.contentDescription = context.getString(option.labelRes)
            swatch.setOnClickListener {
                playerPrefs.subtitleColor = option.color
                renderSelection()
                onColorChanged()
            }
        }
        renderSelection()
    }

    fun swatchDrawable(color: Int, selected: Boolean, density: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(
                PlayerSubtitleColorPolicy.swatchStrokeWidthPx(selected, density),
                PlayerSubtitleColorPolicy.swatchStrokeColor(color, selected)
            )
        }
}
