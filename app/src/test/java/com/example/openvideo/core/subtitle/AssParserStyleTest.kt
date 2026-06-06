package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssParserStyleTest {

    @Test
    fun parseAppliesNamedAssStyleToDialogueCue() {
        val content = """
            [Script Info]
            Title: style sample

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, OutlineColour, Outline, Shadow, Alignment, MarginL, MarginR, MarginV
            Style: Default,Arial,42,&H0000FFFF,&H00000000,3,1,2,20,30,40

            [Events]
            Format: Layer, Start, End, Style, Text
            Dialogue: 0,0:00:01.00,0:00:02.50,Default,{\bord3}Hello\NWorld
        """.trimIndent()

        val item = AssParser.parse(content).single()

        assertEquals("Hello\nWorld", item.text)
        assertEquals(1_000L, item.startTimeMs)
        assertEquals(2_500L, item.endTimeMs)
        assertEquals(
            SubtitleCueStyle(
                fontName = "Arial",
                fontSizeSp = 42f,
                primaryColor = 0xFFFFFF00.toInt(),
                outlineColor = 0xFF000000.toInt(),
                outlineWidth = 3f,
                shadowDepth = 1f,
                alignment = 2,
                marginL = 20,
                marginR = 30,
                marginV = 40
            ),
            item.style
        )
    }

    @Test
    fun parseKeepsPlainAssDialogueStyleEmptyWhenNoStyleSectionMatches() {
        val content = """
            [Events]
            Dialogue: 0,0:00:01.00,0:00:02.00,Missing,,0,0,0,,Plain text
        """.trimIndent()

        val item = AssParser.parse(content).single()

        assertEquals("Plain text", item.text)
        assertNull(item.style)
    }
}
