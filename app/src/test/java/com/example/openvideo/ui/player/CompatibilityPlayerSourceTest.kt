package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CompatibilityPlayerSourceTest {

    @Test
    fun projectUsesStableOfficialLibVlcArtifact() {
        val catalog = rootText("gradle", "libs.versions.toml")
        val build = rootText("app", "build.gradle.kts")

        assertTrue(catalog.contains("libvlc = \"3.7.5\""))
        assertTrue(catalog.contains("group = \"org.videolan.android\", name = \"libvlc-all\""))
        assertTrue(build.contains("implementation(libs.libvlc)"))
        assertTrue(build.contains("compileSdk = 36"))
        assertFalse(catalog.contains("libvlc = \"4.0.0-eap"))
    }

    @Test
    fun compatibilityPlaybackIsOnlyOpenedByExplicitErrorAction() {
        val hud = playerText("PlayerErrorHudController.kt")
        val events = playerText("PlayerEventController.kt")
        val activity = playerText("PlayerActivity.kt")

        assertTrue(hud.contains("compatibilityButtonProvider()?.setOnClickListener { onOpenCompatibilityMode() }"))
        assertTrue(activity.contains("private fun openCompatibilityMode()"))
        assertTrue(activity.contains("CompatibilityPlayerContract.createIntent("))
        assertFalse(events.contains("openCompatibilityMode"))
        assertFalse(events.contains("CompatibilityPlayerActivity"))
    }

    @Test
    fun compatibilityPlayerPreservesPositionAndDoesNotForceHardwareDecoder() {
        val activity = playerText("CompatibilityPlayerActivity.kt")
        val contract = playerText("CompatibilityPlayerContract.kt")
        val viewModel = playerText("CompatibilityPlayerViewModel.kt")

        assertTrue(activity.contains("setHWDecoderEnabled(true, false)"))
        assertFalse(activity.contains("setHWDecoderEnabled(true, true)"))
        assertTrue(activity.contains("mediaPlayer.setTime(lastPositionMs, false)"))
        assertTrue(activity.contains("contentResolver.openFileDescriptor(request.uri, \"r\")"))
        assertTrue(activity.contains("Media(libVlc, descriptor.fileDescriptor)"))
        assertTrue(activity.contains("mediaFileDescriptor?.close()"))
        assertTrue(contract.contains("startPositionMs"))
        assertTrue(contract.contains("requestHeaders"))
        assertTrue(viewModel.contains("repository.saveHistory("))
        assertTrue(viewModel.contains("position = if (playbackEnded) 0L else positionMs"))
    }

    @Test
    fun compatibilityActivityAndLicenseArePackaged() {
        val manifest = rootText("app", "src", "main", "AndroidManifest.xml")
        val metadata = rootText("app", "src", "main", "res", "raw", "third_party_license_metadata")
        val proguard = rootText("app", "proguard-rules.pro")

        assertTrue(manifest.contains(".ui.player.CompatibilityPlayerActivity"))
        assertTrue(metadata.contains("LibVLC Android"))
        assertTrue(proguard.contains("-keep class org.videolan.libvlc.** { *; }"))
    }

    private fun playerText(file: String): String = rootText(
        "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player", file
    )

    private fun rootText(vararg parts: String): String {
        val relative = Paths.get(parts.first(), *parts.drop(1).toTypedArray())
        val path: Path = sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
