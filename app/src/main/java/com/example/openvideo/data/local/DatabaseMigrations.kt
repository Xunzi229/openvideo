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

    val ALL = arrayOf(MIGRATION_1_2)
}
