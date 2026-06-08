package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SubtitleDelayCorrectionPolicyTest {

    @Test
    fun shiftsSubtitleItemsAndPreservesCueContentForCopy() {
        val style = SubtitleCueStyle(fontSizeSp = 30f, primaryColor = 0xFFFFFF00.toInt())
        val plan = SubtitleDelayCorrectionPolicy.planShiftedCopy(
            items = listOf(
                SubtitleItem(
                    index = 1,
                    startTimeMs = 1_000,
                    endTimeMs = 2_000,
                    text = "Hello",
                    style = style
                ),
                SubtitleItem(index = 2, startTimeMs = 3_000, endTimeMs = 4_000, text = "World")
            ),
            deltaMs = 750,
            sourceName = "demo.srt"
        )

        assertEquals(750, plan.deltaMs)
        assertEquals(2, plan.changedLineCount)
        assertFalse(plan.overwritesOriginal)
        assertEquals("demo.shift+750ms.srt", plan.suggestedCopyName)
        assertEquals(
            listOf(
                SubtitleItem(index = 1, startTimeMs = 1_750, endTimeMs = 2_750, text = "Hello", style = style),
                SubtitleItem(index = 2, startTimeMs = 3_750, endTimeMs = 4_750, text = "World")
            ),
            plan.items
        )
    }

    @Test
    fun clampsNegativeShiftedTimesToZeroWithoutTouchingOriginalList() {
        val original = listOf(
            SubtitleItem(index = 1, startTimeMs = 300, endTimeMs = 900, text = "Intro")
        )

        val plan = SubtitleDelayCorrectionPolicy.planShiftedCopy(
            items = original,
            deltaMs = -500,
            sourceName = "intro.ass"
        )

        assertEquals("intro.shift-500ms.ass", plan.suggestedCopyName)
        assertEquals(
            listOf(SubtitleItem(index = 1, startTimeMs = 0, endTimeMs = 400, text = "Intro")),
            plan.items
        )
        assertEquals(
            listOf(SubtitleItem(index = 1, startTimeMs = 300, endTimeMs = 900, text = "Intro")),
            original
        )
    }

    @Test
    fun suggestedCopyNameFallsBackWhenSourceNameIsBlankOrExtensionless() {
        assertEquals(
            "subtitle.shift+100ms.srt",
            SubtitleDelayCorrectionPolicy.planShiftedCopy(
                items = emptyList(),
                deltaMs = 100,
                sourceName = ""
            ).suggestedCopyName
        )
        assertEquals(
            "subtitle.shift-100ms.srt",
            SubtitleDelayCorrectionPolicy.planShiftedCopy(
                items = emptyList(),
                deltaMs = -100,
                sourceName = "subtitle"
            ).suggestedCopyName
        )
    }
}
