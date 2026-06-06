package com.example.openvideo.ui.series

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesListLayoutSourceTest {

    @Test
    fun listLayoutProvidesTitleListAndEmptyStateIds() {
        val layout = layoutText("fragment_series_list.xml")

        assertTrue(layout.contains("@+id/header"))
        assertTrue(layout.contains("@+id/tv_title"))
        assertTrue(layout.contains("@+id/recycler_series"))
        assertTrue(layout.contains("@+id/tv_empty"))
        assertTrue(layout.contains("@string/series_list_empty"))
        assertTrue(layout.contains("@drawable/bg_app_root"))
    }

    @Test
    fun seriesItemLayoutProvidesPosterTitleFolderAndChevronIds() {
        val layout = layoutText("item_series.xml")

        assertTrue(layout.contains("@+id/iv_poster"))
        assertTrue(layout.contains("@+id/tv_series_title"))
        assertTrue(layout.contains("@+id/tv_series_folder"))
        assertTrue(layout.contains("@+id/iv_chevron"))
        assertTrue(layout.contains("@drawable/bg_elevated_card_strong"))
        assertTrue(layout.contains("@drawable/ic_movie"))
        assertTrue(layout.contains("@drawable/ic_arrow_up"))
    }

    private fun layoutText(name: String): String =
        layoutFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun layoutFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "res", "layout", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
