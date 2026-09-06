package com.openvideo.app.data.local

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaIdentityDaoTest {
    @Test
    fun insertsANewIdentityAndReturnsItsPersistedId() = runBlocking {
        val dao = InMemoryIdentityDao()
        val incoming = identity()
        val id = dao.upsertIdentity(incoming)
        assertEquals(listOf(incoming.copy(identityId = id)), dao.rows.values.toList())
    }

    @Test
    fun rescanningSameVideoUpdatesMetadataWithoutCreatingAnotherIdentity() = runBlocking {
        val previous = identity(identityId = 10)
        val dao = InMemoryIdentityDao(previous)
        val incoming = previous.copy(title = "updated", lastSeen = 20)
        assertEquals(10L, dao.upsertIdentity(incoming))
        assertEquals(listOf(incoming), dao.rows.values.toList())
    }

    @Test
    fun canMoveExistingIdentityToUnclaimedPathAndVideoId() = runBlocking {
        val previous = identity(identityId = 10)
        val dao = InMemoryIdentityDao(previous)
        val moved = previous.copy(currentVideoId = 200, normalizedPathKey = "/new.mp4", currentPath = "/new.mp4")
        assertEquals(10L, dao.upsertIdentity(moved))
        assertEquals(moved, dao.rows[10])
    }

    @Test
    fun updateDoesNotOverwriteAnotherPathOwner() = runBlocking {
        val previous = identity(identityId = 10)
        val owner = identity(identityId = 11, videoId = 200, path = "/other.mp4")
        val dao = InMemoryIdentityDao(previous, owner)
        assertEquals(11L, dao.upsertIdentity(previous.copy(normalizedPathKey = owner.normalizedPathKey)))
        assertEquals(listOf(previous, owner), dao.rows.values.toList())
    }

    @Test
    fun updateDoesNotOverwriteAnotherVideoIdOwner() = runBlocking {
        val previous = identity(identityId = 10)
        val owner = identity(identityId = 11, videoId = 200, path = "/other.mp4")
        val dao = InMemoryIdentityDao(previous, owner)
        assertEquals(11L, dao.upsertIdentity(previous.copy(currentVideoId = owner.currentVideoId)))
        assertEquals(listOf(previous, owner), dao.rows.values.toList())
    }

    @Test
    fun duplicateInsertPreservesIdentityAndFirstSeenWhileRefreshingMetadata() = runBlocking {
        val previous = identity(identityId = 10)
        val dao = InMemoryIdentityDao(previous)
        val incoming = identity().copy(sizeBytes = 500, firstSeen = 20, lastSeen = 20)
        assertEquals(10L, dao.upsertIdentity(incoming))
        assertEquals(listOf(incoming.copy(identityId = 10, firstSeen = previous.firstSeen)), dao.rows.values.toList())
    }

    @Test
    fun duplicatePathCanAdoptANewVideoId() = runBlocking {
        val previous = identity(identityId = 10)
        val dao = InMemoryIdentityDao(previous)
        val incoming = identity(videoId = 200)
        assertEquals(10L, dao.upsertIdentity(incoming))
        assertEquals(listOf(incoming.copy(identityId = 10)), dao.rows.values.toList())
    }

    @Test
    fun duplicateVideoIdCanAdoptANewPath() = runBlocking {
        val previous = identity(identityId = 10)
        val dao = InMemoryIdentityDao(previous)
        val incoming = identity(path = "/moved.mp4")
        assertEquals(10L, dao.upsertIdentity(incoming))
        assertEquals(listOf(incoming.copy(identityId = 10)), dao.rows.values.toList())
    }

    @Test
    fun conflictingPathAndVideoOwnersAreNotMergedOrDeleted() = runBlocking {
        val pathOwner = identity(identityId = 10)
        val videoOwner = identity(identityId = 11, videoId = 200, path = "/other.mp4")
        val dao = InMemoryIdentityDao(pathOwner, videoOwner)
        assertEquals(10L, dao.upsertIdentity(identity(videoId = 200)))
        assertEquals(listOf(pathOwner, videoOwner), dao.rows.values.toList())
    }

    private fun identity(identityId: Long = 0, videoId: Long = 100, path: String = "/movie.mp4") =
        MediaIdentityEntity(identityId, videoId, "Movie", path, path, "movie", 100, 1_000, 1920, 1080, 1, 1, 1)

    // Exercises the DAO default method, without simulating Room transactions or SQLite.
    private class InMemoryIdentityDao(vararg initial: MediaIdentityEntity) : MediaIdentityDao {
        val rows = initial.associateByTo(linkedMapOf()) { it.identityId }

        override suspend fun insertIdentityIgnoringConflicts(identity: MediaIdentityEntity): Long {
            if (rows.values.any { it.currentVideoId == identity.currentVideoId || it.normalizedPathKey == identity.normalizedPathKey }) return -1
            val id = (rows.keys.maxOrNull() ?: 0) + 1
            rows[id] = identity.copy(identityId = id)
            return id
        }

        override suspend fun updateIdentity(identity: MediaIdentityEntity) {
            check(rows.values.none {
                it.identityId != identity.identityId &&
                    (it.currentVideoId == identity.currentVideoId || it.normalizedPathKey == identity.normalizedPathKey)
            })
            if (rows.containsKey(identity.identityId)) rows[identity.identityId] = identity
        }

        override suspend fun getByCurrentVideoId(videoId: Long) = rows.values.find { it.currentVideoId == videoId }
        override suspend fun getByNormalizedPathKey(normalizedPathKey: String) = rows.values.find { it.normalizedPathKey == normalizedPathKey }
        override suspend fun getByIdentityId(identityId: Long) = rows[identityId]
        override suspend fun getByIdentityIds(identityIds: List<Long>) = identityIds.mapNotNull(rows::get)
        override suspend fun insertIdentity(identity: MediaIdentityEntity): Long = error("Strict insert must not be used by upsert")
        override suspend fun upsertPathHistory(pathHistory: MediaPathHistoryEntity): Unit = error("Not used by identity upsert")
        override suspend fun findFingerprintCandidates(sizeBytes: Long, durationMs: Long, width: Int, height: Int): List<MediaIdentityEntity> = error("Not used by identity upsert")
        override suspend fun getPathHistory(identityId: Long): List<MediaPathHistoryEntity> = error("Not used by identity upsert")
    }
}
