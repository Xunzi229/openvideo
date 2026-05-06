package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoOptionsSheetTest {

    @Test
    fun playOptionInvokesCallerPlayCallback() {
        val source = String(Files.readAllBytes(sourceFile("VideoOptionsSheet.kt")))

        assertTrue(source.contains("private val onPlay: () -> Unit"))
        assertTrue(source.contains("onPlay()"))
    }

    @Test
    fun homeAndFolderMenusRoutePlayToPlayer() {
        val homeSource = String(Files.readAllBytes(sourceFile("HomeFragment.kt")))
        val folderSource = String(Files.readAllBytes(folderSourceFile()))

        assertTrue(homeSource.contains("onPlay = { openPlayer(video) }"))
        assertTrue(folderSource.contains("onPlay = { openPlayer(video) }"))
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "home",
            name
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun folderSourceFile(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "local",
            "FolderVideosFragment.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
