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
                    exists INTEGER NOT NULL,
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

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
