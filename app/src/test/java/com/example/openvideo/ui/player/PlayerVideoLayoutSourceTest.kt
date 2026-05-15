package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerVideoLayoutSourceTest {

    @Test
    fun playerActivityDelegatesRuntimeOrientationAndContentRatioToVideoLayoutPolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val listenerBlock = source.substringAfter("override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {")
            .substringBefore("\n            }")
        val orientationBlock = source.substringAfter("private fun applyVideoOrientation")
            .substringBefore("\n    private fun toggleScreenLock")
        val aspectRatioBlock = source.substringAfter("private fun applyPlayerContentAspectRatio() {")
            .substringBefore("\n    @OptIn(UnstableApi::class)\n    private fun videoRenderView()")

        assertTrue(listenerBlock.contains("videoSize.unappliedRotationDegrees"))
        assertTrue(listenerBlock.contains("videoSize.pixelWidthHeightRatio"))
        assertTrue(orientationBlock.contains("PlayerVideoLayoutPolicy"))
        assertTrue(aspectRatioBlock.contains("PlayerVideoLayoutPolicy"))
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
