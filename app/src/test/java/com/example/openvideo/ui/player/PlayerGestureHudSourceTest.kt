package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerGestureHudSourceTest {

    @Test
    fun doubleTapSeekHudSurvivesTheGestureUpThatTriggeredIt() {
        val source = sourceFile("PlayerGestureController.kt").readText()

        assertTrue(source.contains("keepGestureHudAfterActionUp"))
        assertTrue(source.contains("keepGestureHudAfterActionUp = true"))
        assertTrue(source.contains("if (keepGestureHudAfterActionUp)"))
    }

    @Test
    fun doubleTapForwardAndBackwardPreferencesKeepTheirConfiguredDirection() {
        val source = sourceFile("PlayerGestureController.kt").readText()
        val doubleTapBlock = source.substringAfter("override fun onDoubleTap")
            .substringBefore("override fun onLongPress")

        assertTrue(doubleTapBlock.contains("DoubleTapAction.FORWARD -> handleDoubleTapSeek(PlayerSwipeSide.RIGHT)"))
        assertTrue(doubleTapBlock.contains("DoubleTapAction.BACKWARD -> handleDoubleTapSeek(PlayerSwipeSide.LEFT)"))
        assertFalse(doubleTapBlock.contains("DoubleTapAction.FORWARD,\r\n                    DoubleTapAction.BACKWARD -> handleDoubleTapSeek(e.x)"))
        assertFalse(doubleTapBlock.contains("DoubleTapAction.FORWARD,\n                    DoubleTapAction.BACKWARD -> handleDoubleTapSeek(e.x)"))
    }

    @Test
    fun accumulatedDoubleTapSeekUsesAnchorPositionInsteadOfPotentiallyStaleUiState() {
        val source = sourceFile("PlayerGestureController.kt").readText()

        assertTrue(source.contains("doubleTapSeekAnchorPositionMs"))
        assertTrue(source.contains("PlayerDoubleTapSeekPolicy.preview("))
        assertTrue(source.contains("anchorPositionMs = doubleTapSeekAnchorPositionMs"))
        assertTrue(source.contains("doubleTapSeekAnchorPositionMs = preview.anchorPositionMs"))
    }

    @Test
    fun doubleTapSeekDoesNotCommitWhenDurationIsUnknown() {
        val source = sourceFile("PlayerGestureController.kt").readText()
        val block = source.substringAfter("private fun handleDoubleTapSeek")
            .substringBefore("\n    private fun")

        assertTrue(block.contains("if (preview.seekable)"))
        assertTrue(block.contains("viewModel.seekTo(preview.targetMs)"))
    }

    @Test
    fun longPressSpeedUsesPolicyForStartAndRelease() {
        val source = sourceFile("PlayerGestureController.kt").readText()

        assertTrue(source.contains("PlayerLongPressPolicy.onPress("))
        assertTrue(source.contains("PlayerLongPressPolicy.onRelease("))
        assertTrue(source.contains("decision.targetSpeed?.let { speed ->"))
        assertTrue(source.contains("release.restoreSpeed?.let(viewModel::setSpeed)"))
    }

    @Test
    fun controlsChromeVisibilityUsesPresentationPolicy() {
        val source = sourceFile("PlayerActivity.kt").readText()

        assertTrue(source.contains("PlayerChromeVisibilityPolicy.show("))
        assertTrue(source.contains("PlayerChromeVisibilityPolicy.hide()"))
        assertTrue(source.contains("PlayerChromeVisibilityPolicy.showLocked("))
        assertTrue(source.contains("PlayerChromeVisibilityPolicy.pictureInPicture()"))
        assertTrue(source.contains("applyChromePresentation("))
    }

    @Test
    fun endedQueueSwitchRespectsAutoPlayNextPreference() {
        val source = sourceFile("PlayerActivity.kt").readText()
        val block = source.substringAfter("private fun handlePlaybackEnded()")
            .substringBefore("\n    private fun")

        assertTrue(block.contains("PlayerPlaybackEndPolicy.decide("))
        assertTrue(block.contains("autoPlayNext = playerPrefs.autoPlayNext"))
    }

    @Test
    fun abLoopUsesPurePolicyForToggleAndTickSeek() {
        val source = sourceFile("PlayerActivity.kt").readText()

        assertTrue(source.contains("PlayerAbLoopPolicy.onToggle("))
        assertTrue(source.contains("applyAbLoopResult("))
    }

    @Test
    fun playbackTickUsesSingleSeekDecisionPolicy() {
        val source = sourceFile("PlayerActivity.kt").readText()

        assertTrue(source.contains("applyPlaybackTickSeek("))
        assertTrue(source.contains("PlayerPlaybackTickPolicy.seekTarget("))
        assertTrue(source.contains("hasSkippedIntro = result.hasSkippedIntro"))
        assertTrue(source.contains("hasSkippedOutro = result.hasSkippedOutro"))
    }

    @Test
    fun videoSwitchResetsMediaBoundPlaybackStateThroughPolicy() {
        val activitySource = sourceFile("PlayerActivity.kt").readText()
        val gestureSource = sourceFile("PlayerGestureController.kt").readText()

        assertTrue(activitySource.contains("resetPlaybackSessionForNewVideo()"))
        assertTrue(activitySource.contains("switchSessionVideo("))
        assertTrue(activitySource.contains("dismissSubtitleSettingsSheet()"))
        assertTrue(activitySource.contains("PlayerVideoSwitchPolicy.resetForNewVideo()"))
        assertTrue(activitySource.contains("abLoopState = reset.abLoopState"))
        assertTrue(activitySource.contains("playerGestures.resetForNewVideo(reset)"))
        assertTrue(gestureSource.contains("doubleTapSeekState = reset.doubleTapSeekState"))
        assertTrue(gestureSource.contains("pendingSeekTarget = reset.pendingSeekTarget"))
    }

    @Test
    fun horizontalSeekGestureUsesDownPositionAnchorAndSeekPreviewPolicy() {
        val source = sourceFile("PlayerGestureController.kt").readText()

        assertTrue(source.contains("seekGestureAnchorPositionMs"))
        assertTrue(source.contains("seekGestureAnchorPositionMs = viewModel.uiState.value.currentPosition"))
        assertTrue(source.contains("PlayerSeekGesturePolicy.horizontalPreview("))
        assertTrue(source.contains("anchorPositionMs = seekGestureAnchorPositionMs ?: state.currentPosition"))
        assertTrue(source.contains("pendingSeekTarget = preview.targetMs.takeIf { preview.seekable }"))
    }

    @Test
    fun verticalSeekGestureUsesSeekPreviewPolicyAndDoesNotCommitUnknownDuration() {
        val source = sourceFile("PlayerGestureController.kt").readText()

        assertTrue(source.contains("PlayerSeekGesturePolicy.verticalPreview("))
        assertTrue(source.contains("screenHeightPx = activity.resources.displayMetrics.heightPixels"))
        assertTrue(source.contains("pendingSeekTarget = preview.targetMs.takeIf { preview.seekable }"))
    }

    @Test
    fun brightnessAndVolumeGesturesUseLevelAdjustmentPolicy() {
        val source = sourceFile("PlayerGestureController.kt").readText()

        assertTrue(source.contains("PlayerLevelAdjustmentPolicy.verticalBrightness("))
        assertTrue(source.contains("PlayerLevelAdjustmentPolicy.verticalVolume("))
    }

    @Test
    fun cancelledSeekGestureClearsPendingTargetWithoutSeeking() {
        val source = sourceFile("PlayerGestureController.kt").readText()

        assertTrue(source.contains("MotionEvent.ACTION_CANCEL -> {"))
        assertTrue(source.contains("pendingSeekTarget = null"))
        assertTrue(source.contains("seekGestureAnchorPositionMs = null"))
    }

    @Test
    fun lockedChromeVisibilityUsesLockedControlsPolicy() {
        val source = sourceFile("PlayerActivity.kt").readText()
        assertTrue(source.contains("PlayerLockedControlsPolicy.visibility(isScreenLocked, controlsVisible)"))
        assertTrue(source.contains("PlayerLockedControlsPolicy.isChromeRegionVisible("))
    }

    @Test
    fun lockedGestureOverlayUsesPureLockGesturePolicy() {
        val source = sourceFile("PlayerActivity.kt").readText()
        val lockBlock = source.substringAfter("private fun setLockedGestureOverlay()")
            .substringBefore("\n    private fun")

        assertTrue(lockBlock.contains("PlayerLockGesturePolicy.onTouch("))
        assertTrue(lockBlock.contains("PlayerTouchActionPolicy.fromMotionActionMasked(event.actionMasked)"))
        assertTrue(lockBlock.contains("if (decision.revealLockedControls) showLockedControls()"))
        assertTrue(lockBlock.contains("decision.consumeTouch"))
        assertFalse(
            "Lock overlay must not map MotionEvent inline.",
            lockBlock.contains("toPlayerTouchAction()")
        )
    }

    @Test
    fun tutorialPageExposesGesturePresetsThroughPurePresetPolicy() {
        val source = sourceFile("PlayerSettingsDialog.kt").readText()

        assertTrue(source.contains("PlayerGesturePreset.entries.map"))
        assertTrue(source.contains("applyGesturePreset(preset)"))
        assertTrue(source.contains("PlayerGesturePresetPolicy.settingsFor(preset)"))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun sourceFile(fileName: String): Path {
        val relative = Paths.get(
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
        return sequenceOf(
            relative,
            Paths.get("app").resolve(relative)
        ).first(Files::exists)
    }
}
