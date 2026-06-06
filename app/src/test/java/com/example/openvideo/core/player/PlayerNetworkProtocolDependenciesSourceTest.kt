package com.example.openvideo.core.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerNetworkProtocolDependenciesSourceTest {

    @Test
    fun media3NetworkProtocolModulesArePackagedForUrlPlayback() {
        val versions = String(Files.readAllBytes(rootFile("gradle", "libs.versions.toml")))
        val appBuild = String(Files.readAllBytes(rootFile("app", "build.gradle.kts")))

        assertTrue(versions.contains("media3-exoplayer-hls"))
        assertTrue(versions.contains("media3-exoplayer-dash"))
        assertTrue(versions.contains("media3-exoplayer-rtsp"))
        assertTrue(appBuild.contains("implementation(libs.media3.exoplayer.hls)"))
        assertTrue(appBuild.contains("implementation(libs.media3.exoplayer.dash)"))
        assertTrue(appBuild.contains("implementation(libs.media3.exoplayer.rtsp)"))
    }

    private fun rootFile(vararg segments: String): Path {
        val relative = Paths.get("", *segments)
        return sequenceOf(relative, Paths.get("..").resolve(relative))
            .first(Files::exists)
    }
}
