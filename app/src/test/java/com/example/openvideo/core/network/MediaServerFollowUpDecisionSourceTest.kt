package com.example.openvideo.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MediaServerFollowUpDecisionSourceTest {

    @Test
    fun smbDlnaJellyfinPlexFollowUpDecisionRecordIsComplete() {
        val doc = rootText("docs", "roadmap", "smb-nas-dlna-research.md")

        assertTrue(doc.contains("P3-MEDIA-SERVER-001"))
        assertTrue(doc.contains("SMB/DLNA/Jellyfin/Plex follow-up decision"))
        assertTrue(doc.contains("Jellyfin native API"))
        assertTrue(doc.contains("Plex API"))
        assertTrue(doc.contains("Do not use DLNA as the default Jellyfin/Plex integration path"))
        assertTrue(doc.contains("Do not add Jellyfin or Plex runtime dependencies to the APK in this slice"))
        assertTrue(doc.contains("access tokens"))
        assertTrue(doc.contains("transcoding"))
        assertTrue(doc.contains("direct play"))
        assertTrue(doc.contains("future Phase"))
    }

    @Test
    fun productionApkDoesNotDeclareJellyfinOrPlexRuntimeDependenciesYet() {
        val gradle = rootText("app", "build.gradle.kts")
        val versions = rootText("gradle", "libs.versions.toml")
        val dependencyFiles = gradle + "\n" + versions

        assertFalse(dependencyFiles.contains("org.jellyfin.sdk", ignoreCase = true))
        assertFalse(dependencyFiles.contains("jellyfin-sdk", ignoreCase = true))
        assertFalse(dependencyFiles.contains("com.plexapp", ignoreCase = true))
        assertFalse(dependencyFiles.contains("plex-api", ignoreCase = true))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
