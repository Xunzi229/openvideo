package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSmartCropSourceTest {

    @Test
    fun landscapeControlsExposeSmartCropQuickButton() {
        val source = String(Files.readAllBytes(landscapeControlsSource()))

        assertTrue(source.contains("@+id/btn_land_smart_crop"))
        assertTrue(source.contains("@string/player_action_smart_crop"))
        assertTrue(source.contains("@drawable/ic_cut"))
    }

    @Test
    fun playerActivityRoutesSmartCropButtonToHandler() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter(
            "findViewById<View>(R.id.btn_land_smart_crop)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {"
        ).substringBefore("\n        }\n\n        findViewById<View>(R.id.btn_land_aspect)")

        assertTrue(block.contains("handleSmartCropQuickToggle()"))
    }

    @Test
    fun smartCropHandlerUsesPolicyAndAppliesDisplaySettings() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("private fun handleSmartCropQuickToggle() {")
            .substringBefore("\n    private fun showAspectRatioQuickDialog()")

        assertTrue(block.contains("PlayerSmartCropPolicy.quickToggleDecision("))
        assertTrue(block.contains("smartCropContentFrameMode = decision.contentFrameMode"))
        assertTrue(block.contains("decision.aspectRatioOverride?.let"))
        assertTrue(block.contains("applyDisplaySettings()"))
    }

    @Test
    fun smartCropHandlerDoesNotPersistContentFrameMode() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("private fun handleSmartCropQuickToggle() {")
            .substringBefore("\n    private fun showAspectRatioQuickDialog()")

        assertTrue(!block.contains("playerPrefs.contentFrameMode = decision.contentFrameMode"))
    }

    @Test
    fun smartCropHandlerDoesNotTreatPersistedContentFrameModeAsActiveSmartCrop() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("private fun handleSmartCropQuickToggle() {")
            .substringBefore("\n    private fun showAspectRatioQuickDialog()")

        assertTrue(block.contains("currentMode = activeSmartCropMode ?: ContentFrameMode.OFF"))
        assertTrue(!block.contains("currentMode = activeSmartCropMode ?: playerPrefs.contentFrameMode"))
    }

    @Test
    fun smartCropSamplesVideoRenderBeforeVisualFallback() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("playerView.postDelayed({")
            .substringBefore("\n        }, SMART_CROP_CAPTURE_DELAY_MS)")

        assertTrue(block.indexOf("captureSmartCropRenderBounds") >= 0)
        assertTrue(block.indexOf("captureSmartCropVisualBounds") >= 0)
        assertTrue(block.indexOf("captureSmartCropRenderBounds") < block.indexOf("captureSmartCropVisualBounds"))
    }

    @Test
    fun smartCropCancelsPreviousToastBeforeCapturingFallbackScreenshots() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val handler = source.substringAfter("private fun handleSmartCropQuickToggle() {")
            .substringBefore("\n    private fun hideControlsForSmartCropCapture()")
        val cancelIndex = handler.indexOf("cancelSmartCropToast()")
        val hideIndex = handler.indexOf("hideControlsForSmartCropCapture()")
        val helper = source.substringAfter("private fun showSmartCropToast(messageRes: Int) {")
            .substringBefore("\n    private fun")

        assertTrue(source.contains("private var smartCropToast: Toast? = null"))
        assertTrue(cancelIndex >= 0)
        assertTrue(hideIndex >= 0)
        assertTrue(cancelIndex < hideIndex)
        assertTrue(helper.contains("cancelSmartCropToast()"))
        assertTrue(helper.contains("smartCropToast = Toast.makeText"))
        assertTrue(helper.contains("smartCropToast?.show()"))
    }

    @Test
    fun restoredPortraitContentFrameModeIsSuppressedUnlessSmartCropSessionIsActive() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("val transformContentFrameMode = if (!landscapeViewport) {")
            .substringBefore("\n        val restoredSmartCropDecision")

        assertTrue(block.contains("smartCropContentFrameMode == null && frameSize.width < frameSize.height"))
        assertTrue(block.contains("ContentFrameMode.OFF"))
    }

    @Test
    fun smartCropTransformOverrideOnlyAppliesInLandscapeViewport() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("private fun applyPlayerContentFrameTransform(")
            .substringBefore("\n    private fun contentFrameSourceSize(")

        assertTrue(block.contains("val smartCropActive ="))
        assertTrue(block.contains("&& landscapeViewport"))
        assertTrue(block.contains("val activeSmartCropTransformOverride = smartCropTransformOverride.takeIf { smartCropActive }"))
        assertTrue(block.contains("cachedBaseContentFrameTransform = activeSmartCropTransformOverride"))
        assertTrue(block.contains("val transform = activeSmartCropTransformOverride"))
        assertTrue(!block.contains("cachedBaseContentFrameTransform = smartCropTransformOverride"))
        assertTrue(!block.contains("val transform = smartCropTransformOverride"))
    }

    @Test
    fun aspectRatioSelectionClearsSmartCropSessionBeforeApplyingDisplaySettings() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val block = source.substringAfter("private fun showAspectRatioQuickDialog() {")
            .substringBefore("\n    private fun handleSmartCropQuickToggle()")
        val clearIndex = block.indexOf("clearSmartCropSession()")
        val applyIndex = block.indexOf("applyDisplaySettings()")

        assertTrue(clearIndex >= 0)
        assertTrue(applyIndex >= 0)
        assertTrue(clearIndex < applyIndex)
    }

    @Test
    fun playerSettingsAspectRatioCallbackClearsSmartCropSessionBeforeApplyingDisplaySettings() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val dialogBlock = source.substringAfter("private fun openPlayerSettingsDialog()")
            .substringBefore("\n    private fun showAspectRatioQuickDialog()")
        val helperBlock = source.substringAfter("private fun applyAspectRatioDisplayChange() {")
            .substringBefore("\n    private fun showAspectRatioQuickDialog()")
        val clearIndex = helperBlock.indexOf("clearSmartCropSession()")
        val applyIndex = helperBlock.indexOf("applyDisplaySettings()")

        assertTrue(dialogBlock.contains("onAspectRatioChanged = ::applyAspectRatioDisplayChange"))
        assertTrue(clearIndex >= 0)
        assertTrue(applyIndex >= 0)
        assertTrue(clearIndex < applyIndex)
    }

    private fun landscapeControlsSource(): Path =
        resource("layout-land", "player_controls.xml")

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
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun resource(dir: String, file: String): Path {
        val relativePath = Paths.get("src", "main", "res", dir, file)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
