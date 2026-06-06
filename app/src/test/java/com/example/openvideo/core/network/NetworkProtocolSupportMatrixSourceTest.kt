package com.example.openvideo.core.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class NetworkProtocolSupportMatrixSourceTest {

    @Test
    fun networkProtocolSupportMatrixDocumentsImplementedUrlProtocols() {
        val doc = rootText("docs", "roadmap", "network-protocol-support-matrix.md")

        assertTrue(doc.contains("P3-MATRIX-001"))
        assertTrue(doc.contains("HTTP/HTTPS progressive"))
        assertTrue(doc.contains("HLS"))
        assertTrue(doc.contains("DASH"))
        assertTrue(doc.contains("RTSP"))
        assertTrue(doc.contains("Media3 module"))
        assertTrue(doc.contains("Buffering profile"))
        assertTrue(doc.contains("Cleartext HTTP"))
        assertTrue(doc.contains("Live / unknown duration"))
        assertTrue(doc.contains("Known limits"))
        assertTrue(doc.contains("Verification samples"))
    }

    @Test
    fun matrixMatchesPackagedProtocolModulesAndBufferProfiles() {
        val doc = rootText("docs", "roadmap", "network-protocol-support-matrix.md")
        val appBuild = rootText("app", "build.gradle.kts")
        val bufferingPolicy = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "player",
            "PlayerBufferingPolicy.kt"
        )

        assertTrue(appBuild.contains("implementation(libs.media3.exoplayer.hls)"))
        assertTrue(appBuild.contains("implementation(libs.media3.exoplayer.dash)"))
        assertTrue(appBuild.contains("implementation(libs.media3.exoplayer.rtsp)"))
        assertTrue(bufferingPolicy.contains("ADAPTIVE_STREAM"))
        assertTrue(bufferingPolicy.contains("RTSP_STREAM"))
        assertTrue(bufferingPolicy.contains("NETWORK_PROGRESSIVE"))

        assertTrue(doc.contains("media3-exoplayer-hls"))
        assertTrue(doc.contains("media3-exoplayer-dash"))
        assertTrue(doc.contains("media3-exoplayer-rtsp"))
        assertTrue(doc.contains("ADAPTIVE_STREAM"))
        assertTrue(doc.contains("RTSP_STREAM"))
        assertTrue(doc.contains("NETWORK_PROGRESSIVE"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
