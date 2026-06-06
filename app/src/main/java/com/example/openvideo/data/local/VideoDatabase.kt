package com.example.openvideo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        HistoryEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistVideoEntity::class,
        MediaIdentityEntity::class,
        MediaPathHistoryEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        MediaSourceEntity::class,
        NetworkRecentItemEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun mediaIdentityDao(): MediaIdentityDao
    abstract fun seriesEpisodeDao(): SeriesEpisodeDao
    abstract fun mediaSourceDao(): MediaSourceDao
    abstract fun networkRecentItemDao(): NetworkRecentItemDao
}
