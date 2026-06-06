package com.example.openvideo.ui.series

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesEpisodePlaybackUiSourceTest {

    @Test
    fun uiStateMapsPlayableEpisodeFieldsIntoVideoItem() {
        val source = sourceText("SeriesEpisodeUiState.kt")

        assertTrue(source.contains("import com.example.openvideo.core.media.LocalMediaUriPolicy"))
        assertTrue(source.contains("import com.example.openvideo.data.local.SeriesEpisodePlaybackEntity"))
        assertTrue(source.contains("import com.example.openvideo.data.model.VideoItem"))
        assertTrue(source.contains("val videoId: Long"))
        assertTrue(source.contains("val videoTitle: String"))
        assertTrue(source.contains("val videoPath: String"))
        assertTrue(source.contains("val videoDuration: Long"))
        assertTrue(source.contains("val videoSize: Long"))
        assertTrue(source.contains("val videoWidth: Int"))
        assertTrue(source.contains("val videoHeight: Int"))
        assertTrue(source.contains("val videoDateAdded: Long"))
        assertTrue(source.contains("val isAvailable: Boolean"))
        assertTrue(source.contains("val watchStatus: SeriesEpisodeWatchStatus"))
        assertTrue(source.contains("fun from(entity: SeriesEpisodePlaybackEntity): SeriesEpisodeUiState"))
        assertTrue(source.contains("isAvailable = SeriesEpisodeAvailabilityPolicy.isAvailable(entity.videoPath)"))
        assertTrue(source.contains("watchStatus = SeriesEpisodeWatchStatusPolicy.status("))
        assertTrue(source.contains("historyLastPositionMs = entity.historyLastPositionMs"))
        assertTrue(source.contains("durationMs = entity.videoDuration"))
        assertTrue(source.contains("fun toVideoItem(): VideoItem"))
        assertTrue(source.contains("id = videoId"))
        assertTrue(source.contains("uri = LocalMediaUriPolicy.playbackUri(videoPath)"))
        assertTrue(source.contains("thumbnailUri = null"))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "series", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
