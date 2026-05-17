package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Guards that small P9-1 slimming slices stay routed through their pure policies,
 * so the inlined `if (config.orientation == 1) ...` magic number and the inlined
 * `formatTime(...)` math do not drift back into [PlayerActivity].
 */
class PlayerActivityP9SlimmingSourceTest {

    @Test
    fun fullscreenButtonUsesOrientationTogglePolicy() {
        val source = playerActivitySource()
        assertTrue(
            "btnFullscreen click must delegate to PlayerOrientationTogglePolicy.",
            source.contains("PlayerOrientationTogglePolicy.nextRequestedOrientation(")
        )
        assertFalse(
            "btnFullscreen must not compare configuration.orientation against the literal `1`.",
            source.contains("resources.configuration.orientation == 1")
        )
    }

    @Test
    fun formatTimeDelegatesToPlayerTimeFormatter() {
        val source = playerActivitySource()
        assertTrue(
            "formatTime must delegate to PlayerTimeFormatter.format.",
            source.contains("private fun formatTime(ms: Long): String = PlayerTimeFormatter.format(ms)")
        )
        assertFalse(
            "formatTime must not keep an inlined hour/minute/second branch.",
            source.contains("String.format(\"%d:%02d:%02d\"") &&
                source.contains("String.format(\"%02d:%02d\"")
        )
    }

    @Test
    fun landSpeedLabelDelegatesToPlayerSpeedLabel() {
        val source = playerActivitySource()
        assertTrue(
            "landSpeedLabel must delegate to PlayerSpeedLabel.format.",
            source.contains("private fun landSpeedLabel(speed: Float): String = PlayerSpeedLabel.format(speed)")
        )
        assertFalse(
            "landSpeedLabel must not keep its old DefaultPlayerSettings + manual formatting branch.",
            source.contains("val s = DefaultPlayerSettings.supportedSpeedOrDefault(speed)")
        )
    }

    @Test
    fun fourKResolutionBadgeUsesLandscapeBadgePolicy() {
        val source = playerActivitySource()
        assertTrue(
            "Landscape badge visibility must go through PlayerLandscapeBadgePolicy.is4kVideo(...).",
            source.contains("PlayerLandscapeBadgePolicy.is4kVideo(w)")
        )
        assertFalse(
            "Landscape badge must not inline the 3840 width magic number.",
            source.contains("if (w >= 3840)")
        )
    }

    @Test
    fun lockOverlayUsesTouchActionPolicy() {
        val source = playerActivitySource()
        assertTrue(
            "Locked gesture overlay must map touches via PlayerTouchActionPolicy.",
            source.contains("PlayerTouchActionPolicy.fromMotionActionMasked(event.actionMasked)")
        )
        assertFalse(
            "Activity must not keep MotionEvent.toPlayerTouchAction extension.",
            source.contains("fun MotionEvent.toPlayerTouchAction()")
        )
    }

    @Test
    fun pipAspectRatioConversionLivesOnPolicyType() {
        val activitySource = playerActivitySource()
        val pipBlock = activitySource.substringAfter("private fun enterPipModeIfSupported()")
            .substringBefore("\n    private fun startPlaybackServiceIfNeeded")
        assertTrue(pipBlock.contains("decision.aspectRatio?.toRational()"))
        assertTrue(pipBlock.contains("PlayerPipPolicy.fallbackRational()"))
        assertFalse(pipBlock.contains("private fun PlayerPipAspectRatio.toRational()"))
    }

    @Test
    fun mirrorAndSubtitlePositionGoThroughDisplayAdjustment() {
        val source = playerActivitySource()
        assertTrue(
            "applyDisplaySettings must use PlayerDisplayAdjustment.mirrorScaleX(...).",
            source.contains("PlayerDisplayAdjustment.mirrorScaleX(playerPrefs.mirror)")
        )
        assertTrue(
            "Subtitle position translation must use PlayerDisplayAdjustment.subtitleTranslationY(...).",
            source.contains("PlayerDisplayAdjustment.subtitleTranslationY(")
        )
        assertFalse(
            "Activity must not keep the inline `if (playerPrefs.mirror) -1f else 1f` ternary.",
            source.contains("if (playerPrefs.mirror) -1f else 1f")
        )
        assertFalse(
            "Activity must not keep the inline `playerView.height * 0.6f` travel computation.",
            source.contains("playerView.height * 0.6f")
        )
        assertFalse(
            "Activity must not keep the inline `subtitlePosition.coerceIn(0f, 1f)` computation.",
            source.contains("playerPrefs.subtitlePosition.coerceIn(0f, 1f)")
        )
    }

    @Test
    fun landscapeGeometryUsesPolicy() {
        val source = playerActivitySource()
        assertTrue(
            "applyLandscapePlayerGeometry must delegate to PlayerLandscapeGeometryPolicy.compute(...).",
            source.contains("PlayerLandscapeGeometryPolicy.compute(")
        )
        // All landscape geometry ratios live in the policy now; they must not reappear inline.
        val bannedRatios = listOf(
            "0.022f",
            "0.026f",
            "0.028f",
            "0.032f",
            "0.049f",
            "0.060f",
            "0.009f"
        )
        for (ratio in bannedRatios) {
            assertFalse(
                "Landscape ratio `$ratio` must stay inside PlayerLandscapeGeometryPolicy.",
                source.contains(ratio)
            )
        }
        // dp clamp endpoints must not be inlined either.
        val bannedDpClamps = listOf(
            "40f * dm",
            "52f * dm",
            "64f * dm",
            "14f * dm",
            "22f * dm",
            "6f * dm",
            "12f * dm"
        )
        for (clamp in bannedDpClamps) {
            assertFalse(
                "Landscape dp clamp `$clamp` must stay inside PlayerLandscapeGeometryPolicy.",
                source.contains(clamp)
            )
        }
    }

    private fun playerActivitySource(): String {
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
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
