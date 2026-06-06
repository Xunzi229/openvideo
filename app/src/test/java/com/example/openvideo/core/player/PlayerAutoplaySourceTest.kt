package com.example.openvideo.core.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerAutoplaySourceTest {

    @Test
    fun settingMediaUriRequestsAutoplayBeforePrepare() {
        val source = String(Files.readAllBytes(playerManagerSource()))
        val method = source.substringAfter("fun setMediaUri(uri: Uri, requestHeaders: Map<String, String> = emptyMap()) {")
            .substringBefore("\n    }")

        val autoplayIndex = method.indexOf("playWhenReady = true")
        val prepareIndex = method.indexOf("prepare()")

        assertTrue("setMediaUri should request autoplay", autoplayIndex >= 0)
        assertTrue(
            "setMediaUri should set playWhenReady before prepare",
            autoplayIndex < prepareIndex
        )
    }

    @Test
    fun videoAdjustmentsAreSkippedWhenValuesHaveNotChanged() {
        val source = String(Files.readAllBytes(playerVideoEffectsControllerSource()))
        val managerSource = String(Files.readAllBytes(playerManagerSource()))
        val method = source.substringAfter("fun applyVideoAdjustments(")
            .substringBefore("\n    private fun saturationMatrix")

        assertTrue(
            "Video adjustments should avoid repeatedly calling setVideoEffects for identical values.",
            source.contains("lastVideoAdjustments")
                && method.contains("val nextAdjustments = VideoAdjustments(")
                && method.contains("if (lastVideoAdjustments == nextAdjustments) return")
                && method.contains("lastVideoAdjustments = nextAdjustments")
        )
        assertTrue(managerSource.contains("fun applyVideoAdjustments("))
        assertTrue(managerSource.contains("videoEffects.applyVideoAdjustments(brightness, contrast, saturation)"))
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

    private fun playerVideoEffectsControllerSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "player",
            "PlayerVideoEffectsController.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
