package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoThumbnailLoadingSourceTest {

    @Test
    fun videoRowsExposeThumbnailLoadingOverlay() {
        val listLayout = String(Files.readAllBytes(layoutSource("item_video.xml")))
        val gridLayout = String(Files.readAllBytes(layoutSource("item_video_grid.xml")))
        val adapter = String(Files.readAllBytes(adapterSource()))

        assertTrue(listLayout.contains("@+id/thumbnail_loading"))
        assertTrue(gridLayout.contains("@+id/thumbnail_loading"))
        assertTrue(adapter.contains("thumbnailLoading"))
        assertTrue(adapter.contains("RequestListener"))
        assertTrue(adapter.contains("onLoadStarted"))
        assertTrue(adapter.contains("onLoadCleared"))
    }

    private fun layoutSource(name: String): Path = sourcePath("main", "res", "layout", name)

    private fun adapterSource(): Path = sourcePath(
        "main",
        "java",
        "com",
        "example",
        "openvideo",
        "ui",
        "home",
        "VideoGridAdapter.kt"
    )

    private fun sourcePath(vararg segments: String): Path {
        val relativePath = Paths.get("src", *segments)
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
