package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerNetworkStatusSourceTest {

    @Test
    fun portraitAndLandscapeControlsExposeNetworkStatusLabel() {
        val portrait = resourceText("layout", "player_controls.xml")
        val landscape = resourceText("layout-land", "player_controls.xml")

        listOf(portrait, landscape).forEach { source ->
            assertTrue(source.contains("""android:id="@+id/tv_network_status""""))
            assertTrue(source.contains("""android:text="@string/player_network_status_buffering""""))
            assertTrue(source.contains("""android:visibility="gone""""))
        }
    }

    @Test
    fun activityBindsNetworkStatusLabelIntoEventController() {
        val source = playerSource("PlayerActivity.kt")

        assertTrue(source.contains("private lateinit var tvNetworkStatus: TextView"))
        assertTrue(source.contains("tvNetworkStatus = findViewById(R.id.tv_network_status)"))
        assertTrue(source.contains("onShowNetworkStatus = { labelRes ->"))
        assertTrue(source.contains("tvNetworkStatus.setText(labelRes)"))
        assertTrue(source.contains("onHideNetworkStatus = { tvNetworkStatus.visibility = View.GONE }"))
    }

    @Test
    fun eventControllerAppliesNetworkStatusPolicy() {
        val source = playerSource("PlayerEventController.kt")

        assertTrue(source.contains("onShowNetworkStatus: (Int) -> Unit"))
        assertTrue(source.contains("onHideNetworkStatus: () -> Unit"))
        assertTrue(source.contains("PlayerNetworkStatusPolicy.present("))
        assertTrue(source.contains("isNetworkUri = viewModel.currentVideoUri?.scheme in setOf(\"http\", \"https\", \"rtsp\")"))
        assertTrue(source.contains("isLive = viewModel.player?.isCurrentMediaItemLive == true"))
        assertTrue(source.contains("durationMs = viewModel.player?.duration ?: 0L"))
        assertTrue(source.contains("autoRetryPending = true"))
    }

    private fun playerSource(file: String): String = loadText(
        Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", file)
    )

    private fun resourceText(dir: String, file: String): String = loadText(
        Paths.get("src", "main", "res", dir, file)
    )

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
