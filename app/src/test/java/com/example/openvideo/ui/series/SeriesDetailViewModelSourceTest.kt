package com.example.openvideo.ui.series

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesDetailViewModelSourceTest {

    @Test
    fun detailViewModelUsesRepositorySeriesEpisodeFlowsOnly() {
        val source = sourceText("SeriesDetailViewModel.kt")

        assertTrue(source.contains("@HiltViewModel"))
        assertTrue(source.contains("class SeriesDetailViewModel @Inject constructor("))
        assertTrue(source.contains("private val repository: VideoRepository"))
        assertTrue(source.contains("fun getEpisodesForSeries(seriesId: Long): Flow<List<SeriesEpisodeUiState>>"))
        assertTrue(source.contains("repository.getPlayableEpisodesForSeries(seriesId)"))
        assertTrue(source.contains("episodes.map(SeriesEpisodeUiState::from)"))
        assertFalse(source.contains("SeriesEpisodeDao"))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "series", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
