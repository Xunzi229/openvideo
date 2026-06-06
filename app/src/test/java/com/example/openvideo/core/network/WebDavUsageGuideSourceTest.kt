package com.example.openvideo.core.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class WebDavUsageGuideSourceTest {

    @Test
    fun webDavUsageGuideDocumentsCurrentMvpBehaviorAndLimits() {
        val doc = rootText("docs", "roadmap", "webdav-usage-and-limits.md")

        assertTrue(doc.contains("P3-WD-DOC-001"))
        assertTrue(doc.contains("Add a WebDAV source"))
        assertTrue(doc.contains("EncryptedSharedPreferences"))
        assertTrue(doc.contains("Room does not store passwords"))
        assertTrue(doc.contains("PROPFIND Depth: 0"))
        assertTrue(doc.contains("PROPFIND Depth: 1"))
        assertTrue(doc.contains("Authorization"))
        assertTrue(doc.contains("same-directory subtitles"))
        assertTrue(doc.contains("WebDavMemoryCache"))
        assertTrue(doc.contains("Known limits"))
        assertTrue(doc.contains("edit credentials"))
        assertTrue(doc.contains("large directory"))
    }

    @Test
    fun guideStaysAlignedWithImplementedWebDavCodePaths() {
        val doc = rootText("docs", "roadmap", "webdav-usage-and-limits.md")
        val client = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "network",
            "WebDavConnectionClient.kt"
        )
        val credentialStore = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "prefs",
            "WebDavCredentialStore.kt"
        )
        val policy = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "network",
            "WebDavConnectionPolicy.kt"
        )
        val browser = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "sources",
            "WebDavBrowserFragment.kt"
        )

        assertTrue(client.contains("buildPropfindRequest"))
        assertTrue(policy.contains("PROPFIND"))
        assertTrue(credentialStore.contains("EncryptedSharedPreferences"))
        assertTrue(browser.contains("Authorization"))

        assertTrue(doc.contains("WebDavConnectionClient"))
        assertTrue(doc.contains("WebDavConnectionPolicy"))
        assertTrue(doc.contains("WebDavCredentialStore"))
        assertTrue(doc.contains("WebDavBrowserFragment"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
