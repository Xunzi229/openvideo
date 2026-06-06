package com.example.openvideo.ui.sources

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SourceCredentialPrivacySourceTest {

    @Test
    fun sourcesAndDetailLayoutsExposeCredentialPrivacyNotice() {
        val sourcesLayout = resourceText("layout", "fragment_sources.xml")
        val detailLayout = resourceText("layout", "fragment_source_detail.xml")
        val fragment = sourceText("sources", "SourcesFragment.kt")
        val detailFragment = sourceText("sources", "SourceDetailFragment.kt")

        assertTrue(sourcesLayout.contains("""android:id="@+id/tv_source_privacy_notice""""))
        assertTrue(detailLayout.contains("""android:id="@+id/tv_source_detail_privacy_notice""""))
        assertTrue(fragment.contains("SourceCredentialPrivacyPolicy.buildNotice("))
        assertTrue(detailFragment.contains("SourceCredentialPrivacyPolicy.buildNotice("))
    }

    @Test
    fun stringsAndDocsExplainCredentialStorageAndExportPolicy() {
        val strings = resourceText("values", "strings.xml")
        val docs = loadText(Paths.get("docs", "roadmap", "network-source-privacy.md"))

        assertTrue(strings.contains("source_privacy_title"))
        assertTrue(strings.contains("source_privacy_storage"))
        assertTrue(strings.contains("source_privacy_export"))
        assertTrue(strings.contains("source_privacy_diagnostics"))
        assertTrue(docs.contains("Android Keystore"))
        assertTrue(docs.contains("EncryptedSharedPreferences"))
        assertTrue(docs.contains("Settings export"))
        assertTrue(docs.contains("passwords, tokens, cookies, and headers"))
    }

    @Test
    fun settingsBackupAllowlistAlreadyBlocksSensitiveNetworkMarkers() {
        val allowlist = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "core", "prefs", "SettingsBackupAllowlistPolicy.kt")
        )

        assertTrue(allowlist.contains("\"password\""))
        assertTrue(allowlist.contains("\"token\""))
        assertTrue(allowlist.contains("\"cookie\""))
        assertTrue(allowlist.contains("\"authorization\""))
    }

    private fun sourceText(vararg parts: String): String =
        loadText(Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", *parts))

    private fun resourceText(dir: String, name: String): String =
        loadText(Paths.get("src", "main", "res", dir, name))

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath),
            Paths.get("..").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
