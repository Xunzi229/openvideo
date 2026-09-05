package com.example.openvideo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface MediaIdentityDao {

    @Transaction
    suspend fun upsertIdentity(identity: MediaIdentityEntity): Long {
        if (identity.identityId != 0L) {
            val pathOwner = getByNormalizedPathKey(identity.normalizedPathKey)
            if (pathOwner != null && pathOwner.identityId != identity.identityId) {
                return pathOwner.identityId
            }

            val videoOwner = getByCurrentVideoId(identity.currentVideoId)
            if (videoOwner != null && videoOwner.identityId != identity.identityId) {
                return videoOwner.identityId
            }

            updateIdentity(identity)
            return identity.identityId
        }

        val insertedId = insertIdentityIgnoringConflicts(identity)
        if (insertedId > 0L) return insertedId

        // A scan can race with another scan, or a path can be reused with changed metadata.
        // Resolve the row that won either unique constraint instead of retrying an ABORT insert.
        val existing = getByNormalizedPathKey(identity.normalizedPathKey)
            ?: getByCurrentVideoId(identity.currentVideoId)
            ?: return insertIdentityIgnoringConflicts(identity)
        val existingIdentity = identity.copy(identityId = existing.identityId)
        val existingVideoOwner = getByCurrentVideoId(identity.currentVideoId)
        if (existingVideoOwner == null || existingVideoOwner.identityId == existing.identityId) {
            updateIdentity(existingIdentity)
        }
        return existing.identityId
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIdentity(identity: MediaIdentityEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIdentityIgnoringConflicts(identity: MediaIdentityEntity): Long

    @Update
    suspend fun updateIdentity(identity: MediaIdentityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPathHistory(pathHistory: MediaPathHistoryEntity)

    @Query("SELECT * FROM media_identity WHERE identityId = :identityId LIMIT 1")
    suspend fun getByIdentityId(identityId: Long): MediaIdentityEntity?

    @Query("SELECT * FROM media_identity WHERE identityId IN (:identityIds)")
    suspend fun getByIdentityIds(identityIds: List<Long>): List<MediaIdentityEntity>

    @Query("SELECT * FROM media_identity WHERE currentVideoId = :videoId LIMIT 1")
    suspend fun getByCurrentVideoId(videoId: Long): MediaIdentityEntity?

    @Query("SELECT * FROM media_identity WHERE normalizedPathKey = :normalizedPathKey LIMIT 1")
    suspend fun getByNormalizedPathKey(normalizedPathKey: String): MediaIdentityEntity?

    @Query(
        """
        SELECT * FROM media_identity
        WHERE sizeBytes = :sizeBytes
            AND durationMs = :durationMs
            AND width = :width
            AND height = :height
        ORDER BY lastSeen DESC
        """
    )
    suspend fun findFingerprintCandidates(
        sizeBytes: Long,
        durationMs: Long,
        width: Int,
        height: Int
    ): List<MediaIdentityEntity>

    @Query("SELECT * FROM media_path_history WHERE identityId = :identityId ORDER BY seenAt DESC")
    suspend fun getPathHistory(identityId: Long): List<MediaPathHistoryEntity>
}
