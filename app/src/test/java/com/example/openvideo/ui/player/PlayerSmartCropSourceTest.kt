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
        val source = String(Files.readAllBytes(playerControlsBinderSource()))
        val block = source.substringAfter(
            "activity.findViewById<View>(R.id.btn_land_smart_crop)?.setGuardedClick(PlayerLockedInteraction.SETTINGS) {"
        ).substringBefore("\n        }\n\n        findViewById<View>(R.id.btn_land_aspect)")

        assertTrue(block.contains("onHandleSmartCropQuickToggle()"))
    }

    @Test
    fun smartCropHandlerUsesPolicyAndAppliesDisplaySettings() {
        val source = String(Files.readAllBytes(playerSmartCropControllerSource()))
        val block = source.substringAfter("fun handleQuickToggle() {")
            .substringBefore("\n    private fun captureVisualBounds(")

        assertTrue(block.contains("PlayerSmartCropPolicy.quickToggleDecision("))
        assertTrue(block.contains("contentFrameMode = decision.contentFrameMode"))
        assertTrue(block.contains("decision.aspectRatioOverride?.let"))
        assertTrue(block.contains("onApplyDisplaySettings()"))
    }

    @Test
    fun smartCropHandlerDoesNotPersistContentFrameMode() {
        val source = String(Files.readAllBytes(playerSmartCropControllerSource()))
        val block = source.substringAfter("fun handleQuickToggle() {")
            .substringBefore("\n    private fun captureVisualBounds(")

        assertTrue(!block.contains("playerPrefs.contentFrameMode = decision.contentFrameMode"))
    }

    @Test
    fun smartCropHandlerDoesNotTreatPersistedContentFrameModeAsActiveSmartCrop() {
        val source = String(Files.readAllBytes(playerSmartCropControllerSource()))
        val block = source.substringAfter("fun handleQuickToggle() {")
            .substringBefore("\n    private fun captureVisualBounds(")

        assertTrue(block.contains("currentMode = activeSmartCropMode ?: ContentFrameMode.OFF"))
        assertTrue(!block.contains("currentMode = activeSmartCropMode ?: playerPrefs.contentFrameMode"))
    }

    @Test
    fun smartCropSamplesVideoRenderBeforeVisualFallback() {
        val source = String(Files.readAllBytes(playerSmartCropControllerSource()))
        val block = source.substringAfter("playerView.postDelayed({")
            .substringBefore("\n        }, SMART_CROP_CAPTURE_DELAY_MS)")

        assertTrue(block.indexOf("captureRenderBounds") >= 0)
        assertTrue(block.indexOf("captureVisualBounds") >= 0)
        assertTrue(block.indexOf("captureRenderBounds") < block.indexOf("captureVisualBounds"))
    }

    @Test
    fun smartCropCancelsPreviousToastBeforeCapturingFallbackScreenshots() {
        val source = String(Files.readAllBytes(playerSmartCropControllerSource()))
        val handler = source.substringAfter("fun handleQuickToggle() {")
            .substringBefore("\n    fun clearSession()")
        val cancelIndex = handler.indexOf("cancelSmartCropToast()")
        val hideIndex = handler.indexOf("hideControlsForCapture()")
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
        val source = String(Files.readAllBytes(contentFrameTransformControllerSource()))
        val block = source.substringAfter("val transformContentFrameMode = if (!landscapeViewport) {")
            .substringBefore("\n        val restoredSmartCropDecision")

        assertTrue(block.contains("smartCropState.contentFrameMode == null && frameSize.width < frameSize.height"))
        assertTrue(block.contains("ContentFrameMode.OFF"))
    }

    @Test
    fun smartCropTransformOverrideOnlyAppliesInLandscapeViewport() {
        val source = String(Files.readAllBytes(contentFrameTransformControllerSource()))
        val block = source.substringAfter("fun applyTransform(")
            .substringBefore("\n    fun sourceSize(")

        assertTrue(block.contains("val smartCropActive ="))
        assertTrue(block.contains("&& landscapeViewport"))
        assertTrue(block.contains("val activeSmartCropTransformOverride = smartCropState.transformOverride.takeIf { smartCropActive }"))
        assertTrue(block.contains("baseTransform = activeSmartCropTransformOverride"))
        assertTrue(block.contains("val transform = activeSmartCropTransformOverride"))
        assertTrue(!block.contains("baseTransform = smartCropState.transformOverride"))
        assertTrue(!block.contains("val transform = smartCropState.transformOverride"))
    }

    @Test
    fun aspectRatioSelectionClearsSmartCropSessionBeforeApplyingDisplaySettings() {
        val activitySource = String(Files.readAllBytes(playerActivitySource()))
        val quickDialogs = String(Files.readAllBytes(playerQuickDialogControllerSource()))
        val quickBlock = quickDialogs.substringAfter("fun showAspectRatioQuickDialog()")
            .substringBefore("\n    fun showSpeedPickerDialog()")
        val helperBlock = activitySource.substringAfter("onAspectRatioChanged = {")
            .substringBefore("\n            },")
        val clearIndex = helperBlock.indexOf("smartCrop.clearSession()")
        val applyIndex = helperBlock.indexOf("applyDisplaySettings()")

        assertTrue(quickBlock.contains("onAspectRatioChanged()"))
        assertTrue(clearIndex >= 0)
        assertTrue(applyIndex >= 0)
        assertTrue(clearIndex < applyIndex)
    }

    @Test
    fun playerSettingsAspectRatioCallbackClearsSmartCropSessionBeforeApplyingDisplaySettings() {
        val source = String(Files.readAllBytes(playerActivitySource()))
        val quickDialogs = String(Files.readAllBytes(playerQuickDialogControllerSource()))
        val dialogBlock = quickDialogs.substringAfter("fun openPlayerSettingsDialog()")
            .substringBefore("\n    fun showAspectRatioQuickDialog()")
        val helperBlock = source.substringAfter("onAspectRatioChanged = {")
            .substringBefore("\n            },")
        val clearIndex = helperBlock.indexOf("smartCrop.clearSession()")
        val applyIndex = helperBlock.indexOf("applyDisplaySettings()")

        assertTrue(source.contains("onAspectRatioChanged = {"))
        assertTrue(dialogBlock.contains("onAspectRatioChanged = onAspectRatioChanged"))
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

    private fun playerQuickDialogControllerSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerQuickDialogController.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun playerSmartCropControllerSource(): Path {
        return kotlinSource("PlayerSmartCropController.kt")
    }

    private fun playerControlsBinderSource(): Path {
        return kotlinSource("PlayerControlsBinder.kt")
    }

    private fun contentFrameTransformControllerSource(): Path {
        return kotlinSource("PlayerContentFrameTransformController.kt")
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

    private fun resource(dir: String, file: String): Path {
        val relativePath = Paths.get("src", "main", "res", dir, file)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
