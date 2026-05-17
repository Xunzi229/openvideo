package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSubtitleLoadSourceTest {

    @Test
    fun playerActivityDelegatesSubtitleLoadToCoordinatorAndApplyPolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("private fun loadSubtitlesAsync(uriString: String, videoPath: String, showToast: Boolean = false) {")
            .substringBefore("\n    private fun initViews()")

        assertTrue(block.contains("PlayerSubtitleLoadCoordinator.load("))
        assertTrue(block.contains("PlayerSubtitleLoadApplyPolicy.afterLoad("))
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
}
