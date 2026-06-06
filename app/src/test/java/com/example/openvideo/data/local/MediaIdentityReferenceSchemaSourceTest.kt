package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MediaIdentityReferenceSchemaSourceTest {

    @Test
    fun historyFavoriteAndPlaylistRowsKeepOptionalMediaIdentityId() {
        val historySource = String(Files.readAllBytes(sourceFile("HistoryEntity.kt")))
        val favoriteSource = String(Files.readAllBytes(sourceFile("FavoriteEntity.kt")))
        val playlistVideoSource = String(Files.readAllBytes(sourceFile("PlaylistVideoEntity.kt")))
        val databaseSource = String(Files.readAllBytes(sourceFile("VideoDatabase.kt")))

        assertTrue(historySource.contains("val mediaIdentityId: Long? = null"))
        assertTrue(historySource.contains("""Index(value = ["mediaIdentityId"])"""))
        assertTrue(favoriteSource.contains("val mediaIdentityId: Long? = null"))
        assertTrue(favoriteSource.contains("""Index(value = ["mediaIdentityId"])"""))
        assertTrue(playlistVideoSource.contains("val mediaIdentityId: Long? = null"))
        assertTrue(playlistVideoSource.contains("""Index(value = ["mediaIdentityId"])"""))
        assertTrue(databaseSource.contains("version = 10"))
    }

    @Test
    fun migrationSixToSevenAddsMediaIdentityReferenceColumns() {
        val migrationSource = String(Files.readAllBytes(sourceFile("DatabaseMigrations.kt")))

        assertTrue(migrationSource.contains("MIGRATION_6_7"))
        assertTrue(migrationSource.contains("ALTER TABLE play_history ADD COLUMN mediaIdentityId INTEGER DEFAULT NULL"))
        assertTrue(migrationSource.contains("ALTER TABLE favorites ADD COLUMN mediaIdentityId INTEGER DEFAULT NULL"))
        assertTrue(migrationSource.contains("ALTER TABLE playlist_videos ADD COLUMN mediaIdentityId INTEGER DEFAULT NULL"))
        assertTrue(migrationSource.contains("CREATE INDEX IF NOT EXISTS index_play_history_mediaIdentityId"))
        assertTrue(migrationSource.contains("CREATE INDEX IF NOT EXISTS index_favorites_mediaIdentityId"))
        assertTrue(migrationSource.contains("CREATE INDEX IF NOT EXISTS index_playlist_videos_mediaIdentityId"))
        assertTrue(migrationSource.contains("MIGRATION_6_7"))
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "data", "local", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
