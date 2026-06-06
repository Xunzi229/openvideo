package com.example.openvideo.core.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Phase3ReleaseNotesSourceTest {

    @Test
    fun phase3ReleaseNotesDraftCoversUserChangesKnownIssuesAndRollbackPoints() {
        val notes = rootText("docs", "roadmap", "release-notes-v0.4.0-draft.md")

        assertTrue(notes.contains("P3-REL-001"))
        assertTrue(notes.contains("OpenVideo v0.4.0"))
        assertTrue(notes.contains("Release Notes Draft"))
        assertTrue(notes.contains("Network and multi-source playback"))
        assertTrue(notes.contains("URL"))
        assertTrue(notes.contains("HLS"))
        assertTrue(notes.contains("DASH"))
        assertTrue(notes.contains("RTSP"))
        assertTrue(notes.contains("WebDAV"))
        assertTrue(notes.contains("Sources"))
        assertTrue(notes.contains("Privacy"))
        assertTrue(notes.contains("Known issues"))
        assertTrue(notes.contains("Rollback points"))
        assertTrue(notes.contains("Verification"))
        assertTrue(notes.contains("Not included"))
    }

    @Test
    fun releaseNotesLinkPhase3EndDocuments() {
        val notes = rootText("docs", "roadmap", "release-notes-v0.4.0-draft.md")

        assertTrue(notes.contains("network-source-privacy.md"))
        assertTrue(notes.contains("network-protocol-support-matrix.md"))
        assertTrue(notes.contains("webdav-usage-and-limits.md"))
        assertTrue(notes.contains("smb-nas-dlna-research.md"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
