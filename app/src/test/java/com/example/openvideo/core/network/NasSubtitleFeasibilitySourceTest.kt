package com.example.openvideo.core.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class NasSubtitleFeasibilitySourceTest {

    @Test
    fun webDavSubtitleMatcherDelegatesToProtocolAgnosticRemoteMatcher() {
        val webDavSource = rootText("app", "src", "main", "java", "com", "example", "openvideo", "core", "network", "WebDavSubtitleMatcher.kt")

        assertTrue(webDavSource.contains("RemoteSidecarSubtitleMatcher.Item("))
        assertTrue(webDavSource.contains("RemoteSidecarSubtitleMatcher.matchForVideo("))
    }

    @Test
    fun smbResearchDocumentRecordsNasSubtitlePrototypeDecision() {
        val doc = rootText("docs", "roadmap", "smb-nas-dlna-research.md")

        assertTrue(doc.contains("P3-SMB-002"))
        assertTrue(doc.contains("RemoteSidecarSubtitleMatcher"))
        assertTrue(doc.contains("Path encoding"))
        assertTrue(doc.contains("Permission model"))
        assertTrue(doc.contains("same directory only"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
