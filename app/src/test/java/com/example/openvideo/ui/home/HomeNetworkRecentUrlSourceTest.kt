package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class HomeNetworkRecentUrlSourceTest {

    @Test
    fun homeViewModelRecordsAndClearsNetworkRecentUrlsThroughRepository() {
        val source = sourceText("HomeViewModel.kt")

        assertTrue(source.contains("fun recordNetworkRecentUrl(normalizedUrl: String, title: String)"))
        assertTrue(source.contains("viewModelScope.launch { repository.recordNetworkRecentUrl(normalizedUrl, title) }"))
        assertTrue(source.contains("fun clearNetworkRecentUrls()"))
        assertTrue(source.contains("viewModelScope.launch { repository.clearNetworkRecentUrls() }"))
    }

    @Test
    fun repositoryUpsertsRecentUrlWithRedactedDisplayUrl() {
        val source = repositoryText()

        assertTrue(source.contains("private val networkRecentItemDao: NetworkRecentItemDao"))
        assertTrue(source.contains("fun getNetworkRecentUrls(): Flow<List<NetworkRecentItemEntity>>"))
        assertTrue(source.contains("suspend fun recordNetworkRecentUrl(normalizedUrl: String, title: String)"))
        assertTrue(source.contains("NetworkRecentUrlPolicy.displayUrlFor(normalizedUrl)"))
        assertTrue(source.contains("networkRecentItemDao.getByNormalizedUrl(normalizedUrl)"))
        assertTrue(source.contains("networkRecentItemDao.upsert("))
        assertTrue(source.contains("suspend fun clearNetworkRecentUrls() = networkRecentItemDao.deleteAll()"))
    }

    private fun sourceText(name: String): String {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "home", name)
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }

    private fun repositoryText(): String {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "data", "repository", "VideoRepository.kt")
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
