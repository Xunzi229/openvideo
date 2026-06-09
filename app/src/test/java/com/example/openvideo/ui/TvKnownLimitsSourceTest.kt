package com.example.openvideo.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TvKnownLimitsSourceTest {

    @Test
    fun androidTvKnownLimitsDocumentCoversReleaseFacingLimits() {
        val doc = rootText("docs", "roadmap", "android-tv-known-limits.md")

        assertTrue(doc.contains("P5-TV-LIMITS-001"))
        assertTrue(doc.contains("Android TV Known Limits"))
        assertTrue(doc.contains("Codec and format limits"))
        assertTrue(doc.contains("Storage and permissions"))
        assertTrue(doc.contains("Network sources"))
        assertTrue(doc.contains("Remote and focus"))
        assertTrue(doc.contains("TV home scope"))
        assertTrue(doc.contains("Release copy"))
        assertTrue(doc.contains("READ_MEDIA_VIDEO"))
        assertTrue(doc.contains("READ_MEDIA_VISUAL_USER_SELECTED"))
        assertTrue(doc.contains("No DRM license flow"))
        assertTrue(doc.contains("WebDAV"))
        assertTrue(doc.contains("SMB, Jellyfin, Plex"))
        assertTrue(doc.contains("network-protocol-support-matrix.md"))
        assertTrue(doc.contains("webdav-usage-and-limits.md"))
        assertTrue(doc.contains("tv-regression-matrix.md"))
    }

    @Test
    fun androidTvKnownLimitsStayAlignedWithManifestAndProtocolDocs() {
        val doc = rootText("docs", "roadmap", "android-tv-known-limits.md")
        val manifest = rootText("app", "src", "main", "AndroidManifest.xml")
        val networkMatrix = rootText("docs", "roadmap", "network-protocol-support-matrix.md")
        val webDavLimits = rootText("docs", "roadmap", "webdav-usage-and-limits.md")
        val mediaServerDecision = rootText("docs", "roadmap", "smb-nas-dlna-research.md")

        assertTrue(manifest.contains("android.permission.READ_MEDIA_VIDEO"))
        assertTrue(manifest.contains("android.permission.READ_MEDIA_VISUAL_USER_SELECTED"))
        assertTrue(networkMatrix.contains("No DRM license flow"))
        assertTrue(webDavLimits.contains("No pagination or lazy loading"))
        assertTrue(mediaServerDecision.contains("Do not add Jellyfin or Plex runtime dependencies"))

        assertTrue(doc.contains("android.permission.READ_MEDIA_VIDEO"))
        assertTrue(doc.contains("android.permission.READ_MEDIA_VISUAL_USER_SELECTED"))
        assertTrue(doc.contains("No pagination or lazy loading"))
        assertTrue(doc.contains("Do not add Jellyfin or Plex runtime dependencies"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
