package com.example.openvideo.ui.history

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HistoryFragmentSourceTest {

    @Test
    fun historyFragmentUsesContinueWatchingProjectionAndBindsStatusLabels() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("HistoryContinueWatchingPolicy.buildItems("))
        assertTrue(source.contains("adapter.submitList(items)"))
        assertTrue(source.contains("holder.duration.text = formatDuration(item.entity.duration)"))
        assertTrue(source.contains("holder.resolution.text = item.watchedTimeLabel"))
        assertTrue(source.contains("holder.size.text = item.progressLabel"))
        assertTrue(source.contains("holder.itemView.alpha = if (item.isAvailable) 1f else 0.6f"))
        assertTrue(source.contains("if (item.isAvailable) onClick(item)"))
        assertTrue(source.contains("private fun formatDuration(ms: Long): String"))
    }

    @Test
    fun historyFragmentHidesSortingControls() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("view.findViewById<View>(R.id.sort_row)?.visibility = View.GONE"))
    }

    private fun sourceFile(): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "history", "HistoryFragment.kt")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
