package com.example.openvideo.compat

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CompatibilityImplementationTest {

    @Test
    fun playerActivityGuardsPipApisAndChecksDeviceFeature() {
        val source = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerActivity.kt")))
        val pipController = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerPipController.kt")))

        assertTrue(source.contains("private fun isInPipModeCompat(): Boolean = playerPip.isInPipModeCompat()"))
        assertTrue(pipController.contains("PlayerPipCompatPolicy.isInPictureInPictureMode("))
        val pipCompat = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerPipCompatPolicy.kt")))
        assertTrue(pipCompat.contains("sdkInt >= Build.VERSION_CODES.N"))
        val pipPolicy = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerPipPolicy.kt")))
        assertTrue(pipPolicy.contains("sdkInt < Build.VERSION_CODES.O"))
        assertTrue(pipController.contains("PackageManager.FEATURE_PICTURE_IN_PICTURE"))
        assertTrue(pipController.contains("runCatching"))
    }

    @Test
    fun mediaDeletionUsesSystemDeleteRequestOnAndroidRAndAbove() {
        val source = String(Files.readAllBytes(sourceFile("data", "scanner", "VideoScanner.kt")))

        assertTrue(source.contains("fun createDeleteRequest"))
        assertTrue(source.contains("MediaStore.createDeleteRequest"))
        assertTrue(source.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.R"))
    }

    @Test
    fun mediaDeletionSurfacesRecoverableSecurityIntentOnAndroidQ() {
        val source = String(Files.readAllBytes(sourceFile("data", "scanner", "VideoScanner.kt")))

        assertTrue(source.contains("RecoverableSecurityException"))
        assertTrue(source.contains("userAction.actionIntent"))
    }

    @Test
    fun deleteConfirmationResultRetriesDeleteForAndroidQGrantFlow() {
        val home = String(Files.readAllBytes(sourceFile("ui", "home", "HomeFragment.kt")))
        val folder = String(Files.readAllBytes(sourceFile("ui", "local", "FolderVideosFragment.kt")))

        assertTrue(home.contains("completePendingDeleteAfterSystemGrant"))
        assertTrue(home.contains("viewModel.deleteVideos(pendingDeleteVideos)"))
        assertTrue(folder.contains("completePendingDeleteAfterSystemGrant"))
        assertTrue(folder.contains("viewModel.deleteVideos(pendingDeleteVideos)"))
    }

    @Test
    fun playerSettingsChangesApplyImmediatelyFromSharedPrefsListener() {
        val activity = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerActivity.kt")))
        val subtitleController = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerSubtitleController.kt")))
        val prefs = String(Files.readAllBytes(sourceFile("core", "prefs", "PlayerPrefs.kt")))

        assertTrue(prefs.contains("fun requiresImmediatePlayerApply"))
        assertTrue(subtitleController.contains("PlayerPrefs.requiresImmediatePlayerApply(key)"))
        assertTrue(activity.contains("onApplyPlayerSettings = ::applyPlayerSettings"))
    }

    @Test
    fun backgroundAudioStartsForegroundPlaybackService() {
        val activity = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerActivity.kt")))
        val notificationController = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerPlaybackNotificationController.kt")))
        val service = String(Files.readAllBytes(sourceFile("core", "player", "PlaybackService.kt")))
        val intents = String(Files.readAllBytes(sourceFile("core", "player", "PlaybackServiceIntents.kt")))

        assertTrue(service.contains("ACTION_START"))
        assertTrue(service.contains("startForeground"))
        assertTrue(activity.contains("playbackNotifications.startIfNeeded"))
        // ACTION_START routing is now centralized in PlaybackServiceIntents; the Activity goes
        // through the notification controller/helper instead of referencing the action string directly.
        assertTrue(notificationController.contains("PlaybackServiceIntents.start("))
        assertTrue(intents.contains("PlaybackService.ACTION_START"))
    }

    @Test
    fun volumeBoostUsesLoudnessEnhancerInsteadOfInvalidPlayerVolume() {
        val manager = String(Files.readAllBytes(sourceFile("core", "player", "PlayerManager.kt")))
        val source = String(Files.readAllBytes(sourceFile("core", "player", "PlayerAudioEffectsController.kt")))

        assertTrue(manager.contains("audioEffects.setVolumeBoost(enabled)"))
        assertTrue(source.contains("LoudnessEnhancer"))
        assertTrue(source.contains("setTargetGain"))
    }

    @Test
    fun playerScreenshotsUseMediaStoreOnAndroidQAndAbove() {
        val manager = String(Files.readAllBytes(sourceFile("core", "player", "PlayerManager.kt")))
        val source = String(Files.readAllBytes(sourceFile("core", "player", "PlayerMediaExportController.kt")))

        assertTrue(manager.contains("mediaExport.takeScreenshot(videoView, callback)"))
        assertTrue(source.contains("MediaStore.Images.Media.getContentUri"))
        assertTrue(source.contains("MediaStore.MediaColumns.RELATIVE_PATH"))
        assertTrue(source.contains("IS_PENDING"))
        assertTrue(source.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q"))
    }

    @Test
    fun manifestDeclaresAndroid14PartialVideoPermission() {
        val manifest = String(Files.readAllBytes(mainFile("AndroidManifest.xml")))

        assertTrue(manifest.contains("android.permission.READ_MEDIA_VISUAL_USER_SELECTED"))
    }

    @Test
    fun homeRuntimePermissionHandlesAndroid14PartialVideoAccess() {
        val homeSource = String(Files.readAllBytes(sourceFile("ui", "home", "HomeFragment.kt")))
        val policySource = String(Files.readAllBytes(sourceFile("ui", "home", "MediaLibraryPermissionPolicy.kt")))

        assertTrue(homeSource.contains("RequestMultiplePermissions"))
        assertTrue(homeSource.contains("MediaLibraryPermissionPolicy.requiredPermissions"))
        assertTrue(homeSource.contains("hasVideoReadPermission"))
        assertTrue(policySource.contains("Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED"))
        assertTrue(policySource.contains("Build.VERSION_CODES.UPSIDE_DOWN_CAKE"))
    }

    private fun sourceFile(vararg parts: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            *parts
        )
        return appAware(relativePath)
    }

    private fun mainFile(name: String): Path {
        return appAware(Paths.get("src", "main", name))
    }

    private fun appAware(relativePath: Path): Path {
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
