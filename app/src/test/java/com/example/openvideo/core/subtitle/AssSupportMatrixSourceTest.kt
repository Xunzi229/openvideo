package com.example.openvideo.core.subtitle

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AssSupportMatrixSourceTest {

    @Test
    fun assSupportMatrixDocumentsCurrentParserAndDeferredRenderingScope() {
        val docPath = rootFile("docs", "roadmap", "ass-support-matrix.md")
        assertTrue("ASS support matrix doc is missing", Files.exists(docPath))

        val doc = rootText(docPath)

        assertTrue(doc.contains("P4-ASS-001"))
        assertTrue(doc.contains(".ass / .ssa"))
        assertTrue(doc.contains("[Events]"))
        assertTrue(doc.contains("Dialogue:"))
        assertTrue(doc.contains("Start / End"))
        assertTrue(doc.contains("\\N / \\n"))
        assertTrue(doc.contains("override tags"))
        assertTrue(doc.contains("[V4+ Styles]"))
        assertTrue(doc.contains("Fontname / Fontsize"))
        assertTrue(doc.contains("PrimaryColour / SecondaryColour"))
        assertTrue(doc.contains("Outline / Shadow"))
        assertTrue(doc.contains("Alignment / MarginL / MarginR / MarginV"))
        assertTrue(doc.contains("\\pos / \\move"))
        assertTrue(doc.contains("Karaoke"))
        assertTrue(doc.contains("Drawing / vector clips"))
        assertTrue(doc.contains("Transform / fade / animation"))
        assertTrue(doc.contains("Layer / collision"))
        assertTrue(doc.contains("WebDAV / NAS"))
        assertTrue(doc.contains("P4-ASS-002"))
        assertTrue(doc.contains("SubtitleCueStyle"))
        assertTrue(doc.contains("style metadata"))
        assertTrue(doc.contains("conservatively applied to TextView rendering"))
    }

    @Test
    fun assSupportMatrixMatchesCurrentCodePaths() {
        val docPath = rootFile("docs", "roadmap", "ass-support-matrix.md")
        assertTrue("ASS support matrix doc is missing", Files.exists(docPath))

        val doc = rootText(docPath)
        val parser = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "subtitle",
            "AssParser.kt"
        )
        val loader = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "subtitle",
            "SubtitleLoader.kt"
        )
        val item = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "subtitle",
            "SubtitleItem.kt"
        )
        val cueStylePolicy = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerSubtitleCueStylePolicy.kt"
        )
        val webDavGuide = rootText("docs", "roadmap", "webdav-usage-and-limits.md")
        val nasResearch = rootText("docs", "roadmap", "smb-nas-dlna-research.md")

        assertTrue(parser.contains("trimmed.startsWith(\"[Events]\", ignoreCase = true)"))
        assertTrue(parser.contains("trimmed.startsWith(\"Dialogue:\")"))
        assertTrue(parser.contains("parseStyle(trimmed, styleFormat)"))
        assertTrue(parser.contains("split(\",\", limit = format.size)"))
        assertTrue(parser.contains(".replace(\"\\\\N\", \"\\n\")"))
        assertTrue(parser.contains("Regex(\"\\\\{[^}]*\\\\}\")"))
        assertTrue(loader.contains("\"ass\", \"ssa\" -> AssParser.parse(content)"))
        assertTrue(item.contains("data class SubtitleCueStyle"))
        assertTrue(item.contains("val style: SubtitleCueStyle? = null"))
        assertTrue(cueStylePolicy.contains("PlayerSubtitleCueStylePolicy"))
        assertTrue(cueStylePolicy.contains("textView.setTextSize"))
        assertTrue(cueStylePolicy.contains("textView.setShadowLayer"))
        assertTrue(webDavGuide.contains("ASS/SSA remote subtitle matching is deferred"))
        assertTrue(nasResearch.contains("defer ASS/SSA"))

        assertTrue(doc.contains("AssParser.parse"))
        assertTrue(doc.contains("SubtitleLoader"))
        assertTrue(doc.contains("remote sidecar matching is deferred"))
    }

    private fun rootText(path: Path): String =
        String(Files.readAllBytes(path))

    private fun rootText(vararg parts: String): String =
        rootText(rootFile(*parts))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative))
                    .firstOrNull(Files::exists) ?: relative
            }
}
