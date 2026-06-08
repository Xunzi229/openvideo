package com.example.openvideo.ui.series

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesDetailFragmentSourceTest {

    @Test
    fun detailFragmentCollectsEpisodeRowsAndStartsPlayerWithSeriesQueue() {
        val source = sourceText("SeriesDetailFragment.kt")

        assertTrue(source.contains("@AndroidEntryPoint"))
        assertTrue(source.contains("class SeriesDetailFragment : Fragment()"))
        assertTrue(source.contains("fun newInstance(seriesId: Long, seriesTitle: String): SeriesDetailFragment"))
        assertTrue(source.contains("""putLong("series_id", seriesId)"""))
        assertTrue(source.contains("""putString("series_title", seriesTitle)"""))
        assertTrue(source.contains("private val viewModel: SeriesDetailViewModel by viewModels()"))
        assertTrue(source.contains("inflater.inflate(R.layout.fragment_series_detail, container, false)"))
        assertTrue(source.contains("view.findViewById<TextView>(R.id.tv_title).text = seriesTitle"))
        assertTrue(source.contains("parentFragmentManager.popBackStack()"))
        assertTrue(source.contains("private var episodeSnapshot: List<SeriesEpisodeUiState> = emptyList()"))
        assertTrue(source.contains("adapter = SeriesEpisodeAdapter("))
        assertTrue(source.contains("onClick = { episode -> openEpisode(episode) }"))
        assertTrue(source.contains("onFocusChanged = { episode -> lastFocusedEpisodeId = episode.episodeId }"))
        assertTrue(source.contains("openEpisode(episode)"))
        assertTrue(source.contains("viewModel.getEpisodesForSeries(seriesId).collect { episodes ->"))
        assertTrue(source.contains("episodeSnapshot = episodes"))
        assertTrue(source.contains("adapter.submitList(episodes) { restoreEpisodeFocusIfNeeded(episodes) }"))
        assertTrue(source.contains("val hasEpisodes = episodes.isNotEmpty()"))
        assertTrue(source.contains("emptyView.visibility = if (hasEpisodes) View.GONE else View.VISIBLE"))
        assertTrue(source.contains("recyclerView.visibility = if (hasEpisodes) View.VISIBLE else View.GONE"))
        assertTrue(source.contains("emptyView.isFocusable = true"))
        assertTrue(source.contains("emptyView.nextFocusUpId = R.id.btn_back"))
        assertTrue(source.contains("private fun updateEpisodeFocusOrder(view: View, hasEpisodes: Boolean)"))
        assertTrue(source.contains("val contentFocusTargetId = if (hasEpisodes) R.id.recycler_episodes else R.id.tv_empty"))
        assertTrue(source.contains("view.findViewById<View>(R.id.btn_back).nextFocusDownId = contentFocusTargetId"))
        assertTrue(source.contains("private fun openEpisode(episode: SeriesEpisodeUiState)"))
        assertTrue(source.contains("val selectedVideo = episode.toVideoItem()"))
        assertTrue(source.contains("val queue = episodeSnapshot.filter { it.isAvailable }.map { it.toVideoItem() }"))
        assertTrue(source.contains("putSessionQueue(queue.ifEmpty { listOf(selectedVideo) })"))
        assertTrue(source.contains("""putExtra("video_uri", selectedVideo.uri.toString())"""))
        assertTrue(source.contains("""putExtra("video_title", selectedVideo.title)"""))
        assertTrue(source.contains("""putExtra("video_id", selectedVideo.id)"""))
        assertTrue(source.contains("""putExtra("video_path", selectedVideo.path)"""))
        assertTrue(source.contains("putExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, selectedVideo.width)"))
        assertTrue(source.contains("putExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, selectedVideo.height)"))
        assertFalse(source.contains("SeriesEpisodeDao"))
    }

    private fun sourceText(name: String): String =
        sourceFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "series", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
