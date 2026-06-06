package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class NetworkRecentItemSchemaSourceTest {

    @Test
    fun databaseDefinesNetworkRecentItemEntityDaoAndVersionTen() {
        val entitySource = localSourceText("NetworkRecentItemEntity.kt")
        val daoSource = localSourceText("NetworkRecentItemDao.kt")
        val databaseSource = localSourceText("VideoDatabase.kt")
        val appModuleSource = sourceText(appModuleSourceFile())

        assertTrue(entitySource.contains("""tableName = "network_recent_items""""))
        assertTrue(entitySource.contains("Index(value = [\"normalizedUrl\"], unique = true)"))
        assertTrue(entitySource.contains("Index(value = [\"lastPlayedAt\"])"))
        assertTrue(entitySource.contains("@PrimaryKey(autoGenerate = true) val recentId: Long = 0"))
        assertTrue(entitySource.contains("val sourceId: Long? = null"))
        assertTrue(entitySource.contains("val uri: String"))
        assertTrue(entitySource.contains("val normalizedUrl: String"))
        assertTrue(entitySource.contains("val displayUrl: String"))
        assertTrue(entitySource.contains("val title: String"))
        assertTrue(entitySource.contains("val durationMs: Long = 0L"))
        assertTrue(entitySource.contains("val lastPlayedAt: Long"))

        assertTrue(daoSource.contains("interface NetworkRecentItemDao"))
        assertTrue(daoSource.contains("fun getAll(): Flow<List<NetworkRecentItemEntity>>"))
        assertTrue(daoSource.contains("suspend fun getByNormalizedUrl(normalizedUrl: String): NetworkRecentItemEntity?"))
        assertTrue(daoSource.contains("suspend fun upsert(item: NetworkRecentItemEntity): Long"))
        assertTrue(daoSource.contains("suspend fun delete(recentId: Long)"))
        assertTrue(daoSource.contains("suspend fun deleteAll()"))

        assertTrue(databaseSource.contains("NetworkRecentItemEntity::class"))
        assertTrue(databaseSource.contains("version = 10"))
        assertTrue(databaseSource.contains("abstract fun networkRecentItemDao(): NetworkRecentItemDao"))
        assertTrue(appModuleSource.contains("fun provideNetworkRecentItemDao(db: VideoDatabase): NetworkRecentItemDao"))
    }

    @Test
    fun migrationNineToTenCreatesNetworkRecentItemsTableAndIndexes() {
        val migrationSource = localSourceText("DatabaseMigrations.kt")

        assertTrue(migrationSource.contains("MIGRATION_9_10"))
        assertTrue(migrationSource.contains("CREATE TABLE IF NOT EXISTS network_recent_items"))
        assertTrue(migrationSource.contains("recentId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue(migrationSource.contains("sourceId INTEGER DEFAULT NULL"))
        assertTrue(migrationSource.contains("uri TEXT NOT NULL"))
        assertTrue(migrationSource.contains("normalizedUrl TEXT NOT NULL"))
        assertTrue(migrationSource.contains("displayUrl TEXT NOT NULL"))
        assertTrue(migrationSource.contains("title TEXT NOT NULL"))
        assertTrue(migrationSource.contains("durationMs INTEGER NOT NULL DEFAULT 0"))
        assertTrue(migrationSource.contains("lastPlayedAt INTEGER NOT NULL"))
        assertTrue(migrationSource.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_network_recent_items_normalizedUrl"))
        assertTrue(migrationSource.contains("CREATE INDEX IF NOT EXISTS index_network_recent_items_lastPlayedAt"))
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
