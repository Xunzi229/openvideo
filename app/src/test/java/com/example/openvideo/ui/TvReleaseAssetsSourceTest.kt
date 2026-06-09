package com.example.openvideo.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TvReleaseAssetsSourceTest {

    @Test
    fun tvReleaseAssetsDocumentCoversStoreArtworkAndRemoteHelpRequirements() {
        val docPath = rootFile("docs", "roadmap", "tv-release-assets.md")

        assertTrue("TV release assets roadmap document should exist", Files.exists(docPath))

        val doc = String(Files.readAllBytes(docPath))
        assertTrue(doc.contains("P5-TV-ASSETS-001"))
        assertTrue(doc.contains("Android TV Release Assets"))
        assertTrue(doc.contains("TV banner"))
        assertTrue(doc.contains("TV icon"))
        assertTrue(doc.contains("Store screenshots"))
        assertTrue(doc.contains("Remote help artwork"))
        assertTrue(doc.contains("Current code anchors"))
        assertTrue(doc.contains("bg_tv_banner"))
        assertTrue(doc.contains("android:banner"))
        assertTrue(doc.contains("LEANBACK_LAUNCHER"))
        assertTrue(doc.contains("tv-regression-matrix.md"))
        assertTrue(doc.contains("android-tv-known-limits.md"))
        assertTrue(doc.contains("not final store screenshots"))
    }

    @Test
    fun tvReleaseAssetsDocumentStaysAlignedWithManifestAndBannerBaseline() {
        val docPath = rootFile("docs", "roadmap", "tv-release-assets.md")
        val manifest = rootText("app", "src", "main", "AndroidManifest.xml")
        val banner = rootText("app", "src", "main", "res", "drawable", "bg_tv_banner.xml")

        assertTrue("TV release assets roadmap document should exist", Files.exists(docPath))

        val doc = String(Files.readAllBytes(docPath))
        assertTrue(manifest.contains("""android:banner="@drawable/bg_tv_banner""""))
        assertTrue(manifest.contains("android.intent.category.LEANBACK_LAUNCHER"))
        assertTrue(banner.contains("""android:width="320dp""""))
        assertTrue(banner.contains("""android:height="180dp""""))

        assertTrue(doc.contains("""android:banner="@drawable/bg_tv_banner""""))
        assertTrue(doc.contains("android.intent.category.LEANBACK_LAUNCHER"))
        assertTrue(doc.contains("320dp x 180dp"))
        assertTrue(doc.contains("placeholder"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).firstOrNull(Files::exists)
                    ?: relative
            }
}
