package com.example.openvideo.core.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class SubtitleExportWriterTest {

    @Test
    fun writesUtf8ExportPlanBytesToCallerProvidedStream() {
        val plan = SubtitleUtf8ExportPolicy.planSrtCopy(
            items = listOf(
                SubtitleItem(index = 3, startTimeMs = 1_000, endTimeMs = 2_000, text = "Hello")
            ),
            sourceName = "movie.gbk.srt"
        )
        val out = ByteArrayOutputStream()

        val result = SubtitleExportWriter.writePlanToOutputStream(out, plan)

        assertTrue(result is SubtitleExportWriter.Result.Success)
        assertEquals(plan.content, out.toString(Charsets.UTF_8.name()))
        assertEquals(plan.bytes.size, (result as SubtitleExportWriter.Result.Success).bytesWritten)
    }

    @Test
    fun reportsWriteFailureWithoutOwningUriOrFilesystemPaths() {
        val plan = SubtitleUtf8ExportPolicy.planSrtCopy(
            items = emptyList(),
            sourceName = "broken.srt"
        )

        val result = SubtitleExportWriter.writePlanToOutputStream(FailingOutputStream(), plan)

        assertEquals(
            SubtitleExportWriter.Result.Failure(SubtitleExportWriter.FailureReason.WRITE_FAILED),
            result
        )
    }

    private class FailingOutputStream : OutputStream() {
        override fun write(b: Int) {
            throw IOException("boom")
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            throw IOException("boom")
        }
    }
}
