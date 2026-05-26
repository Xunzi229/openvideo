package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerVideoZoomSourceTest {

    @Test
    fun playerActivityDelegatesManualZoomToPolicies() {
        val gestureSource = String(Files.readAllBytes(playerGestureControllerSource()))
        val controllerSource = String(Files.readAllBytes(contentFrameTransformControllerSource()))
        val gestureBlock = gestureSource
        val transformBlock = controllerSource.substringAfter("fun applyTransform(")
            .substringBefore("\n    fun sourceSize(")

        assertTrue(gestureBlock.contains("ScaleGestureDetector"))
        assertTrue(gestureBlock.contains("PlayerVideoZoomPolicy.applyScaleFactor"))
        assertTrue(gestureBlock.contains("pinchZoomDetector.isInProgress"))
        assertTrue(gestureBlock.contains("PlayerVideoZoomGesturePolicy.interceptsSingleFingerGestures"))
        assertTrue(gestureBlock.contains("PlayerVideoZoomGesturePolicy.doubleTapResetsZoom"))
        assertTrue(transformBlock.contains("PlayerContentFrameApplyPolicy.resolveTransformWithManualZoom"))
    }

    @Test
    fun applyPolicyComposesManualZoomInPolicyLayer() {
        val source = String(Files.readAllBytes(applyPolicySource()))
        assertTrue(source.contains("fun resolveTransformWithManualZoom"))
        assertTrue(source.contains("PlayerVideoZoomPolicy.composeTransform"))
    }

    private fun playerActivitySource(): Path = kotlinSource("PlayerActivity.kt")

    private fun playerGestureControllerSource(): Path =
        kotlinSource("PlayerGestureController.kt")

    private fun contentFrameTransformControllerSource(): Path =
        kotlinSource("PlayerContentFrameTransformController.kt")

    private fun applyPolicySource(): Path = kotlinSource("PlayerContentFrameApplyPolicy.kt")

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
