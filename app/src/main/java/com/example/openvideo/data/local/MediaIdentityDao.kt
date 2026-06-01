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
    suspend fun upsertIdentity(identity: MediaIdentityEntity): Long =
        if (identity.identityId == 0L) {
            insertIdentity(identity)
        } else {
            updateIdentity(identity)
            identity.identityId
        }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIdentity(identity: MediaIdentityEntity): Long

    @Update
    suspend fun updateIdentity(identity: MediaIdentityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPathHistory(pathHistory: MediaPathHistoryEntity)

    @Query("SELECT * FROM media_identity WHERE identityId = :identityId LIMIT 1")
    suspend fun getByIdentityId(identityId: Long): MediaIdentityEntity?

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
