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
    fun media3BaselineIncludesMp4ExtractorNalLengthFixes() {
        val versions = String(Files.readAllBytes(rootFile("gradle", "libs.versions.toml")))

        assertTrue(versions.contains("media3 = \"1.9.0\""))
        assertTrue(versions.contains("media3FfmpegDecoder = \"1.9.0+1\""))
        assertTrue(versions.indexOf("media3 = \"1.9.0\"") < versions.indexOf("[libraries]"))
        assertTrue(versions.indexOf("media3FfmpegDecoder = \"1.9.0+1\"") < versions.indexOf("[libraries]"))
    }

    @Test
    fun playerPrefersExtensionAudioRenderers() {
        val source = String(Files.readAllBytes(sourceFile("PlayerManager.kt")))

        assertTrue(source.contains("DefaultRenderersFactory"))
        assertTrue(source.contains("EXTENSION_RENDERER_MODE_PREFER"))
        assertTrue(source.contains("setEnableDecoderFallback(true)"))
        assertTrue(source.contains("setRenderersFactory(renderersFactory)"))
    }

    @Test
    fun playerMarksSpecialAudioAsSoftwareFallbackCandidates() {
        val trackInfoSource = String(Files.readAllBytes(sourceFile("PlayerAudioTrackInfo.kt")))
        val mediaInfoSource = String(
            Files.readAllBytes(
                rootFile(
                    "app",
                    "src",
                    "main",
                    "java",
                    "com",
                    "example",
                    "openvideo",
                    "ui",
                    "player",
                    "PlayerMediaInfo.kt"
                )
            )
        )

        listOf(
            "audio/vnd.dts",
            "audio/vnd.dts.hd",
            "audio/true-hd",
            "audio/mlp"
        ).forEach { mime ->
            assertTrue("Missing special audio mime in track info: $mime", trackInfoSource.contains(mime))
            assertTrue("Missing special audio mime in media info: $mime", mediaInfoSource.contains(mime))
        }
        assertTrue(trackInfoSource.contains("requiresSoftwareAudioFallback"))
        assertTrue(trackInfoSource.contains("needsSoftwareAudioFallback"))
    }

    @Test
    fun audioDecoderLabelUsesSoftwareFallbackFlagForAllSpecialAudio() {
        val infoSource = String(
            Files.readAllBytes(
                rootFile(
                    "app",
                    "src",
                    "main",
                    "java",
                    "com",
                    "example",
                    "openvideo",
                    "ui",
                    "player",
                    "PlayerSettingsInfoController.kt"
                )
            )
        )
        val decoderLabel = infoSource
            .substringAfter("private fun audioDecoderLabel(")
            .substringBefore("\n    private fun audioCodecLabel")

        assertTrue(decoderLabel.contains("track.requiresSoftwareAudioFallback"))
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
