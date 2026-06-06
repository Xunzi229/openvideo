package com.example.openvideo.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS playlists (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS playlist_videos (
                    playlistId INTEGER NOT NULL,
                    videoId INTEGER NOT NULL,
                    videoTitle TEXT NOT NULL,
                    videoPath TEXT NOT NULL,
                    videoDuration INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    PRIMARY KEY(playlistId, videoId),
                    FOREIGN KEY(playlistId) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE play_history ADD COLUMN speed REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE play_history ADD COLUMN aspectRatioKey TEXT NOT NULL DEFAULT 'fit'")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE play_history ADD COLUMN externalSubtitleUri TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE play_history ADD COLUMN subtitlesEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE play_history ADD COLUMN audioTrackGroupIndex INTEGER NOT NULL DEFAULT -1")
            db.execSQL("ALTER TABLE play_history ADD COLUMN audioTrackIndex INTEGER NOT NULL DEFAULT -1")
            db.execSQL("ALTER TABLE play_history ADD COLUMN audioMuted INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE play_history ADD COLUMN contentFrameKey TEXT NOT NULL DEFAULT 'off'"
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS media_identity (
                    identityId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    currentVideoId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    currentPath TEXT NOT NULL,
                    normalizedPathKey TEXT NOT NULL,
                    normalizedTitleKey TEXT NOT NULL,
                    sizeBytes INTEGER NOT NULL,
                    durationMs INTEGER NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    modifiedTime INTEGER NOT NULL,
                    firstSeen INTEGER NOT NULL,
                    lastSeen INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_media_identity_currentVideoId
                ON media_identity(currentVideoId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_media_identity_normalizedPathKey
                ON media_identity(normalizedPathKey)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_media_identity_fingerprint
                ON media_identity(sizeBytes, durationMs, width, height)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS media_path_history (
                    identityId INTEGER NOT NULL,
                    path TEXT NOT NULL,
                    normalizedPathKey TEXT NOT NULL,
                    seenAt INTEGER NOT NULL,
                    fileExists INTEGER NOT NULL,
                    PRIMARY KEY(identityId, normalizedPathKey),
                    FOREIGN KEY(identityId) REFERENCES media_identity(identityId) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_media_path_history_identityId
                ON media_path_history(identityId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_media_path_history_normalizedPathKey
                ON media_path_history(normalizedPathKey)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE play_history ADD COLUMN mediaIdentityId INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE favorites ADD COLUMN mediaIdentityId INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE playlist_videos ADD COLUMN mediaIdentityId INTEGER DEFAULT NULL")
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_play_history_mediaIdentityId
                ON play_history(mediaIdentityId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_favorites_mediaIdentityId
                ON favorites(mediaIdentityId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_playlist_videos_mediaIdentityId
                ON playlist_videos(mediaIdentityId)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS series (
                    seriesId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    normalizedTitleKey TEXT NOT NULL,
                    folderPath TEXT NOT NULL,
                    posterPath TEXT DEFAULT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_series_normalizedTitleKey_folderPath
                ON series(normalizedTitleKey, folderPath)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS episodes (
                    episodeId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    seriesId INTEGER NOT NULL,
                    identityId INTEGER NOT NULL,
                    season INTEGER DEFAULT NULL,
                    episodeStart INTEGER NOT NULL,
                    episodeEnd INTEGER DEFAULT NULL,
                    episodeTitle TEXT NOT NULL,
                    confidence TEXT NOT NULL,
                    rule TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(seriesId) REFERENCES series(seriesId) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(identityId) REFERENCES media_identity(identityId) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_episodes_seriesId
                ON episodes(seriesId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_episodes_identityId
                ON episodes(identityId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_episodes_series_order
                ON episodes(seriesId, season, episodeStart)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS media_sources (
                    sourceId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    name TEXT NOT NULL,
                    url TEXT NOT NULL,
                    normalizedUrl TEXT NOT NULL,
                    displayUrl TEXT NOT NULL,
                    lastUsedAt INTEGER NOT NULL DEFAULT 0,
                    isEnabled INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_media_sources_type
                ON media_sources(type)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_media_sources_type_normalizedUrl
                ON media_sources(type, normalizedUrl)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS network_recent_items (
                    recentId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sourceId INTEGER DEFAULT NULL,
                    uri TEXT NOT NULL,
                    normalizedUrl TEXT NOT NULL,
                    displayUrl TEXT NOT NULL,
                    title TEXT NOT NULL,
                    durationMs INTEGER NOT NULL DEFAULT 0,
                    lastPlayedAt INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_network_recent_items_normalizedUrl
                ON network_recent_items(normalizedUrl)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_network_recent_items_lastPlayedAt
                ON network_recent_items(lastPlayedAt)
                """.trimIndent()
            )
        }
    }

    val ALL = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10
    )
}
