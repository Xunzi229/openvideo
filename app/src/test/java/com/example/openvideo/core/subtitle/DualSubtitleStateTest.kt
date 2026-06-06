package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DualSubtitleStateTest {

    @Test
    fun resolvesPrimaryAndSecondaryTextAtPosition() {
        val state = DualSubtitleState(
            primary = PrimarySubtitle(
                items = listOf(SubtitleItem(index = 1, startTimeMs = 1_000, endTimeMs = 2_000, text = "Hello"))
            ),
            secondary = SecondarySubtitle(
                enabled = true,
                items = listOf(SubtitleItem(index = 1, startTimeMs = 1_000, endTimeMs = 2_000, text = "你好"))
            )
        )

        assertEquals(
            DualSubtitleText(primary = "Hello", secondary = "你好"),
            state.textAt(positionMs = 1_500)
        )
    }

    @Test
    fun hidesSecondaryTextWhenSecondaryTrackIsDisabled() {
        val state = DualSubtitleState(
            primary = PrimarySubtitle(
                items = listOf(SubtitleItem(index = 1, startTimeMs = 1_000, endTimeMs = 2_000, text = "Hello"))
            ),
            secondary = SecondarySubtitle(
                enabled = false,
                items = listOf(SubtitleItem(index = 1, startTimeMs = 1_000, endTimeMs = 2_000, text = "你好"))
            )
        )

        assertEquals(
            DualSubtitleText(primary = "Hello", secondary = ""),
            state.textAt(positionMs = 1_500)
        )
    }

    @Test
    fun returnsNullWhenNoTrackHasTextAtPosition() {
        val state = DualSubtitleState(
            primary = PrimarySubtitle(
                items = listOf(SubtitleItem(index = 1, startTimeMs = 1_000, endTimeMs = 2_000, text = "Hello"))
            ),
            secondary = SecondarySubtitle(
                enabled = true,
                items = listOf(SubtitleItem(index = 1, startTimeMs = 1_000, endTimeMs = 2_000, text = "你好"))
            )
        )

        assertNull(state.textAt(positionMs = 3_000))
    }

    @Test
    fun appliesDelayWhenResolvingTrackText() {
        val state = DualSubtitleState(
            primary = PrimarySubtitle(
                items = listOf(SubtitleItem(index = 1, startTimeMs = 1_000, endTimeMs = 2_000, text = "Hello"))
            )
        )

        assertEquals(
            DualSubtitleText(primary = "Hello", secondary = ""),
            state.textAt(positionMs = 900, delayMs = 100)
        )
    }

    @Test
    fun returnsCurrentCueStylesWithPrimaryAndSecondaryText() {
        val primaryStyle = SubtitleCueStyle(fontSizeSp = 32f, primaryColor = 0xFFFFFF00.toInt())
        val secondaryStyle = SubtitleCueStyle(fontSizeSp = 24f, primaryColor = 0xFF00FFFF.toInt())
        val state = DualSubtitleState(
            primary = PrimarySubtitle(
                items = listOf(
                    SubtitleItem(
                        index = 1,
                        startTimeMs = 1_000,
                        endTimeMs = 2_000,
                        text = "Hello",
                        style = primaryStyle
                    )
                )
            ),
            secondary = SecondarySubtitle(
                enabled = true,
                items = listOf(
                    SubtitleItem(
                        index = 1,
                        startTimeMs = 1_000,
                        endTimeMs = 2_000,
                        text = "你好",
                        style = secondaryStyle
                    )
                )
            )
        )

        assertEquals(
            DualSubtitleText(
                primary = "Hello",
                secondary = "你好",
                primaryStyle = primaryStyle,
                secondaryStyle = secondaryStyle
            ),
            state.textAt(positionMs = 1_500)
        )
    }
}
