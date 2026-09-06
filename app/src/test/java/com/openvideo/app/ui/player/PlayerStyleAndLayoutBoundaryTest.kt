package com.openvideo.app.ui.player

import android.app.Application
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import com.openvideo.app.core.prefs.PlayerPrefs
import com.openvideo.app.core.prefs.SubtitleBgStyle
import com.openvideo.app.core.subtitle.SubtitleCueStyle
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class PlayerStyleAndLayoutBoundaryTest {
    @Test fun subtitlePaletteCyclesAndStrokesReflectSelection() {
        val options = PlayerSubtitleColorPolicy.options
        options.forEachIndexed { index, option ->
            assertEquals(option, PlayerSubtitleColorPolicy.optionFor(option.color))
            assertEquals(index, PlayerSubtitleColorPolicy.indexOf(option.color))
            assertTrue(option.labelRes != 0)
            assertEquals((index + 1) % options.size, PlayerSubtitleColorPolicy.nextIndex(index))
            assertEquals(PlayerSubtitleColorPolicy.SWATCH_STROKE_SELECTED, PlayerSubtitleColorPolicy.swatchStrokeColor(option.color, true))
            assertEquals(if (index == 0) 0x99000000.toInt() else 0x66000000, PlayerSubtitleColorPolicy.swatchStrokeColor(option.color, false))
        }
        assertEquals(options.first(), PlayerSubtitleColorPolicy.optionFor(123))
        assertEquals(0, PlayerSubtitleColorPolicy.indexOf(123))
        assertEquals(4, PlayerSubtitleColorPolicy.swatchStrokeWidthPx(true, 2f))
        assertEquals(2, PlayerSubtitleColorPolicy.swatchStrokeWidthPx(false, 2f))
        assertEquals(1, PlayerSubtitleColorPolicy.swatchStrokeWidthPx(false, 0f))
    }

    @Test fun cueStylesApplyDefaultsClampedSizesAndEveryAlignment() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = PlayerPrefs(context).apply { subtitleSize = 22; subtitleColor = 0xFF123456.toInt() }
        val view = TextView(context)
        PlayerSubtitleCueStylePolicy.apply(view, null, prefs)
        assertEquals(prefs.subtitleColor, view.currentTextColor)
        assertEquals(Gravity.CENTER, view.gravity)
        for (alignment in 1..9) {
            val style = SubtitleCueStyle(fontSizeSp = 100f, primaryColor = -1, outlineWidth = 2f,
                shadowDepth = 3f, outlineColor = 0xFF000000.toInt(), alignment = alignment)
            PlayerSubtitleCueStylePolicy.apply(view, style, prefs, 20, -1)
            val expected = when (alignment) { 1, 4, 7 -> Gravity.LEFT or Gravity.CENTER_VERTICAL; 3, 6, 9 -> Gravity.RIGHT or Gravity.CENTER_VERTICAL; else -> Gravity.CENTER }
            assertEquals(expected, view.gravity)
            assertEquals(48f * context.resources.displayMetrics.scaledDensity, view.textSize, 0.01f)
        }
        assertEquals(10f, PlayerSubtitleCueStylePolicy.resolve(SubtitleCueStyle(fontSizeSp = 1f), 22, -1).textSizeSp, 0f)
    }

    @Test fun backgroundStylesAndPreviewPositionApplyToRealTextViews() {
        val context = RuntimeEnvironment.getApplication()
        val colors = listOf(0, 0xAA000000.toInt(), 0xFF000000.toInt())
        for ((index, style) in SubtitleBgStyle.entries.withIndex()) {
            val view = TextView(context)
            view.layout(0, 0, 200, 100)
            PlayerSubtitleSettingsPreviewPolicy.apply(view, "sample", 22, -1, style, 0.5f)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals("sample", view.text.toString())
            assertEquals(colors[index], (view.background as ColorDrawable).color)
            assertEquals(-1, view.currentTextColor)
        }
        val empty = TextView(context)
        PlayerSubtitleSettingsPreviewPolicy.apply(empty, "sample", 20, -1, SubtitleBgStyle.NONE, 1f)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0f, empty.translationY, 0f)
    }

    @Test fun invalidPixelRatiosAndDimensionsAreNormalizedForLayout() {
        assertEquals(DisplayFrameSize(0, 0), PlayerVideoLayoutPolicy.displayFrameSize(100, 0))
        assertEquals(0f, PlayerVideoLayoutPolicy.displayAspectRatio(100, 0), 0f)
        for (ratio in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(DisplayFrameSize(100, 50), PlayerVideoLayoutPolicy.displayFrameSize(100, 50, ratio))
            assertEquals(2f, PlayerVideoLayoutPolicy.displayAspectRatio(100, 50, ratio), 0f)
        }
        assertEquals(DisplayFrameSize(50, 200), PlayerVideoLayoutPolicy.displayFrameSize(100, 50, 2f, -90))
        assertEquals(0.25f, PlayerVideoLayoutPolicy.displayAspectRatio(100, 50, 2f, -90), 0f)
        assertEquals(DisplayFrameSize(1, 50), PlayerVideoLayoutPolicy.displayFrameSize(1, 50, 0.1f))
    }
}
