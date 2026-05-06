package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerControlsLayoutTest {

    @Test
    fun portraitToolRowDoesNotAddExtraStartPadding() {
        val source = String(Files.readAllBytes(playerControlsSource()))

        assertFalse(source.contains("""android:paddingStart="68dp""""))
    }

    private fun playerControlsSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            "layout",
            "player_controls.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
