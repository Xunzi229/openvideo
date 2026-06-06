package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerPipSourceTest {

    @Test
    fun playerActivityDelegatesPipEntryDecisionToPolicy() {
        val activitySource = String(Files.readAllBytes(playerActivitySource()))
        val source = String(Files.readAllBytes(playerPipControllerSource()))
        val pipBlock = source.substringAfter("fun enterIfSupported() {")
            .substringBefore("\n    fun onPictureInPictureModeChanged")
        val enterBlock = source.substringAfter("private fun enterPictureInPicture(decision: PlayerPipDecision) {")
            .substringBefore("\n    fun onPictureInPictureModeChanged")

        assertTrue(activitySource.contains("private fun enterPipModeIfSupported() = playerPip.enterIfSupported()"))
        assertTrue(pipBlock.contains("PlayerPipPolicy.enterDecision("))
        assertTrue(pipBlock.contains("enterPictureInPicture(decision)"))
        assertTrue(enterBlock.contains("PictureInPictureParams.Builder()"))
        assertTrue(enterBlock.contains(".setAspectRatio("))
        assertTrue(enterBlock.contains("decision.aspectRatio?.toRational()"))
        assertTrue(enterBlock.contains("PlayerPipPolicy.fallbackRational()"))
        assertFalse(
            "PiP fallback must not inline Rational(16, 9) in Activity.",
            pipBlock.contains("Rational(16, 9)")
        )
        assertFalse(
            "Activity must not declare a private PlayerPipAspectRatio.toRational extension.",
            source.contains("private fun PlayerPipAspectRatio.toRational()")
        )
    }

    @Test
    fun platformPipApisStayBehindLintVisibleSdkGuards() {
        val source = String(Files.readAllBytes(playerPipControllerSource()))

        assertTrue(source.contains("import androidx.annotation.RequiresApi"))
        assertTrue(source.contains("if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {"))
        assertTrue(source.contains("enterPictureInPicture(decision)"))
        assertTrue(source.contains("@RequiresApi(Build.VERSION_CODES.O)"))
        assertTrue(source.contains("private fun enterPictureInPicture(decision: PlayerPipDecision)"))
        assertTrue(source.contains("if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {"))
        assertTrue(source.contains("activity.isInPictureInPictureMode"))
    }

    private fun playerActivitySource(): Path {
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerPipControllerSource(): Path {
        return kotlinSource("PlayerPipController.kt")
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
