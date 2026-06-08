package com.example.openvideo.ui.player

import com.example.openvideo.core.subtitle.SubtitleInfo
import com.example.openvideo.core.subtitle.SubtitleInfoStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSubtitleInfoUiPolicyTest {

    @Test
    fun formatsLoadedSubtitleDiagnosticsAsCompactMultilineSummary() {
        val summary = PlayerSubtitleInfoUiPolicy.summaryText(
            info = SubtitleInfo(
                sourceLabel = "movie.ass",
                encodingLabel = "UTF-8",
                lineCount = 12,
                firstStartTimeMs = 1_000,
                lastEndTimeMs = 62_500,
                durationMs = 61_500,
                styledLineCount = 3,
                hasStyledCues = true,
                isEmpty = false,
                status = SubtitleInfoStatus.Loaded
            ),
            labels = labels()
        )

        assertEquals(
            """
                Source: movie.ass
                Lines: 12
                Time range: 00:00:01.000 - 00:01:02.500
                Encoding: UTF-8
                Styled lines: 3
            """.trimIndent(),
            summary
        )
    }

    @Test
    fun formatsEmptySubtitleDiagnosticsAsEmptyState() {
        val summary = PlayerSubtitleInfoUiPolicy.summaryText(
            info = SubtitleInfo(
                sourceLabel = "Unknown subtitle",
                encodingLabel = "Auto detect",
                lineCount = 0,
                firstStartTimeMs = 0,
                lastEndTimeMs = 0,
                durationMs = 0,
                styledLineCount = 0,
                hasStyledCues = false,
                isEmpty = true,
                status = SubtitleInfoStatus.Empty
            ),
            labels = labels()
        )

        assertEquals("No loaded subtitles", summary)
    }

    private fun labels(): PlayerSubtitleInfoUiPolicy.Labels =
        PlayerSubtitleInfoUiPolicy.Labels(
            noLoadedSubtitle = "No loaded subtitles",
            sourcePrefix = "Source:",
            linesPrefix = "Lines:",
            timeRangePrefix = "Time range:",
            encodingPrefix = "Encoding:",
            styledLinesPrefix = "Styled lines:"
        )
}
