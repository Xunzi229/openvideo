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
        val orientationSource = String(Files.readAllBytes(playerVideoOrientationControllerSource()))
        val transformSource = String(Files.readAllBytes(contentFrameTransformControllerSource()))
        val aspectRatioBlock = transformSource.substringAfter("fun applyAspectRatio(")
            .substringBefore("\n    @OptIn(UnstableApi::class)")

        assertTrue(listenerBlock.contains("videoSize.unappliedRotationDegrees"))
        assertTrue(listenerBlock.contains("videoSize.pixelWidthHeightRatio"))
        assertTrue(orientationSource.contains("PlayerVideoLayoutPolicy"))
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

    private fun playerVideoOrientationControllerSource(): Path {
        return kotlinSource("PlayerVideoOrientationController.kt")
    }

    private fun contentFrameTransformControllerSource(): Path {
        return kotlinSource("PlayerContentFrameTransformController.kt")
    }

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
}
