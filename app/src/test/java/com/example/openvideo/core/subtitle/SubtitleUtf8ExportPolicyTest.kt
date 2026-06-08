package com.example.openvideo.core.subtitle

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleUtf8ExportPolicyTest {

    @Test
    fun exportsSubtitleItemsAsUtf8SrtCopyContent() {
        val plan = SubtitleUtf8ExportPolicy.planSrtCopy(
            items = listOf(
                SubtitleItem(index = 9, startTimeMs = 1_234, endTimeMs = 5_678, text = "Hello\nWorld"),
                SubtitleItem(index = 10, startTimeMs = 3_661_001, endTimeMs = 3_662_045, text = "你好")
            ),
            sourceName = "demo.gbk.srt"
        )

        val expectedContent = """
            1
            00:00:01,234 --> 00:00:05,678
            Hello
            World

            2
            01:01:01,001 --> 01:01:02,045
            你好

        """.trimIndent()

        assertEquals("demo.gbk.utf8.srt", plan.suggestedCopyName)
        assertEquals("UTF-8", plan.charsetName)
        assertEquals(expectedContent, plan.content)
        assertArrayEquals(expectedContent.toByteArray(Charsets.UTF_8), plan.bytes)
        assertEquals(2, plan.lineCount)
        assertFalse(plan.overwritesOriginal)
    }

    @Test
    fun exportCopyNameFallsBackToSubtitleWhenSourceNameIsBlankOrExtensionless() {
        assertEquals(
            "subtitle.utf8.srt",
            SubtitleUtf8ExportPolicy.planSrtCopy(emptyList(), sourceName = "").suggestedCopyName
        )
        assertEquals(
            "subtitle.utf8.srt",
            SubtitleUtf8ExportPolicy.planSrtCopy(emptyList(), sourceName = "subtitle").suggestedCopyName
        )
    }

    @Test
    fun detectsOriginalSubtitleTargetsBeforeExport() {
        assertTrue(
            SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(
                targetUri = "content://subtitles/current.srt",
                originalSubtitleUri = "content://subtitles/current.srt"
            )
        )
        assertTrue(
            SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(
                targetUri = "file:///storage/emulated/0/Movies/demo.srt",
                originalSubtitleUri = "/storage/emulated/0/Movies/demo.srt"
            )
        )
        assertTrue(
            SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(
                targetUri = "file:///E:/Videos/demo.srt",
                originalSubtitleUri = "E:\\Videos\\demo.srt"
            )
        )

        assertFalse(
            SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(
                targetUri = "/storage/emulated/0/Movies/demo.utf8.srt",
                originalSubtitleUri = "/storage/emulated/0/Movies/demo.srt"
            )
        )
        assertFalse(
            SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(
                targetUri = "content://subtitles/current.srt",
                originalSubtitleUri = ""
            )
        )
    }
}
