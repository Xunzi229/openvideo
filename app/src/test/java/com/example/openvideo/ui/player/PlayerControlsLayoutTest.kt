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

    @Test
    fun playbackButtonsSitBelowProgressInsteadOfCenterOverlay() {
        listOf("layout", "layout-land").forEach { layoutDir ->
            val source = String(Files.readAllBytes(playerControlsSource(layoutDir)))
            val bottomPanel = source.substringAfter("""android:id="@+id/bottom_panel"""")
            val afterProgress = bottomPanel.substringAfter("""android:id="@+id/progress_row"""")
            val beforeToolRow = afterProgress.substringBefore("""android:id="@+id/tool_row"""")

            assertFalse(
                "$layoutDir should not define a center overlay play button",
                source.contains("@+id/btn_play_center")
            )
            assertTrue("$layoutDir should keep previous below the progress row", beforeToolRow.contains("@+id/btn_prev"))
            assertTrue("$layoutDir should keep play below the progress row", beforeToolRow.contains("@+id/btn_play"))
            assertTrue("$layoutDir should keep next below the progress row", beforeToolRow.contains("@+id/btn_next"))
        }
    }

    private fun boundControlIds(): List<String> = listOf(
        "controls_container",
        "top_bar",
        "top_scrim",
        "bottom_scrim",
        "bottom_panel",
        "playback_controls",
        "tool_row",
        "btn_play",
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
