package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerExitFlashSourceTest {

    @Test
    fun playerExitHidesSurfaceWithoutSynchronouslyDetachingPlayerView() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val finishPlayer = source
            .substringAfter("private fun finishPlayer() {")
            .substringBefore("\n    private fun preparePlayerExitFrame()")
        val prepareExit = source
            .substringAfter("private fun preparePlayerExitFrame() {")
            .substringBefore("\n    private fun")

        assertTrue(
            "Player back button should go through the guarded finish path",
            source.contains("btnBack.setPlayerClickListener(PlayerLockedInteraction.BACK) { finishPlayer() }")
        )
        assertTrue(
            "System back should use the same guarded finish path",
            source.contains("onBackPressedDispatcher.addCallback")
                && source.contains("override fun handleOnBackPressed() {")
                && source.contains("finishPlayer()")
        )
        assertTrue(
            "Exit should prepare a non-black frame before calling finish",
            finishPlayer.indexOf("preparePlayerExitFrame()") in 0 until finishPlayer.indexOf("finish()")
        )
        assertFalse(
            "Exit preparation should avoid PlayerView.setPlayer(null); Media3 can block detaching SurfaceView and throw ExoTimeoutException on some devices",
            prepareExit.contains("playerView.player = null")
        )
        assertTrue(
            "Exit preparation should hide PlayerView so the SurfaceView layer cannot flash black",
            prepareExit.contains("playerView.visibility = View.INVISIBLE")
        )
        assertTrue(
            "Exit preparation should delegate backdrop choice to PlayerExitPolicy",
            prepareExit.contains("PlayerExitPolicy.exitFrameDecision(")
        )
        assertTrue(
            "Exit preparation should map APP_BASE backdrop to ov_bg_base",
            prepareExit.contains("PlayerExitBackdrop.APP_BASE") && prepareExit.contains("R.color.ov_bg_base")
        )
    }

    @Test
    fun playerLayoutsExposeRootForExitBackgroundSwap() {
        sequenceOf(playerLayoutSource("layout"), playerLayoutSource("layout-land")).forEach { layout ->
            val source = String(Files.readAllBytes(layout))
            val root = source.substringBefore("<androidx.media3.ui.PlayerView")
            val playerView = source.substringAfter("android:id=\"@+id/player_view\"")
                .substringBefore("/>")

            assertTrue(
                "Player root layout should have an id so exit can swap away from the black playback background",
                root.contains("android:id=\"@+id/player_root\"")
            )
            assertTrue(
                "PlayerView should use TextureView so mirror and rotation apply to the decoded picture; exit still avoids synchronous Surface detach",
                playerView.contains("app:surface_type=\"texture_view\"")
            )
        }
    }

    @Test
    fun playerExitDisablesCloseAnimationAndReleasesAfterFinishStarts() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val finishPlayer = source
            .substringAfter("private fun finishPlayer() {")
            .substringBefore("\n    private fun preparePlayerExitFrame()")
        val onDestroy = source
            .substringAfter("override fun onDestroy() {")
            .substringBefore("\n    override fun onPictureInPictureModeChanged")

        assertTrue(
            "Player exit should disable Activity close animation so a black SurfaceView frame is not animated over the video list",
            finishPlayer.contains("overridePendingTransition(0, 0)")
        )
        assertTrue(
            "Player exit should schedule release after finish starts instead of releasing the large decoder before the previous screen is visible",
            finishPlayer.contains("handler.postDelayed({")
                && finishPlayer.contains("releasePlayerAfterExit()")
        )
        assertTrue(
            "onDestroy should use the guarded release helper",
            onDestroy.contains("releasePlayerAfterExit()")
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

    private fun playerLayoutSource(folder: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "res",
            folder,
            "activity_player.xml"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
