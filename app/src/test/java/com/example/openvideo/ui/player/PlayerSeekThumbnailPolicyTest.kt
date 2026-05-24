package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSeekThumbnailPolicyTest {

    @Test
    fun thumbnailPositionMsCalculatedCorrectly() {
        // durationMs <= Int.MAX_VALUE
        assertEquals(5000L, PlayerSeekThumbnailPolicy.thumbnailPositionMs(5000, 10000, 10000L))
        // durationMs > Int.MAX_VALUE
        val durationMs = 3_000_000_000L
        val max = PlayerTimeline.SCALED_SEEK_BAR_MAX
        assertEquals(1_500_000_000L, PlayerSeekThumbnailPolicy.thumbnailPositionMs(max / 2, max, durationMs))
    }

    @Test
    fun throttleIntervalMsIsConstant() {
        assertEquals(200L, PlayerSeekThumbnailPolicy.throttleIntervalMs())
    }

    @Test
    fun shouldSkipThumbnailForVariousSchemes() {
        // file scheme should not skip
        assertFalse(PlayerSeekThumbnailPolicy.shouldSkipThumbnail("file"))
        assertFalse(PlayerSeekThumbnailPolicy.shouldSkipThumbnail("FILE"))
        assertFalse(PlayerSeekThumbnailPolicy.shouldSkipThumbnail("content"))
        
        // other schemes should skip
        assertTrue(PlayerSeekThumbnailPolicy.shouldSkipThumbnail(null))
        assertTrue(PlayerSeekThumbnailPolicy.shouldSkipThumbnail("http"))
        assertTrue(PlayerSeekThumbnailPolicy.shouldSkipThumbnail("https"))
        assertTrue(PlayerSeekThumbnailPolicy.shouldSkipThumbnail("rtsp"))
    }

    @Test
    fun portraitAndLandscapeControlsBothExposeSeekThumbnailViews() {
        val portrait = readResource("layout", "player_controls.xml")
        val landscape = readResource("layout-land", "player_controls.xml")

        listOf(portrait, landscape).forEach { source ->
            assertTrue(source.contains("@+id/seek_thumbnail_container"))
            assertTrue(source.contains("@+id/seek_thumbnail_image"))
            assertTrue(source.contains("@+id/seek_thumbnail_time"))
        }
    }

    private fun readResource(dir: String, file: String): String =
        String(Files.readAllBytes(resource(dir, file)))

    private fun resource(dir: String, file: String): Path {
        val relativePath = Paths.get("src", "main", "res", dir, file)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
