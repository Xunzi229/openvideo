package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerControlsLayoutTest {

    @Test
    fun portraitToolRowDoesNotAddExtraStartPadding() {
        val source = String(Files.readAllBytes(playerControlsSource("layout")))

        assertFalse(source.contains("""android:paddingStart="68dp""""))
    }

    @Test
    fun landControlsDefineAllViewsBoundByPlayerActivity() {
        val source = String(Files.readAllBytes(playerControlsSource("layout-land")))

        boundControlIds().forEach { id ->
            assertTrue("layout-land/player_controls.xml missing $id", source.contains("@+id/$id"))
        }
    }

    private fun boundControlIds(): List<String> = listOf(
        "controls_container",
        "top_bar",
        "top_scrim",
        "bottom_scrim",
        "center_controls",
        "bottom_panel",
        "tool_row",
        "btn_play",
        "btn_play_center",
        "btn_prev",
        "btn_next",
        "btn_settings",
        "btn_screenshot",
        "btn_ab_loop",
        "btn_pip",
        "btn_lock",
        "btn_fullscreen",
        "btn_back",
        "seek_bar",
        "tv_current_time",
        "tv_total_time",
        "tv_title"
    )

    private fun playerControlsSource(layoutDir: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            layoutDir,
            "player_controls.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
