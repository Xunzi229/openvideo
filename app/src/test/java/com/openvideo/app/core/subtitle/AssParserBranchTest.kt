package com.openvideo.app.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssParserBranchTest {
    @Test
    fun ignoresDialogueOutsideEventsAndResetsSectionState() {
        assertTrue(AssParser.parse("Dialogue: ignored\n[Script Info]\nTitle: Example").isEmpty())
        assertEquals(listOf(SubtitleItem(0, 1_000, 2_000, "Kept")), AssParser.parse("""
            [Events]
            Format: Start, End, Text
            Dialogue: 0:00:01.00,0:00:02.00,Kept
            [Script Info]
            Dialogue: 0:00:01.00,0:00:02.00,Ignored
        """.trimIndent()))
    }

    @Test
    fun handlesReorderedFormatsCommasAndEmptyStyledText() {
        val cues = AssParser.parse("""
            [Events]
            Format: End, Start, Text
            Dialogue: 0:00:02.50,0:00:01.25,{\i1}
            Dialogue: 0:00:02.50,0:00:01.25,Hello, world\nNext
        """.trimIndent())
        assertEquals(listOf(SubtitleItem(0, 1_250, 2_500, "Hello, world\nNext")), cues)
    }

    @Test
    fun convertsBgrColorsAndInvertedAlphaAcrossSupportedPrefixes() {
        val cases = mapOf(
            "&H112233" to 0xFF332211.toInt(),
            "&h00112233&" to 0xFF332211.toInt(),
            "H80112233" to 0x7F332211,
            "hFF112233" to 0x00332211
        )
        cases.forEach { (color, expected) -> assertEquals(color, expected, styledCue(color).style?.primaryColor) }
    }

    @Test
    fun ignoresInvalidColorLengthsAndEachNonHexComponent() {
        for (color in listOf("", "12345", "GG112233", "00GG2233", "0011GG33", "001122GG")) {
            assertNull(color, styledCue(color).style?.primaryColor)
        }
    }

    @Test
    fun incompleteStylesAndBlankNamesDoNotThrow() {
        val cue = AssParser.parse("""
            [V4+ Styles]
            Format: Name, Fontname, Fontsize
            Style: ,Ignored,24
            Style: Default
            [Events]
            Format: Start, End, Style, Text
            Dialogue: 0:00:01.00,0:00:02.00,Default,Text
        """.trimIndent()).single()
        assertEquals(SubtitleCueStyle(), cue.style)
    }

    @Test
    fun malformedTimeComponentsKeepTextWithZeroFallback() {
        val cases = mapOf(
            "bad" to 0L, "bad:02:03.04" to 123_040L,
            "01:bad:03.04" to 3_603_040L, "01:02:bad.04" to 3_720_040L,
            "01:02:03.bad" to 3_723_000L
        )
        cases.forEach { (time, expected) ->
            assertEquals(time, expected, AssParser.parse("[Events]\nFormat: Start, End, Text\nDialogue: $time,0:00:05.00,Text").single().startTimeMs)
        }
    }

    private fun styledCue(color: String): SubtitleItem = AssParser.parse("""
        [V4+ Styles]
        Format: Name, PrimaryColour
        Style: Default,$color
        [Events]
        Format: Start, End, Style, Text
        Dialogue: 0:00:01.00,0:00:02.00, DEFAULT ,Text
    """.trimIndent()).single()
}
