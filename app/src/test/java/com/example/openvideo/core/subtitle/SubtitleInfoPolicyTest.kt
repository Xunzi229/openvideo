package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleInfoPolicyTest {

    @Test
    fun summarizesLoadedSubtitleItemsForDiagnostics() {
        val info = SubtitleInfoPolicy.summarize(
            items = listOf(
                SubtitleItem(
                    index = 1,
                    startTimeMs = 1_000,
                    endTimeMs = 2_500,
                    text = "Hello",
                    style = SubtitleCueStyle(fontSizeSp = 32f)
                ),
                SubtitleItem(
                    index = 2,
                    startTimeMs = 3_000,
                    endTimeMs = 4_500,
                    text = "World"
                )
            ),
            sourceLabel = "demo.ass",
            encoding = "UTF-8"
        )

        assertEquals("demo.ass", info.sourceLabel)
        assertEquals("UTF-8", info.encodingLabel)
        assertEquals(2, info.lineCount)
        assertEquals(1_000L, info.firstStartTimeMs)
        assertEquals(4_500L, info.lastEndTimeMs)
        assertEquals(3_500L, info.durationMs)
        assertEquals(1, info.styledLineCount)
        assertTrue(info.hasStyledCues)
        assertFalse(info.isEmpty)
        assertEquals(SubtitleInfoStatus.Loaded, info.status)
    }

    @Test
    fun summarizesEmptySubtitleAsDiagnosticEmptyState() {
        val info = SubtitleInfoPolicy.summarize(
            items = emptyList(),
            sourceLabel = "",
            encoding = "auto"
        )

        assertEquals("Unknown subtitle", info.sourceLabel)
        assertEquals("Auto detect", info.encodingLabel)
        assertEquals(0, info.lineCount)
        assertEquals(0L, info.firstStartTimeMs)
        assertEquals(0L, info.lastEndTimeMs)
        assertEquals(0L, info.durationMs)
        assertEquals(0, info.styledLineCount)
        assertFalse(info.hasStyledCues)
        assertTrue(info.isEmpty)
        assertEquals(SubtitleInfoStatus.Empty, info.status)
    }
}
