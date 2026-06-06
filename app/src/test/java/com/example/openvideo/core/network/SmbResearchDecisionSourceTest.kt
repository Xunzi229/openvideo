package com.example.openvideo.core.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SmbResearchDecisionSourceTest {

    @Test
    fun smbResearchDecisionDocumentRecordsDependencyAndPrototypeDirection() {
        val doc = rootText("docs", "roadmap", "smb-nas-dlna-research.md")

        assertTrue(doc.contains("# SMB/NAS/DLNA Research"))
        assertTrue(doc.contains("P3-SMB-001"))
        assertTrue(doc.contains("Recommended first prototype: SMBJ"))
        assertTrue(doc.contains("Do not add an SMB dependency to the APK in this slice"))
        assertTrue(doc.contains("jcifs-ng"))
        assertTrue(doc.contains("smb-kotlin"))
        assertTrue(doc.contains("libsmb2"))
        assertTrue(doc.contains("License"))
        assertTrue(doc.contains("Android compatibility"))
        assertTrue(doc.contains("Performance / streaming risk"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
