package com.example.openvideo.ui.history

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HistoryContinueWatchingLabelsSourceTest {

    @Test
    fun labelsAreLoadedFromStringResources() {
        val source = String(Files.readAllBytes(labelsSource()))

        assertTrue(source.contains("R.string.history_continue_missing_file"))
        assertTrue(source.contains("R.string.history_continue_completed"))
        assertTrue(source.contains("R.string.history_continue_just_now"))
        assertTrue(source.contains("R.string.history_continue_progress_percent"))
    }

    @Test
    fun homeViewModelUsesLocalizedContinueWatchingLabels() {
        val source = String(Files.readAllBytes(homeViewModelSource()))

        assertTrue(source.contains("HistoryContinueWatchingLabels.from(context.applicationContext)"))
        assertTrue(source.contains("labels = continueWatchingLabels"))
    }

    @Test
    fun historyFragmentUsesLocalizedContinueWatchingLabels() {
        val source = String(Files.readAllBytes(historyFragmentSource()))

        assertTrue(source.contains("HistoryContinueWatchingLabels.from(requireContext())"))
    }

    private fun labelsSource(): Path = moduleSource(
        "ui", "history", "HistoryContinueWatchingLabels.kt"
    )

    private fun homeViewModelSource(): Path = moduleSource(
        "ui", "home", "HomeViewModel.kt"
    )

    private fun historyFragmentSource(): Path = moduleSource(
        "ui", "history", "HistoryFragment.kt"
    )

    private fun moduleSource(vararg segments: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", *segments)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
