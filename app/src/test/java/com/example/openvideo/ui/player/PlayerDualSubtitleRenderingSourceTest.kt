package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerDualSubtitleRenderingSourceTest {

    @Test
    fun activityLayoutsExposeNonOverlappingPrimaryAndSecondarySubtitleViews() {
        sequenceOf(playerLayoutSource("layout"), playerLayoutSource("layout-land")).forEach { path ->
            val source = String(Files.readAllBytes(path))

            assertTrue(source.contains("android:id=\"@+id/subtitle_stack\""))
            assertTrue(source.contains("android:id=\"@+id/tv_subtitle\""))
            assertTrue(source.contains("android:id=\"@+id/tv_subtitle_secondary\""))
            assertTrue(source.indexOf("@+id/tv_subtitle") < source.indexOf("@+id/tv_subtitle_secondary"))
        }
    }

    @Test
    fun playerActivityWiresSecondarySubtitleViewToTickAndDisplayControllers() {
        val source = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(source.contains("private lateinit var tvSubtitleSecondary: TextView"))
        assertTrue(source.contains("tvSubtitleSecondary = findViewById(R.id.tv_subtitle_secondary)"))
        assertTrue(source.contains("secondarySubtitleProvider = { tvSubtitleSecondary }"))
    }

    @Test
    fun playbackTickRendersPrimaryAndSecondarySubtitleSeparately() {
        val source = String(Files.readAllBytes(playerPlaybackTickControllerSource()))

        assertTrue(source.contains("private val secondarySubtitleProvider: () -> TextView"))
        assertTrue(source.contains("viewModel.getCurrentDualSubtitle()"))
        assertTrue(source.contains("val secondaryPresentation = PlayerSubtitlePresentationPolicy.present("))
        assertTrue(source.contains("secondarySubtitle.text = secondaryPresentation.text"))
        assertTrue(source.contains("secondarySubtitle.visibility = if (secondaryPresentation.visible) View.VISIBLE else View.GONE"))
        assertTrue(source.contains("PlayerSubtitleCueStylePolicy.apply("))
        assertTrue(source.contains("style = dualSubtitleText?.primaryStyle"))
        assertTrue(source.contains("style = dualSubtitleText?.secondaryStyle"))
    }

    @Test
    fun displaySettingsStyleBothSubtitleViews() {
        val source = String(Files.readAllBytes(playerDisplayControllerSource()))

        assertTrue(source.contains("private val secondarySubtitleProvider: () -> TextView"))
        assertTrue(source.contains("subtitle = subtitleProvider()"))
        assertTrue(source.contains("subtitle = secondarySubtitleProvider()"))
        assertTrue(source.contains("sizeSp = playerPrefs.subtitleSize"))
        assertTrue(source.contains("sizeSp = playerPrefs.secondarySubtitleSize"))
    }

    private fun playerActivitySource(): Path = kotlinSource("PlayerActivity.kt")

    private fun playerPlaybackTickControllerSource(): Path = kotlinSource("PlayerPlaybackTickController.kt")

    private fun playerDisplayControllerSource(): Path = kotlinSource("PlayerDisplayController.kt")

    private fun kotlinSource(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            name
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerLayoutSource(folder: String): Path {
        val relativePath = Paths.get("src", "main", "res", folder, "activity_player.xml")
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
