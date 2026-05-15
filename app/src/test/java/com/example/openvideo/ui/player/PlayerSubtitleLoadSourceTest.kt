package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSubtitleLoadSourceTest {

    @Test
    fun playerActivityDelegatesSubtitleLoadRoutingToPolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("private fun loadSubtitles(uriString: String, videoPath: String):")
            .substringBefore("\n    private fun initViews()")

        assertTrue(block.contains("PlayerSubtitleLoadPolicy.resolve("))
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
