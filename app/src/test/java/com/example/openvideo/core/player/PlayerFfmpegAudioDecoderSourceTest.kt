package com.example.openvideo.core.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerFfmpegAudioDecoderSourceTest {

    @Test
    fun appBundlesFfmpegAudioDecoderDependency() {
        val versions = String(Files.readAllBytes(rootFile("gradle", "libs.versions.toml")))
        val appBuild = String(Files.readAllBytes(rootFile("app", "build.gradle.kts")))

        assertTrue(versions.contains("media3-ffmpeg-decoder"))
        assertTrue(versions.contains("org.jellyfin.media3"))
        assertTrue(appBuild.contains("implementation(libs.media3.ffmpeg.decoder)"))
    }

    @Test
    fun playerPrefersExtensionAudioRenderers() {
        val source = String(Files.readAllBytes(sourceFile("PlayerManager.kt")))

        assertTrue(source.contains("DefaultRenderersFactory"))
        assertTrue(source.contains("EXTENSION_RENDERER_MODE_PREFER"))
        assertTrue(source.contains("setRenderersFactory(renderersFactory)"))
    }

    private fun sourceFile(fileName: String): Path =
        rootFile(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "player",
            fileName
        )

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
