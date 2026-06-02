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
