package com.example.openvideo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesEpisodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: SeriesEntity): Long

    @Query(
        """
        SELECT * FROM series
        WHERE normalizedTitleKey = :normalizedTitleKey
            AND folderPath = :folderPath
        LIMIT 1
        """
    )
    suspend fun getSeriesByKey(normalizedTitleKey: String, folderPath: String): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisode(episode: EpisodeEntity): Long

    @Query("SELECT * FROM series ORDER BY updatedAt DESC")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY season IS NULL, season, episodeStart")
    fun getEpisodesForSeries(seriesId: Long): Flow<List<EpisodeEntity>>

    @Query(
        """
        SELECT
            episodes.episodeId AS episodeId,
            episodes.seriesId AS seriesId,
            episodes.identityId AS identityId,
            episodes.season AS season,
            episodes.episodeStart AS episodeStart,
            episodes.episodeEnd AS episodeEnd,
            episodes.episodeTitle AS episodeTitle,
            episodes.confidence AS confidence,
            episodes.rule AS rule,
            media_identity.currentVideoId AS videoId,
            media_identity.title AS videoTitle,
            media_identity.currentPath AS videoPath,
            media_identity.durationMs AS videoDuration,
            media_identity.sizeBytes AS videoSize,
            media_identity.width AS videoWidth,
            media_identity.height AS videoHeight,
            media_identity.lastSeen / 1000 AS videoDateAdded,
            (
                SELECT play_history.lastPosition
                FROM play_history
                WHERE play_history.mediaIdentityId = media_identity.identityId
                    OR play_history.videoId = media_identity.currentVideoId
                ORDER BY play_history.timestamp DESC
                LIMIT 1
            ) AS historyLastPositionMs
        FROM episodes
        INNER JOIN media_identity ON episodes.identityId = media_identity.identityId
        WHERE episodes.seriesId = :seriesId
        ORDER BY episodes.season IS NULL, episodes.season, episodes.episodeStart
        """
    )
    fun getPlayableEpisodesForSeries(seriesId: Long): Flow<List<SeriesEpisodePlaybackEntity>>

    @Query("SELECT * FROM episodes WHERE identityId = :identityId LIMIT 1")
    suspend fun getEpisodeByIdentityId(identityId: Long): EpisodeEntity?
}
