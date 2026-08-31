package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PlayerSessionQueueCodecTest {

    @Test
    fun queueRecordsRoundTripWithoutLosingMetadata() {
        val records = listOf(
            PlayerSessionQueueRecord(
                id = 42L,
                title = "Episode 01",
                path = "/media/show/episode01.mkv",
                uri = "content://media/external/video/media/42",
                duration = 3_600_000L,
                size = 1_234_567_890L,
                width = 3840,
                height = 2160,
                dateAdded = 1_700_000_000L,
                thumbnailUri = "content://media/external/video/media/42/thumbnail",
                libraryPath = "/media/show/episode01.mkv",
                dateModified = 1_700_000_100L,
                orientationDegrees = 90
            ),
            PlayerSessionQueueRecord(
                id = 43L,
                title = "第二集",
                path = "/media/show/episode02.mkv",
                uri = "file:///media/show/episode02.mkv",
                duration = 0L,
                size = 0L,
                width = 0,
                height = 0,
                dateAdded = 0L,
                thumbnailUri = null,
                libraryPath = "/media/show/episode02.mkv",
                dateModified = 0L,
                orientationDegrees = 0
            )
        )
        val output = ByteArrayOutputStream()

        PlayerSessionQueueCodec.write(records, output)
        val restored = PlayerSessionQueueCodec.read(ByteArrayInputStream(output.toByteArray()))

        assertEquals(records, restored)
    }

    @Test
    fun invalidHeaderIsRejected() {
        val result = runCatching {
            PlayerSessionQueueCodec.read(ByteArrayInputStream(byteArrayOf(0, 0, 0, 0)))
        }

        assertTrue(result.isFailure)
    }
}
