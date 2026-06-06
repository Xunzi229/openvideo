package com.example.openvideo.ui.series

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesAdapterSourceTest {

    @Test
    fun adapterBindsSeriesTitleFolderAndClick() {
        val source = sourceText("SeriesAdapter.kt")

        assertTrue(source.contains("class SeriesAdapter("))
        assertTrue(source.contains("private val onClick: (SeriesUiState) -> Unit"))
        assertTrue(source.contains("ListAdapter<SeriesUiState"))
        assertTrue(source.contains("R.layout.item_series"))
        assertTrue(source.contains("a.seriesId == b.seriesId"))
        assertTrue(source.contains("view.setOnClickListener"))
        assertTrue(source.contains("onClick(getItem(pos))"))
        assertTrue(source.contains("holder.title.text = item.title"))
        assertTrue(source.contains("holder.folder.text = item.folderPath"))
        assertTrue(source.contains("Glide.with(holder.poster)"))
        assertTrue(source.contains(".load(posterModel(item.posterPath))"))
        assertTrue(source.contains(".placeholder(R.drawable.ic_movie)"))
        assertTrue(source.contains(".fallback(R.drawable.ic_movie)"))
        assertTrue(source.contains(".error(R.drawable.ic_movie)"))
        assertTrue(source.contains("Glide.with(holder.poster.context.applicationContext).clear(holder.poster)"))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "series", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
