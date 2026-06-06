package com.example.openvideo.ui.series

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesEpisodeAdapterSourceTest {

    @Test
    fun adapterBindsEpisodeNumberTitleAndMetaWithoutMutatingData() {
        val source = sourceText("SeriesEpisodeAdapter.kt")

        assertTrue(source.contains("class SeriesEpisodeAdapter("))
        assertTrue(source.contains("private val onClick: (SeriesEpisodeUiState) -> Unit"))
        assertTrue(source.contains(": ListAdapter<SeriesEpisodeUiState"))
        assertTrue(source.contains("R.layout.item_series_episode"))
        assertTrue(source.contains("override fun areItemsTheSame(a: SeriesEpisodeUiState, b: SeriesEpisodeUiState)"))
        assertTrue(source.contains("a.episodeId == b.episodeId"))
        assertTrue(source.contains("view.setOnClickListener"))
        assertTrue(source.contains("if (getItem(pos).isAvailable) onClick(getItem(pos))"))
        assertTrue(source.contains("holder.number.text = item.numberLabel"))
        assertTrue(source.contains("holder.title.text = item.displayTitle"))
        assertTrue(source.contains("holder.itemView.isEnabled = item.isAvailable"))
        assertTrue(source.contains("holder.itemView.alpha = if (item.isAvailable) 1f else 0.6f"))
        assertTrue(source.contains("val watchStatusLabel = SeriesEpisodeWatchStatusPolicy.label("))
        assertTrue(source.contains("isAvailable = item.isAvailable"))
        assertTrue(source.contains("SeriesEpisodeWatchStatusLabels.from(holder.itemView.context)"))
        assertTrue(source.contains("holder.meta.text = holder.itemView.context.getString("))
        assertTrue(source.contains("R.string.series_episode_meta"))
        assertTrue(source.contains("watchStatusLabel"))
        assertTrue(source.contains("item.confidence"))
        assertTrue(source.contains("item.rule"))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "series", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
