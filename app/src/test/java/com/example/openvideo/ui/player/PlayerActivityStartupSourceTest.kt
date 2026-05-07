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
