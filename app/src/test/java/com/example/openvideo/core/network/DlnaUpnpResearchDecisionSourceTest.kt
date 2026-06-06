package com.example.openvideo.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DlnaUpnpResearchDecisionSourceTest {

    @Test
    fun dlnaUpnpResearchDocumentRecordsBrowseAndCastingBoundary() {
        val doc = rootText("docs", "roadmap", "smb-nas-dlna-research.md")

        assertTrue(doc.contains("P3-DLNA-001"))
        assertTrue(doc.contains("DLNA/UPnP"))
        assertTrue(doc.contains("Browsing vs casting boundary"))
        assertTrue(doc.contains("Do not add a DLNA or UPnP runtime dependency to the APK in this slice"))
        assertTrue(doc.contains("defer DLNA to a later Phase"))
        assertTrue(doc.contains("ContentDirectory"))
        assertTrue(doc.contains("MediaRenderer"))
        assertTrue(doc.contains("SSDP"))
        assertTrue(doc.contains("Cling"))
        assertTrue(doc.contains("CyberGarage"))
        assertTrue(doc.contains("Jellyfin/Plex"))
    }

    @Test
    fun productionApkDoesNotDeclareDlnaRuntimeDependenciesYet() {
        val gradle = rootText("app", "build.gradle.kts")
        val versions = rootText("gradle", "libs.versions.toml")
        val dependencyFiles = gradle + "\n" + versions

        assertFalse(dependencyFiles.contains("org.fourthline.cling"))
        assertFalse(dependencyFiles.contains("org.cybergarage"))
        assertFalse(dependencyFiles.contains("upnp", ignoreCase = true))
        assertFalse(dependencyFiles.contains("dlna", ignoreCase = true))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
