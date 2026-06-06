package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesEpisodeSchemaSourceTest {

    @Test
    fun databaseDefinesSeriesEpisodeEntitiesDao() {
        val seriesSource = localSourceText("SeriesEntity.kt")
        val episodeSource = localSourceText("EpisodeEntity.kt")
        val daoSource = localSourceText("SeriesEpisodeDao.kt")
        val databaseSource = localSourceText("VideoDatabase.kt")
        val appModuleSource = sourceText(appModuleSourceFile())

        assertTrue(seriesSource.contains("""tableName = "series""""))
        assertTrue(seriesSource.contains("Index(value = [\"normalizedTitleKey\", \"folderPath\"], unique = true)"))
        assertTrue(seriesSource.contains("@PrimaryKey(autoGenerate = true) val seriesId: Long = 0"))
        assertTrue(seriesSource.contains("val title: String"))
        assertTrue(seriesSource.contains("val normalizedTitleKey: String"))
        assertTrue(seriesSource.contains("val folderPath: String"))
        assertTrue(seriesSource.contains("val posterPath: String? = null"))
        assertTrue(seriesSource.contains("val createdAt: Long"))
        assertTrue(seriesSource.contains("val updatedAt: Long"))

        assertTrue(episodeSource.contains("""tableName = "episodes""""))
        assertTrue(episodeSource.contains("entity = SeriesEntity::class"))
        assertTrue(episodeSource.contains("entity = MediaIdentityEntity::class"))
        assertTrue(episodeSource.contains("onDelete = ForeignKey.CASCADE"))
        assertTrue(episodeSource.contains("Index(value = [\"seriesId\"])"))
        assertTrue(episodeSource.contains("Index(value = [\"identityId\"], unique = true)"))
        assertTrue(episodeSource.contains("Index(value = [\"seriesId\", \"season\", \"episodeStart\"]"))
        assertTrue(episodeSource.contains("@PrimaryKey(autoGenerate = true) val episodeId: Long = 0"))
        assertTrue(episodeSource.contains("val seriesId: Long"))
        assertTrue(episodeSource.contains("val identityId: Long"))
        assertTrue(episodeSource.contains("val season: Int? = null"))
        assertTrue(episodeSource.contains("val episodeStart: Int"))
        assertTrue(episodeSource.contains("val episodeEnd: Int? = null"))
        assertTrue(episodeSource.contains("val episodeTitle: String = \"\""))
        assertTrue(episodeSource.contains("val confidence: String"))
        assertTrue(episodeSource.contains("val rule: String"))

        assertTrue(daoSource.contains("interface SeriesEpisodeDao"))
        assertTrue(daoSource.contains("suspend fun insertSeries"))
        assertTrue(daoSource.contains("suspend fun getSeriesByKey(normalizedTitleKey: String, folderPath: String): SeriesEntity?"))
        assertTrue(daoSource.contains("suspend fun upsertEpisode"))
        assertTrue(daoSource.contains("fun getAllSeries(): Flow<List<SeriesEntity>>"))
        assertTrue(daoSource.contains("fun getEpisodesForSeries(seriesId: Long): Flow<List<EpisodeEntity>>"))
        assertTrue(daoSource.contains("suspend fun getEpisodeByIdentityId(identityId: Long): EpisodeEntity?"))

        assertTrue(databaseSource.contains("SeriesEntity::class"))
        assertTrue(databaseSource.contains("EpisodeEntity::class"))
        assertTrue(databaseSource.contains("abstract fun seriesEpisodeDao(): SeriesEpisodeDao"))
        assertTrue(appModuleSource.contains("fun provideSeriesEpisodeDao(db: VideoDatabase): SeriesEpisodeDao"))
    }

    @Test
    fun migrationSevenToEightCreatesSeriesEpisodeTablesAndIndexes() {
        val migrationSource = localSourceText("DatabaseMigrations.kt")

        assertTrue(migrationSource.contains("MIGRATION_7_8"))
        assertTrue(migrationSource.contains("CREATE TABLE IF NOT EXISTS series"))
        assertTrue(migrationSource.contains("seriesId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue(migrationSource.contains("normalizedTitleKey TEXT NOT NULL"))
        assertTrue(migrationSource.contains("folderPath TEXT NOT NULL"))
        assertTrue(migrationSource.contains("posterPath TEXT DEFAULT NULL"))
        assertTrue(migrationSource.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_series_normalizedTitleKey_folderPath"))
        assertTrue(migrationSource.contains("CREATE TABLE IF NOT EXISTS episodes"))
        assertTrue(migrationSource.contains("episodeId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue(migrationSource.contains("identityId INTEGER NOT NULL"))
        assertTrue(migrationSource.contains("FOREIGN KEY(seriesId) REFERENCES series(seriesId)"))
        assertTrue(migrationSource.contains("FOREIGN KEY(identityId) REFERENCES media_identity(identityId)"))
        assertTrue(migrationSource.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_episodes_identityId"))
        assertTrue(migrationSource.contains("CREATE INDEX IF NOT EXISTS index_episodes_series_order"))
        assertTrue(migrationSource.contains("MIGRATION_7_8"))
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
