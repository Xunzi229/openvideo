package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerDarkChromeSourceTest {

    @Test
    fun playerChromeDoesNotUseThemeDependentAppColors() {
        val forbidden = listOf(
            "@color/ov_bg_base",
            "@color/ov_bg_elevated",
            "@color/ov_bg_elevated_strong",
            "@color/ov_bg_overlay",
            "@color/ov_text_primary",
            "@color/ov_text_secondary",
            "@color/ov_text_tertiary"
        )

        playerChromeSources().forEach { path ->
            val source = String(Files.readAllBytes(path))
            forbidden.forEach { token ->
                assertFalse("${path.fileName} should not use theme-dependent $token", source.contains(token))
            }
        }
    }

    @Test
    fun playerDefinesFixedDarkChromeColors() {
        val colors = String(Files.readAllBytes(resource("values", "colors.xml")))

        listOf(
            """<color name="player_bg">#FF000000</color>""",
            """<color name="player_panel_bg">#D1080A12</color>""",
            """<color name="player_panel_border">#14FFFFFF</color>""",
            """<color name="player_icon">#B8FFFFFF</color>""",
            """<color name="player_title_primary">#F2FFFFFF</color>""",
            """<color name="player_title_normal">#D1FFFFFF</color>""",
            """<color name="player_text_secondary">#73FFFFFF</color>""",
            """<color name="player_accent">#FF4F8CFF</color>"""
        ).forEach { expected ->
            assertTrue("Missing fixed player color $expected", colors.contains(expected))
        }
    }

    @Test
    fun playerVideoListUsesGlassPanelShape() {
        val source = String(Files.readAllBytes(resource("drawable", "bg_player_video_list_panel.xml")))

        assertTrue(source.contains("""android:color="@color/player_panel_bg""""))
        assertTrue(source.contains("""android:topRightRadius="24dp""""))
        assertTrue(source.contains("""android:bottomRightRadius="24dp""""))
        assertTrue(source.contains("""android:color="@color/player_panel_border""""))
    }

    private fun playerChromeSources(): List<Path> = listOf(
        resource("layout", "activity_player.xml"),
        resource("layout-land", "activity_player.xml"),
        resource("layout", "player_controls.xml"),
        resource("layout-land", "player_controls.xml"),
        resource("layout", "dialog_player_video_list.xml"),
        resource("layout", "item_player_session_video.xml"),
        resource("drawable", "bg_player_control_bar.xml"),
        resource("drawable", "bg_elevated_card_strong.xml"),
        resource("drawable", "bg_player_lock_button.xml")
    )

    private fun resource(dir: String, fileName: String): Path {
        val relativePath = Paths.get("src", "main", "res", dir, fileName)
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
