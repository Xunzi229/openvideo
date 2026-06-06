package com.example.openvideo.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoRepositorySeriesEpisodeSourceTest {

    @Test
    fun repositoryMaterializesSeriesEpisodesAfterIdentitySync() {
        val source = repositorySource()

        assertTrue(source.contains("private val seriesEpisodeDao: SeriesEpisodeDao"))
        assertTrue(source.contains("syncSeriesEpisode(video = video, identityId = identityId, now = now)"))
        assertTrue(source.contains("EpisodeNameParser.parse"))
        assertTrue(source.contains("MediaPathNormalizer.normalize(video.path)"))
        assertTrue(source.contains("parentFolderName = parentFolderName(normalizedPath.displayPath)"))
        assertTrue(source.contains("seriesEpisodeDao.getSeriesByKey"))
        assertTrue(source.contains("seriesEpisodeDao.insertSeries"))
        assertTrue(source.contains("seriesEpisodeDao.getEpisodeByIdentityId(identityId)"))
        assertTrue(source.contains("seriesEpisodeDao.upsertEpisode"))
        assertTrue(source.contains("confidence = match.confidence.name"))
        assertTrue(source.contains("rule = match.rule"))
    }

    @Test
    fun repositoryStoresLocalSeriesPosterPathDuringSeriesMaterialization() {
        val source = repositorySource()

        assertTrue(source.contains("LocalArtworkCandidateScanner.candidatesNear(video.path)"))
        assertTrue(source.contains("LocalArtworkFinder.find("))
        assertTrue(source.contains("posterPath = resolveSeriesPosterPath("))
        assertTrue(source.contains("existingPosterPath = existingSeries?.posterPath"))
        assertTrue(source.contains("discoveredPosterPath = discoveredPosterPath"))
    }

    @Test
    fun repositoryDoesNotAutoMaterializeLowConfidenceEpisodeMatches() {
        val source = repositorySource()
        val syncBlock = source
            .substringAfter("private suspend fun syncSeriesEpisode")
            .substringBefore("private fun resolveSeriesPosterPath")

        assertTrue(source.contains("import com.example.openvideo.core.metadata.EpisodeMatchConfidence"))
        assertTrue(syncBlock.contains("if (match.confidence == EpisodeMatchConfidence.LOW) return"))
        assertTrue(syncBlock.indexOf("if (match.confidence == EpisodeMatchConfidence.LOW) return") <
            syncBlock.indexOf("seriesEpisodeDao.getSeriesByKey"))
    }

    @Test
    fun repositoryKeepsSeriesEpisodeMaterializationSeparateFromIdentityConflictHandling() {
        val source = repositorySource()
        val syncBlock = source
            .substringAfter("private suspend fun syncMediaIdentities")
            .substringBefore("private suspend fun syncSeriesEpisode")

        assertTrue(syncBlock.contains("MediaIdentityMatchDecision.Conflict -> null"))
        assertTrue(syncBlock.contains("?: return@forEach"))
        assertTrue(syncBlock.contains("syncSeriesEpisode(video = video, identityId = identityId, now = now)"))
    }

    @Test
    fun repositoryExposesSeriesEpisodeFlowsForDetailScreens() {
        val source = repositorySource()

        assertTrue(source.contains("fun getAllSeries(): Flow<List<SeriesEntity>>"))
        assertTrue(source.contains("fun getEpisodesForSeries(seriesId: Long): Flow<List<EpisodeEntity>>"))
        assertTrue(source.contains("seriesEpisodeDao.getAllSeries()"))
        assertTrue(source.contains("seriesEpisodeDao.getEpisodesForSeries(seriesId)"))
    }

    @Test
    fun repositoryExposesPlayableSeriesEpisodesForPlayerQueue() {
        val source = repositorySource()

        assertTrue(source.contains("fun getPlayableEpisodesForSeries(seriesId: Long): Flow<List<SeriesEpisodePlaybackEntity>>"))
        assertTrue(source.contains("seriesEpisodeDao.getPlayableEpisodesForSeries(seriesId)"))
    }

    private fun repositorySource(): String =
        String(Files.readAllBytes(sourceFile()))

    private fun sourceFile(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "data",
            "repository",
            "VideoRepository.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
