package com.example.openvideo.ui.series

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesListFragmentSourceTest {

    @Test
    fun listFragmentCollectsSeriesRowsAndNavigatesToDetailOnly() {
        val source = sourceText("SeriesListFragment.kt")

        assertTrue(source.contains("@AndroidEntryPoint"))
        assertTrue(source.contains("class SeriesListFragment : Fragment()"))
        assertTrue(source.contains("private val viewModel: SeriesListViewModel by viewModels()"))
        assertTrue(source.contains("inflater.inflate(R.layout.fragment_series_list, container, false)"))
        assertTrue(source.contains("view.findViewById<TextView>(R.id.tv_title).setText(R.string.series_list_title)"))
        assertTrue(source.contains("adapter = SeriesAdapter { series ->"))
        assertTrue(source.contains("SeriesDetailFragment.newInstance(series.seriesId, series.title)"))
        assertTrue(source.contains(".replace(R.id.fragment_container,"))
        assertTrue(source.contains(".addToBackStack(\"series:${'$'}{series.seriesId}\")"))
        assertTrue(source.contains("viewModel.series.collect { series ->"))
        assertTrue(source.contains("adapter.submitList(series)"))
        assertTrue(source.contains("emptyView.visibility = if (series.isEmpty()) View.VISIBLE else View.GONE"))
        assertTrue(source.contains("recyclerView.visibility = if (series.isEmpty()) View.GONE else View.VISIBLE"))
        assertFalse(source.contains("PlayerActivity"))
        assertFalse(source.contains("BottomNavigationView"))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "series", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
