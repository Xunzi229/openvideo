package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HistoryPlaybackMemorySourceTest {

    @Test
    fun historyEntityAndMigrationIncludeSpeedAndAspectRatioColumns() {
        val entitySource = String(Files.readAllBytes(sourceFile("HistoryEntity.kt")))
        val migrationSource = String(Files.readAllBytes(sourceFile("DatabaseMigrations.kt")))
        val dbSource = String(Files.readAllBytes(sourceFile("VideoDatabase.kt")))

        assertTrue(entitySource.contains("val speed: Float"))
        assertTrue(entitySource.contains("val aspectRatioKey: String"))
        assertTrue(entitySource.contains("val contentFrameKey: String"))
        assertTrue(entitySource.contains("val externalSubtitleUri: String"))
        assertTrue(entitySource.contains("val subtitlesEnabled: Boolean"))
        assertTrue(entitySource.contains("val audioTrackGroupIndex: Int"))
        assertTrue(entitySource.contains("val audioTrackIndex: Int"))
        assertTrue(entitySource.contains("val audioMuted: Boolean"))
        assertTrue(migrationSource.contains("MIGRATION_3_4"))
        assertTrue(migrationSource.contains("MIGRATION_4_5"))
        assertTrue(migrationSource.contains("contentFrameKey"))
        assertTrue(migrationSource.contains("ALTER TABLE play_history ADD COLUMN externalSubtitleUri TEXT NOT NULL DEFAULT ''"))
        assertTrue(migrationSource.contains("ALTER TABLE play_history ADD COLUMN subtitlesEnabled INTEGER NOT NULL DEFAULT 1"))
        assertTrue(migrationSource.contains("ALTER TABLE play_history ADD COLUMN audioTrackGroupIndex INTEGER NOT NULL DEFAULT -1"))
        assertTrue(migrationSource.contains("ALTER TABLE play_history ADD COLUMN audioTrackIndex INTEGER NOT NULL DEFAULT -1"))
        assertTrue(migrationSource.contains("ALTER TABLE play_history ADD COLUMN audioMuted INTEGER NOT NULL DEFAULT 0"))
        assertTrue(dbSource.contains("version = 6"))
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "data", "local", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
