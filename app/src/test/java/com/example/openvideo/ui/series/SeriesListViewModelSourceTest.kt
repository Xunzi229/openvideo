package com.example.openvideo.ui.series

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesListViewModelSourceTest {

    @Test
    fun listViewModelExposesRepositorySeriesFlowAsUiState() {
        val source = sourceText("SeriesListViewModel.kt")

        assertTrue(source.contains("@HiltViewModel"))
        assertTrue(source.contains("class SeriesListViewModel @Inject constructor("))
        assertTrue(source.contains("private val repository: VideoRepository"))
        assertTrue(source.contains("val series: Flow<List<SeriesUiState>>"))
        assertTrue(source.contains("repository.getAllSeries()"))
        assertTrue(source.contains("items.map(SeriesUiState::from)"))
        assertFalse(source.contains("SeriesEpisodeDao"))
        assertFalse(source.contains("SeriesEpisodeUiState"))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "series", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
