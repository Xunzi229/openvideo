package com.openvideo.app.core.player

import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MediaExportFileTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun failedFinalizationDeletesPartialOutputAndPropagatesFailure() {
        val file = temporary.newFile("clip.mp4")
        assertThrows(IOException::class.java) {
            MediaExportFile.write(file) {
                file.writeText("partial samples")
                throw IOException("muxer.stop failed")
            }
        }
        assertFalse(file.exists())
    }

    @Test fun completedExportKeepsOutput() {
        val file = temporary.newFile("clip.mp4")
        assertEquals(file.absolutePath, MediaExportFile.write(file) { file.writeText("finalized") })
        assertEquals("finalized", file.readText())
    }

    @Test fun framesBeforeRequestedCutKeepTheirRelativeSpacing() {
        val actualKeyFrameUs = 8_000_000L // user requested 10s
        assertEquals(listOf(0L, 40_000L, 1_000_000L, 2_000_000L),
            listOf(8_000_000L, 8_040_000L, 9_000_000L, 10_000_000L).map {
                ClipTimestampPolicy.relativeTimeUs(it, actualKeyFrameUs)
            })
        assertThrows(IllegalArgumentException::class.java) { ClipTimestampPolicy.relativeTimeUs(1, 2) }
    }
}
