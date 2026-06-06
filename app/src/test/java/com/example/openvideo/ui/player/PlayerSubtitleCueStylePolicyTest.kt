package com.example.openvideo.ui.player

import android.view.Gravity
import com.example.openvideo.core.subtitle.SubtitleCueStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSubtitleCueStylePolicyTest {

    @Test
    fun resolvesAssStyleOverGlobalSubtitleDefaults() {
        val presentation = PlayerSubtitleCueStylePolicy.resolve(
            style = SubtitleCueStyle(
                fontSizeSp = 36f,
                primaryColor = 0xFFFFFF00.toInt(),
                outlineColor = 0xFF000000.toInt(),
                outlineWidth = 2.5f,
                shadowDepth = 1.5f,
                alignment = 3
            ),
            defaultTextSizeSp = 18,
            defaultTextColor = 0xFFFFFFFF.toInt()
        )

        assertEquals(36f, presentation.textSizeSp)
        assertEquals(0xFFFFFF00.toInt(), presentation.textColor)
        assertEquals(2.5f, presentation.shadowRadius)
        assertEquals(1.5f, presentation.shadowDx)
        assertEquals(1.5f, presentation.shadowDy)
        assertEquals(0xFF000000.toInt(), presentation.shadowColor)
        assertEquals(Gravity.RIGHT or Gravity.CENTER_VERTICAL, presentation.gravity)
    }

    @Test
    fun resolvesGlobalDefaultsWhenCueHasNoAssStyle() {
        val presentation = PlayerSubtitleCueStylePolicy.resolve(
            style = null,
            defaultTextSizeSp = 18,
            defaultTextColor = 0xFFFFFFFF.toInt()
        )

        assertEquals(18f, presentation.textSizeSp)
        assertEquals(0xFFFFFFFF.toInt(), presentation.textColor)
        assertEquals(0f, presentation.shadowRadius)
        assertEquals(0f, presentation.shadowDx)
        assertEquals(0f, presentation.shadowDy)
        assertEquals(0x00000000, presentation.shadowColor)
        assertEquals(Gravity.CENTER, presentation.gravity)
    }
}
