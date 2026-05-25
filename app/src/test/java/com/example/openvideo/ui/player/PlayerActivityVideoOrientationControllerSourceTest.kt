package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerActivityVideoOrientationControllerSourceTest {

    @Test
    fun playerActivityDelegatesVideoOrientationToController() {
        val activity = source("PlayerActivity.kt")
        val controller = source("PlayerVideoOrientationController.kt")

        assertTrue(activity.contains("private val videoOrientation by lazy"))
        assertTrue(activity.contains("videoOrientation.apply("))
        assertTrue(activity.contains("videoOrientation.applyInitial()"))
        assertTrue(activity.contains("videoOrientation.preApplyForItem(item)"))

        assertTrue(controller.contains("PlayerVideoOrientationApplyPolicy.shouldApply("))
        assertTrue(controller.contains("PlayerVideoLayoutPolicy.orientationForVideo("))
        assertTrue(controller.contains("PlayerOrientationPolicy.initialOrientationForVideo("))
        assertTrue(controller.contains("intent.sessionVideoQueue()"))

        assertFalse(activity.contains("private fun resolveInitialVideoDimensions()"))
        assertFalse(activity.contains("PlayerVideoLayoutPolicy.orientationForVideo("))
        assertFalse(activity.contains("PlayerOrientationPolicy.initialOrientationForVideo("))
    }

    private fun source(fileName: String): String {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            fileName
        )
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
