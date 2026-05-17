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
        assertTrue(
            "Exit preparation should show an app-colored scrim above the video surface so long-video teardown cannot flash the player black background",
            prepareExit.contains("firstFrameScrim.setBackgroundColor(ContextCompat.getColor(this, backdropColorRes))")
                && prepareExit.contains("firstFrameScrim.alpha = 1f")
                && prepareExit.contains("firstFrameScrim.visibility = View.VISIBLE")
        )
        assertTrue(
            "Exit preparation should hide player chrome above the app-colored exit scrim",
            prepareExit.contains("controlsContainer.visibility = View.INVISIBLE")
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

    @Test
    fun playerExitSettlesPortraitOrientationBeforeFinishingToAvoidReturnBlackFlash() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val finishPlayer = source
            .substringAfter("private fun finishPlayer() {")
            .substringBefore("\n    private fun suppressExitTransition()")
        val settleExit = source
            .substringAfter("private fun settleOrientationBeforeExit(")
            .substringBefore("\n    private fun suppressExitTransition()")
        val rebindControls = source
            .substringAfter("private fun rebindControlsForConfiguration() {")
            .substringBefore("\n    override fun onResume()")

        assertTrue(
            "Player exit should request portrait before finish so the landscape-to-portrait rotation happens behind the player exit scrim",
            finishPlayer.indexOf("preparePlayerExitFrame()") in 0 until finishPlayer.indexOf("settleOrientationBeforeExit(presentation)") &&
                settleExit.contains("requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT")
        )
        assertTrue(
            "Player exit should delay finish until the portrait orientation request has a chance to settle",
            settleExit.contains("handler.postDelayed({") &&
                settleExit.contains("finish()") &&
                settleExit.contains("presentation.finishDelayMs")
        )
        assertTrue(
            "Orientation rebind during exit should restore the app-colored scrim on the freshly inflated player layout",
            rebindControls.contains("if (exitState.isFinishing)") &&
                rebindControls.contains("preparePlayerExitFrame()")
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
