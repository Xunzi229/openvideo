package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtParserTest {
    @Test
    fun parsesMultipleCuesAndPreservesMultilineTextWithEitherLineEnding() {
        val content = "7\n01:02:03,004 --> 01:02:05,006\nHello\nWorld\n\n9\n00:00:06,500 --> 00:00:07,750\nNext"
        val expected = listOf(
            SubtitleItem(7, 3_723_004, 3_725_006, "Hello\nWorld"),
            SubtitleItem(9, 6_500, 7_750, "Next")
        )
        assertEquals(expected, SrtParser.parse(content))
        assertEquals(expected, SrtParser.parse(content.replace("\n", "\r\n")))
    }

    @Test
    fun skipsIncompleteInvalidIndexAndInvalidSeparatorBlocks() {
        val invalid = listOf(
            "", " \n ", "1\n00:00:01,000 --> 00:00:02,000",
            "invalid\n00:00:01,000 --> 00:00:02,000\nText",
            "1\n00:00:01,000 - 00:00:02,000\nText",
            "1\n00:00:01,000 --> 00:00:02,000 --> 00:00:03,000\nText"
        )
        invalid.forEach { assertTrue(it, SrtParser.parse(it).isEmpty()) }
        assertEquals(
            listOf(SubtitleItem(2, 1_000, 2_000, "Valid")),
            SrtParser.parse(invalid.last() + "\n\n2\n00:00:01,000 --> 00:00:02,000\nValid")
        )
    }

    @Test
    fun toleratesMalformedTimeComponentsWithoutDroppingTheText() {
        val cases = mapOf(
            "bad" to 0L,
            "bad:02:03,004" to 123_004L,
            "01:bad:03,004" to 3_603_004L,
            "01:02:bad,004" to 3_720_004L,
            "01:02:03,bad" to 3_723_000L
        )
        cases.forEach { (time, expected) ->
            val item = SrtParser.parse("1\n$time --> 00:00:10,000\nText").single()
            assertEquals(time, expected, item.startTimeMs)
            assertEquals("Text", item.text)
        }
    }
}
