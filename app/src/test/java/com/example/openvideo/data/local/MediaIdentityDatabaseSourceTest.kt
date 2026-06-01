package com.example.openvideo.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MediaIdentityDatabaseSourceTest {

    @Test
    fun databaseDefinesMediaIdentityTablesDaoAndVersionSix() {
        val identitySource = String(Files.readAllBytes(sourceFile("MediaIdentityEntity.kt")))
        val pathHistorySource = String(Files.readAllBytes(sourceFile("MediaPathHistoryEntity.kt")))
        val daoSource = String(Files.readAllBytes(sourceFile("MediaIdentityDao.kt")))
        val databaseSource = String(Files.readAllBytes(sourceFile("VideoDatabase.kt")))

        assertTrue(identitySource.contains("""@Entity("""))
        assertTrue(identitySource.contains("""tableName = "media_identity""""))
        assertTrue(identitySource.contains("val identityId: Long"))
        assertTrue(identitySource.contains("val currentVideoId: Long"))
        assertTrue(identitySource.contains("val currentPath: String"))
        assertTrue(identitySource.contains("val normalizedPathKey: String"))
        assertTrue(identitySource.contains("val normalizedTitleKey: String"))
        assertTrue(identitySource.contains("val sizeBytes: Long"))
        assertTrue(identitySource.contains("val durationMs: Long"))
        assertTrue(identitySource.contains("val modifiedTime: Long"))
        assertTrue(identitySource.contains("val firstSeen: Long"))
        assertTrue(identitySource.contains("val lastSeen: Long"))
        assertTrue(identitySource.contains("""name = "index_media_identity_fingerprint""""))

        assertTrue(pathHistorySource.contains("""tableName = "media_path_history""""))
        assertTrue(pathHistorySource.contains("""primaryKeys = ["identityId", "normalizedPathKey"]"""))
        assertTrue(pathHistorySource.contains("val seenAt: Long"))
        assertTrue(pathHistorySource.contains("val exists: Boolean"))

        assertTrue(daoSource.contains("interface MediaIdentityDao"))
        assertTrue(daoSource.contains("suspend fun upsertIdentity"))
        assertTrue(daoSource.contains("@Transaction"))
        assertTrue(daoSource.contains("OnConflictStrategy.ABORT"))
        assertTrue(daoSource.contains("suspend fun insertIdentity"))
        assertTrue(daoSource.contains("suspend fun updateIdentity"))
        assertFalse(
            daoSource.contains("@Insert(onConflict = OnConflictStrategy.REPLACE)\r\n    suspend fun upsertIdentity") ||
                daoSource.contains("@Insert(onConflict = OnConflictStrategy.REPLACE)\n    suspend fun upsertIdentity")
        )
        assertTrue(daoSource.contains("suspend fun upsertPathHistory"))
        assertTrue(daoSource.contains("getByCurrentVideoId"))
        assertTrue(daoSource.contains("getByNormalizedPathKey"))
        assertTrue(daoSource.contains("findFingerprintCandidates"))

        assertTrue(databaseSource.contains("MediaIdentityEntity::class"))
        assertTrue(databaseSource.contains("MediaPathHistoryEntity::class"))
        assertTrue(databaseSource.contains("version = 6"))
        assertTrue(databaseSource.contains("abstract fun mediaIdentityDao(): MediaIdentityDao"))
    }

    @Test
    fun migrationFiveToSixCreatesMediaIdentityTablesAndIndexes() {
        val migrationSource = String(Files.readAllBytes(sourceFile("DatabaseMigrations.kt")))

        assertTrue(migrationSource.contains("MIGRATION_5_6"))
        assertTrue(migrationSource.contains("CREATE TABLE IF NOT EXISTS media_identity"))
        assertTrue(migrationSource.contains("identityId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue(migrationSource.contains("normalizedPathKey TEXT NOT NULL"))
        assertTrue(migrationSource.contains("normalizedTitleKey TEXT NOT NULL"))
        assertTrue(migrationSource.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_media_identity_currentVideoId"))
        assertTrue(migrationSource.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_media_identity_normalizedPathKey"))
        assertTrue(migrationSource.contains("CREATE INDEX IF NOT EXISTS index_media_identity_fingerprint"))
        assertTrue(migrationSource.contains("CREATE TABLE IF NOT EXISTS media_path_history"))
        assertTrue(migrationSource.contains("PRIMARY KEY(identityId, normalizedPathKey)"))
        assertTrue(migrationSource.contains("FOREIGN KEY(identityId) REFERENCES media_identity(identityId)"))
        assertTrue(migrationSource.contains("MIGRATION_5_6"))
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "data", "local", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
