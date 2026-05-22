package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerContentFrameSourceTest {

    @Test
    fun contentFramePolicyFileExistsAsWaveThreeFoundation() {
        val path = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerContentFramePolicy.kt"
        )
        val resolved = sequenceOf(path, Paths.get("app").resolve(path)).first(Files::exists)
        val source = String(Files.readAllBytes(resolved))
        assertTrue(source.contains("object PlayerContentFramePolicy"))
        assertTrue(source.contains("fun fittedVideoRect"))
        assertTrue(source.contains("fun contentFrameInViewport"))
        assertTrue(source.contains("fun transformToFillViewport"))
        assertTrue(source.contains("fun allowsContentFrameAdjustment"))
    }

    @Test
    fun playerActivityDelegatesContentFrameTransformToApplyPolicy() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val displayBlock = source.substringAfter("private fun applyDisplaySettings() {")
            .substringBefore("\n    private fun initBrightnessAndVolume()")
        val transformBlock = source.substringAfter("private fun applyPlayerContentFrameTransform(")
            .substringBefore("\n    @OptIn(UnstableApi::class)\n    private fun videoRenderView()")

        assertTrue(displayBlock.contains("applyPlayerContentFrameTransform()"))
        assertTrue(transformBlock.contains("PlayerContentFrameApplyPolicy.resolveTransformWithManualZoom"))
        assertTrue(transformBlock.contains("PlayerVideoLayoutPolicy.displayFrameSize"))
        assertFalse(
            "Transform math must stay in policy, not Activity.",
            transformBlock.contains("PlayerContentFramePolicy.fittedVideoRect(")
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
