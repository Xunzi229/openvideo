package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MediaSourceSchemaSourceTest {

    @Test
    fun databaseDefinesMediaSourceEntityDaoAndCurrentVersion() {
        val entitySource = localSourceText("MediaSourceEntity.kt")
        val daoSource = localSourceText("MediaSourceDao.kt")
        val databaseSource = localSourceText("VideoDatabase.kt")
        val appModuleSource = sourceText(appModuleSourceFile())

        assertTrue(entitySource.contains("""tableName = "media_sources""""))
        assertTrue(entitySource.contains("Index(value = [\"type\", \"normalizedUrl\"], unique = true)"))
        assertTrue(entitySource.contains("@PrimaryKey(autoGenerate = true) val sourceId: Long = 0"))
        assertTrue(entitySource.contains("val type: String"))
        assertTrue(entitySource.contains("val name: String"))
        assertTrue(entitySource.contains("val url: String"))
        assertTrue(entitySource.contains("val normalizedUrl: String"))
        assertTrue(entitySource.contains("val displayUrl: String"))
        assertTrue(entitySource.contains("val lastUsedAt: Long = 0L"))
        assertTrue(entitySource.contains("val isEnabled: Boolean = true"))
        assertTrue(entitySource.contains("val createdAt: Long"))
        assertTrue(entitySource.contains("val updatedAt: Long"))

        assertTrue(daoSource.contains("interface MediaSourceDao"))
        assertTrue(daoSource.contains("fun getAll(): Flow<List<MediaSourceEntity>>"))
        assertTrue(daoSource.contains("suspend fun getById(sourceId: Long): MediaSourceEntity?"))
        assertTrue(daoSource.contains("suspend fun getByTypeAndUrl(type: String, normalizedUrl: String): MediaSourceEntity?"))
        assertTrue(daoSource.contains("suspend fun upsert(source: MediaSourceEntity): Long"))
        assertTrue(daoSource.contains("suspend fun update(source: MediaSourceEntity)"))
        assertTrue(daoSource.contains("suspend fun delete(sourceId: Long)"))
        assertTrue(daoSource.contains("suspend fun markUsed(sourceId: Long, lastUsedAt: Long)"))

        assertTrue(databaseSource.contains("MediaSourceEntity::class"))
        assertTrue(databaseSource.contains("version = 10"))
        assertTrue(databaseSource.contains("abstract fun mediaSourceDao(): MediaSourceDao"))
        assertTrue(appModuleSource.contains("fun provideMediaSourceDao(db: VideoDatabase): MediaSourceDao"))
    }

    @Test
    fun migrationEightToNineCreatesMediaSourcesTableAndIndexes() {
        val migrationSource = localSourceText("DatabaseMigrations.kt")

        assertTrue(migrationSource.contains("MIGRATION_8_9"))
        assertTrue(migrationSource.contains("CREATE TABLE IF NOT EXISTS media_sources"))
        assertTrue(migrationSource.contains("sourceId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue(migrationSource.contains("type TEXT NOT NULL"))
        assertTrue(migrationSource.contains("normalizedUrl TEXT NOT NULL"))
        assertTrue(migrationSource.contains("displayUrl TEXT NOT NULL"))
        assertTrue(migrationSource.contains("isEnabled INTEGER NOT NULL DEFAULT 1"))
        assertTrue(migrationSource.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_media_sources_type_normalizedUrl"))
        assertTrue(migrationSource.contains("CREATE INDEX IF NOT EXISTS index_media_sources_type"))
    }

    private fun localSourceText(name: String): String =
        sourceText(localSourceFile(name))

    private fun sourceText(path: Path?): String =
        path?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun localSourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "data", "local", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }

    private fun appModuleSourceFile(): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "di", "AppModule.kt")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
