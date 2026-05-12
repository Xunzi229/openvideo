package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerActivityStartupSourceTest {

    @Test
    fun controlsAreAttachedAfterPlayerIsInitialized() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val onCreate = source.substringAfter("override fun onCreate(savedInstanceState: Bundle?) {")
            .substringBefore("\n    private fun applyPlayerSettings()")

        assertTrue(
            "setupControls should run after initialize so player listeners attach to the active player",
            onCreate.indexOf("viewModel.initialize(") < onCreate.indexOf("setupControls()")
        )
    }

    @Test
    fun playerKeepsBlackScrimUntilFirstFrameWhenOpeningOrSwitchingVideo() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(source.contains("private lateinit var firstFrameScrim: View"))
        assertTrue(source.contains("R.id.player_first_frame_scrim"))
        assertTrue(source.contains("private fun showFirstFrameScrim()"))
        assertTrue(source.contains("private fun hideFirstFrameScrim()"))
        assertTrue(source.contains("override fun onRenderedFirstFrame()"))
        assertTrue(source.contains("hideFirstFrameScrim()"))
        assertTrue(source.contains("showFirstFrameScrim()") && source.contains("viewModel.switchToVideo("))

        sequenceOf(playerLayoutSource("layout"), playerLayoutSource("layout-land")).forEach { layout ->
            val layoutSource = String(Files.readAllBytes(layout))
            assertTrue(layoutSource.contains("android:id=\"@+id/player_first_frame_scrim\""))
            assertTrue(layoutSource.contains("android:background=\"@color/player_bg\""))
            assertTrue(
                "Scrim should sit above PlayerView and below gesture/controls to cover TextureView first-frame flashes",
                layoutSource.indexOf("@+id/player_view") < layoutSource.indexOf("@+id/player_first_frame_scrim") &&
                    layoutSource.indexOf("@+id/player_first_frame_scrim") < layoutSource.indexOf("@+id/gesture_overlay")
            )
        }
    }

    private fun playerActivitySource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerActivity.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerLayoutSource(folder: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            folder,
            "activity_player.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
