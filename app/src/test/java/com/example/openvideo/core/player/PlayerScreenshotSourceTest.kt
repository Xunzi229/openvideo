package com.example.openvideo.core.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerScreenshotSourceTest {

    @Test
    fun screenshotsSupportTextureViewRenderer() {
        val managerSource = String(Files.readAllBytes(playerManagerSource()))
        val activitySource = String(Files.readAllBytes(playerActivitySource()))

        assertTrue(
            "PlayerManager screenshot API should accept a View so both TextureView and SurfaceView renderers are supported",
            managerSource.contains("fun takeScreenshot(videoView: android.view.View")
        )
        assertTrue(
            "Screenshot capture should use TextureView.bitmap when PlayerView renders through a TextureView",
            managerSource.contains("is TextureView") && managerSource.contains("videoView.bitmap")
        )
        assertTrue(
            "Screenshot capture should keep PixelCopy for SurfaceView fallback",
            managerSource.contains("is SurfaceView") && managerSource.contains("PixelCopy.request")
        )
        assertTrue(
            "PlayerActivity should pass the current video renderer view to screenshot capture",
            activitySource.contains("val videoView = videoRenderView()")
                && activitySource.contains("playerManager.takeScreenshot(videoView)")
        )
    }

    private fun playerManagerSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "player",
            "PlayerManager.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
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
