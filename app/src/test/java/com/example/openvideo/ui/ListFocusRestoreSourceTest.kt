package com.example.openvideo.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ListFocusRestoreSourceTest {

    @Test
    fun homeCategoryListsRecordAndRestoreFocusedVideoAfterPlayerReturn() {
        val fragment = sourceText("home", "HomeFragment.kt")
        val adapter = sourceText("home", "VideoGridAdapter.kt")

        assertTrue(adapter.contains("private val onFocusChanged: (VideoItem) -> Unit = {}"))
        assertTrue(adapter.contains("if (hasFocus && pos != RecyclerView.NO_POSITION) onFocusChanged(getItem(pos))"))
        assertTrue(fragment.contains("private val lastFocusedHomeVideoIds = mutableMapOf<HomeCategory, Long>()"))
        assertTrue(fragment.contains("private val pendingHomeVideoFocusRestoreIds = mutableMapOf<HomeCategory, Long>()"))
        assertTrue(fragment.contains("onFocusChanged = { video -> lastFocusedHomeVideoIds[category] = video.id }"))
        assertTrue(fragment.contains("restoreHomeVideoFocusIfNeeded(category, list)"))
        assertTrue(fragment.contains("lastFocusedHomeVideoIds[category]?.let { pendingHomeVideoFocusRestoreIds[category] = it }"))
        assertTrue(fragment.contains("private fun restoreHomeVideoFocusIfNeeded(category: HomeCategory, list: List<VideoItem>)"))
        assertTrue(fragment.contains("recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()"))
    }

    @Test
    fun localFolderListRecordsAndRestoresFocusedFolderAfterBackStackReturn() {
        val fragment = sourceText("local", "LocalFolderFragment.kt")
        val adapter = sourceText("local", "VideoFolderAdapter.kt")

        assertTrue(adapter.contains("private val onFocusChanged: (VideoFolder) -> Unit = {}"))
        assertTrue(adapter.contains("view.setOnFocusChangeListener { _, hasFocus ->"))
        assertTrue(adapter.contains("if (hasFocus && position != RecyclerView.NO_POSITION) onFocusChanged(getItem(position))"))
        assertTrue(fragment.contains("private var lastFocusedFolderKey: String? = null"))
        assertTrue(fragment.contains("private var pendingFolderFocusRestoreKey: String? = null"))
        assertTrue(fragment.contains("onFocusChanged = { folder -> lastFocusedFolderKey = folder.key }"))
        assertTrue(fragment.contains("adapter.submitList(folders) { restoreFolderFocusIfNeeded(folders) }"))
        assertTrue(fragment.contains("pendingFolderFocusRestoreKey = lastFocusedFolderKey"))
        assertTrue(fragment.contains("private fun restoreFolderFocusIfNeeded(folders: List<VideoFolder>)"))
        assertTrue(fragment.contains("recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()"))
    }

    @Test
    fun seriesListRecordsAndRestoresFocusedSeriesAfterBackStackReturn() {
        val fragment = sourceText("series", "SeriesListFragment.kt")
        val adapter = sourceText("series", "SeriesAdapter.kt")

        assertTrue(adapter.contains("private val onFocusChanged: (SeriesUiState) -> Unit = {}"))
        assertTrue(adapter.contains("view.setOnFocusChangeListener { _, hasFocus ->"))
        assertTrue(adapter.contains("if (hasFocus && pos != RecyclerView.NO_POSITION) onFocusChanged(getItem(pos))"))
        assertTrue(fragment.contains("private var lastFocusedSeriesId: Long? = null"))
        assertTrue(fragment.contains("private var pendingSeriesFocusRestoreId: Long? = null"))
        assertTrue(fragment.contains("onFocusChanged = { series -> lastFocusedSeriesId = series.seriesId }"))
        assertTrue(fragment.contains("adapter.submitList(series) { restoreSeriesFocusIfNeeded(series) }"))
        assertTrue(fragment.contains("pendingSeriesFocusRestoreId = lastFocusedSeriesId"))
        assertTrue(fragment.contains("private fun restoreSeriesFocusIfNeeded(series: List<SeriesUiState>)"))
        assertTrue(fragment.contains("recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()"))
    }

    @Test
    fun folderVideoListRecordsAndRestoresFocusedVideoAfterPlayerReturn() {
        val fragment = sourceText("local", "FolderVideosFragment.kt")
        val adapter = sourceText("home", "VideoGridAdapter.kt")

        assertTrue(adapter.contains("private val onFocusChanged: (VideoItem) -> Unit = {}"))
        assertTrue(adapter.contains("view.setOnFocusChangeListener { _, hasFocus ->"))
        assertTrue(adapter.contains("if (hasFocus && pos != RecyclerView.NO_POSITION) onFocusChanged(getItem(pos))"))
        assertTrue(fragment.contains("private var lastFocusedVideoId: Long? = null"))
        assertTrue(fragment.contains("private var pendingVideoFocusRestoreId: Long? = null"))
        assertTrue(fragment.contains("onFocusChanged = { video -> lastFocusedVideoId = video.id }"))
        assertTrue(fragment.contains("adapter.submitList(folderVideos) { restoreVideoFocusIfNeeded(folderVideos) }"))
        assertTrue(fragment.contains("pendingVideoFocusRestoreId = lastFocusedVideoId"))
        assertTrue(fragment.contains("private fun restoreVideoFocusIfNeeded(videos: List<VideoItem>)"))
        assertTrue(fragment.contains("recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()"))
    }

    @Test
    fun seriesDetailRecordsAndRestoresFocusedEpisodeAfterPlayerReturn() {
        val fragment = sourceText("series", "SeriesDetailFragment.kt")
        val adapter = sourceText("series", "SeriesEpisodeAdapter.kt")

        assertTrue(adapter.contains("private val onFocusChanged: (SeriesEpisodeUiState) -> Unit = {}"))
        assertTrue(adapter.contains("view.setOnFocusChangeListener { _, hasFocus ->"))
        assertTrue(adapter.contains("if (hasFocus && pos != RecyclerView.NO_POSITION) onFocusChanged(getItem(pos))"))
        assertTrue(fragment.contains("private var lastFocusedEpisodeId: Long? = null"))
        assertTrue(fragment.contains("private var pendingEpisodeFocusRestoreId: Long? = null"))
        assertTrue(fragment.contains("onFocusChanged = { episode -> lastFocusedEpisodeId = episode.episodeId }"))
        assertTrue(fragment.contains("adapter.submitList(episodes) { restoreEpisodeFocusIfNeeded(episodes) }"))
        assertTrue(fragment.contains("pendingEpisodeFocusRestoreId = lastFocusedEpisodeId"))
        assertTrue(fragment.contains("private fun restoreEpisodeFocusIfNeeded(episodes: List<SeriesEpisodeUiState>)"))
        assertTrue(fragment.contains("recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()"))
    }

    private fun sourceText(packageName: String, name: String): String =
        sourceFile(packageName, name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun sourceFile(packageName: String, name: String): Path? {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            packageName,
            name
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
