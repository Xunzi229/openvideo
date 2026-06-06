package com.example.openvideo.ui.series

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeriesDetailLayoutSourceTest {

    @Test
    fun detailLayoutProvidesHeaderListAndEmptyStateIds() {
        val layout = layoutText("fragment_series_detail.xml")

        assertTrue(layout.contains("@+id/header"))
        assertTrue(layout.contains("@+id/btn_back"))
        assertTrue(layout.contains("@+id/tv_title"))
        assertTrue(layout.contains("@+id/recycler_episodes"))
        assertTrue(layout.contains("@+id/tv_empty"))
        assertTrue(layout.contains("@string/series_detail_empty"))
        assertTrue(layout.contains("@drawable/bg_app_root"))
    }

    @Test
    fun episodeItemLayoutProvidesStableTextIds() {
        val layout = layoutText("item_series_episode.xml")

        assertTrue(layout.contains("@+id/tv_episode_number"))
        assertTrue(layout.contains("@+id/tv_episode_title"))
        assertTrue(layout.contains("@+id/tv_episode_meta"))
        assertTrue(layout.contains("@drawable/bg_elevated_card_strong"))
        assertTrue(layout.contains("@color/ov_text_primary"))
        assertTrue(layout.contains("@color/ov_text_secondary"))
    }

    private fun layoutText(name: String): String =
        layoutFile(name)?.let { String(Files.readAllBytes(it)) }.orEmpty()

    private fun layoutFile(name: String): Path? {
        val relativePath = Paths.get("src", "main", "res", "layout", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
    }
}
