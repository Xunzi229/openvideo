package com.example.openvideo.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoRepositoryMediaIdentitySourceTest {

    @Test
    fun repositorySyncsMediaIdentityAfterSuccessfulScan() {
        val source = repositorySource()

        assertTrue(source.contains("private val mediaIdentityDao: MediaIdentityDao"))
        assertTrue(source.contains(".onEach { outcome ->"))
        assertTrue(source.contains("if (outcome is VideoScanOutcome.Success)"))
        assertTrue(source.contains("syncMediaIdentities(outcome.videos)"))
        assertTrue(source.contains("MediaFingerprintPolicy.fromFields"))
        assertTrue(source.contains("MediaIdentityMatcher.match"))
        assertTrue(source.contains("mediaIdentityDao.getByCurrentVideoId(video.id)"))
        assertTrue(source.contains("mediaIdentityDao.getByNormalizedPathKey(fingerprint.normalizedPathKey)"))
        assertTrue(source.contains("mediaIdentityDao.findFingerprintCandidates"))
        assertTrue(source.contains("mediaIdentityDao.upsertIdentity"))
        assertTrue(source.contains("mediaIdentityDao.upsertPathHistory"))
        assertTrue(source.contains("dateAdded * MILLIS_PER_SECOND"))
    }

    @Test
    fun repositoryKeepsIdentitySyncSeparateFromHistoryFavoriteAndPlaylistRecovery() {
        val source = repositorySource()
        val syncBlock = source
            .substringAfter("private suspend fun syncMediaIdentities")
            .substringBefore("private suspend fun resolveMediaIdentity")

        assertTrue(syncBlock.contains("MediaIdentityMatchDecision.Conflict -> null"))
        assertTrue(syncBlock.contains("MediaIdentityMatchDecision.NoMatch -> MediaIdentityEntity("))
        assertTrue(syncBlock.contains("MediaPathHistoryEntity("))
        assertTrue(!syncBlock.contains("historyDao.upsert"))
        assertTrue(!syncBlock.contains("favoriteDao.insert"))
        assertTrue(!syncBlock.contains("playlistDao.insertVideo"))
    }

    @Test
    fun repositoryWritesMediaIdentityIdWhenPersistingHistoryFavoriteAndPlaylistRows() {
        val source = repositorySource()

        assertTrue(source.contains("private suspend fun resolveMediaIdentityId(video: VideoItem): Long?"))
        assertTrue(source.contains("mediaIdentityId = resolveMediaIdentityId(video)"))
        assertTrue(source.contains("HistoryEntity("))
        assertTrue(source.contains("FavoriteEntity("))
        assertTrue(source.contains("PlaylistVideoEntity("))
    }

    @Test
    fun repositoryFallsBackToMediaIdentityWhenLoadingHistoryByVideoId() {
        val source = repositorySource()

        assertTrue(source.contains("historyDao.getByVideoId(videoId) ?:"))
        assertTrue(source.contains("mediaIdentityDao.getByCurrentVideoId(videoId)"))
        assertTrue(source.contains("historyDao.getByMediaIdentityId(identity.identityId)"))
    }

    @Test
    fun repositoryUsesIdentityAwareFavoriteListAndFavoriteChecks() {
        val source = repositorySource()

        assertTrue(source.contains("fun getFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllWithIdentityFallback()"))
        assertTrue(source.contains("suspend fun isFavorite(videoId: Long): Boolean ="))
        assertTrue(source.contains("favoriteDao.isFavorite(videoId)"))
        assertTrue(source.contains("mediaIdentityDao.getByCurrentVideoId(videoId)"))
        assertTrue(source.contains("favoriteDao.isFavoriteByMediaIdentityId(identity.identityId)"))
    }

    @Test
    fun repositoryTogglesExistingIdentityFavoriteInsteadOfDuplicatingIt() {
        val source = repositorySource()
        val toggleBlock = source
            .substringAfter("suspend fun toggleFavorite(video: VideoItem)")
            .substringBefore("suspend fun isFavorite(videoId: Long)")

        assertTrue(toggleBlock.contains("val identityId = resolveMediaIdentityId(video)"))
        assertTrue(toggleBlock.contains("favoriteDao.isFavoriteByMediaIdentityId(identityId)"))
        assertTrue(toggleBlock.contains("favoriteDao.deleteByMediaIdentityId(identityId)"))
        assertTrue(toggleBlock.contains("mediaIdentityId = identityId"))
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
