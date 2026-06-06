package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesEpisodePlaybackSourceTest {

    @Test
    fun playableEpisodeReadModelCarriesCurrentIdentityVideoFields() {
        val source = sourceText("SeriesEpisodePlaybackEntity.kt")

        assertTrue(source.contains("data class SeriesEpisodePlaybackEntity("))
        assertTrue(source.contains("val episodeId: Long"))
        assertTrue(source.contains("val identityId: Long"))
        assertTrue(source.contains("val videoId: Long"))
        assertTrue(source.contains("val videoTitle: String"))
        assertTrue(source.contains("val videoPath: String"))
        assertTrue(source.contains("val videoDuration: Long"))
        assertTrue(source.contains("val videoSize: Long"))
        assertTrue(source.contains("val videoWidth: Int"))
        assertTrue(source.contains("val videoHeight: Int"))
        assertTrue(source.contains("val videoDateAdded: Long"))
        assertTrue(source.contains("val historyLastPositionMs: Long?"))
    }

    @Test
    fun daoJoinsEpisodesToCurrentMediaIdentityForPlaybackQueue() {
        val source = sourceText("SeriesEpisodeDao.kt")

        assertTrue(source.contains("fun getPlayableEpisodesForSeries(seriesId: Long): Flow<List<SeriesEpisodePlaybackEntity>>"))
        assertTrue(source.contains("FROM episodes"))
        assertTrue(source.contains("INNER JOIN media_identity ON episodes.identityId = media_identity.identityId"))
        assertTrue(source.contains("media_identity.currentVideoId AS videoId"))
        assertTrue(source.contains("media_identity.currentPath AS videoPath"))
        assertTrue(source.contains("AS historyLastPositionMs"))
        assertTrue(source.contains("FROM play_history"))
        assertTrue(source.contains("play_history.mediaIdentityId = media_identity.identityId"))
        assertTrue(source.contains("play_history.videoId = media_identity.currentVideoId"))
        assertTrue(source.contains("ORDER BY play_history.timestamp DESC"))
        assertTrue(source.contains("ORDER BY episodes.season IS NULL, episodes.season, episodes.episodeStart"))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "data", "local", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
