package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VttParserTest {
    @Test
    fun parsesHeaderCueIdentifiersMultilineTextAndFinalCueWithoutBlankLine() {
        val content = "WEBVTT\n\nfirst\n01:02:03.004 --> 01:02:05.006\n Hello \n World \n\nsecond\n00:00:06.500 --> 00:00:07.750\nNext"
        val expected = listOf(
            SubtitleItem(0, 3_723_004, 3_725_006, "Hello\nWorld"),
            SubtitleItem(1, 6_500, 7_750, "Next")
        )
        assertEquals(expected, VttParser.parse(content))
        assertEquals(expected, VttParser.parse(content.replace("\n", "\r\n")))
    }

    @Test
    fun acceptsMinuteSecondTimestampsAndCueSettings() {
        assertEquals(
            listOf(SubtitleItem(0, 62_003, 64_500, "Text")),
            VttParser.parse("WEBVTT\n\n01:02.003 --> 01:04.500 align:start position:10%\nText")
        )
    }

    @Test
    fun returnsEmptyWhenNoCueOrNoTextExists() {
        listOf("", "WEBVTT", "WEBVTT\n\nNOTE comment", "00:00:01.000 --> 00:00:02.000")
            .forEach { assertTrue(it, VttParser.parse(it).isEmpty()) }
    }

    @Test
    fun skipsEmptyCueWithoutConsumingFollowingCueIdentifier() {
        assertEquals(
            listOf(SubtitleItem(0, 3_000, 4_000, "Text")),
            VttParser.parse("00:00:01.000 --> 00:00:02.000\n\nnext\n00:00:03.000 --> 00:00:04.000\nText")
        )
    }

    @Test(timeout = 1_000)
    fun skipsLineWithMultipleArrowsAndContinuesParsing() {
        assertEquals(
            listOf(SubtitleItem(0, 3_000, 4_000, "Valid")),
            VttParser.parse("00:01.000 --> 00:02.000 --> broken\nBad\n\n00:00:03.000 --> 00:00:04.000\nValid")
        )
    }

    @Test
    fun malformedTimestampDoesNotCrashOrDiscardText() {
        val cases = mapOf(
            "bad" to 0L,
            "00:01:02:03.004" to 0L,
            "bad:02:03.004" to 123_004L,
            "01:bad:03.004" to 3_603_004L,
            "01:02:bad.004" to 3_720_004L,
            "01:02:03.bad" to 3_723_000L
        )
        cases.forEach { (time, expected) ->
            assertEquals(time, expected, VttParser.parse("$time --> 00:00:10.000\nText").single().startTimeMs)
        }
    }
}
