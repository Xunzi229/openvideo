package com.example.openvideo.ui.sources

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class WebDavSourceSelectionSourceTest {

    @Test
    fun gradleUsesOkHttpForWebDavPropfindAndMockWebServerForTests() {
        val versions = rootText("gradle", "libs.versions.toml")
        val appBuild = rootText("app", "build.gradle.kts")

        assertTrue(versions.contains("""okhttp = "5.3.2""""))
        assertTrue(versions.contains("""okhttp = { group = "com.squareup.okhttp3", name = "okhttp""""))
        assertTrue(versions.contains("""okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver""""))
        assertTrue(appBuild.contains("implementation(libs.okhttp)"))
        assertTrue(appBuild.contains("testImplementation(libs.okhttp.mockwebserver)"))
    }

    @Test
    fun dependencyDecisionIsDocumentedWithLicenseAndRisk() {
        val decision = rootText("docs", "roadmap", "webdav-dependency-selection.md")
        val phase = rootText("docs", "roadmap", "phases", "phase-3-network-sources", "README.md")

        assertTrue(decision.contains("P3-WD-001"))
        assertTrue(decision.contains("OkHttp"))
        assertTrue(decision.contains("PROPFIND"))
        assertTrue(decision.contains("Apache-2.0"))
        assertTrue(decision.contains("MockWebServer"))
        assertTrue(decision.contains("维护风险"))
        assertTrue(phase.contains("| P3-WD-001 | P1 | ✅ WebDAV 依赖选型"))
    }

    @Test
    fun sourcesFragmentOpensWebDavDialogInsteadOfPlannedToast() {
        val source = sourceText("SourcesFragment.kt")
        val strings = rootText("app", "src", "main", "res", "values", "strings.xml")

        val webDavBlock = source.substringAfter("R.id.row_source_webdav")
            .substringBefore("view.findViewById<View>(R.id.row_source_future)")
        assertTrue(webDavBlock.contains("WebDavSourceDialog.show("))
        assertTrue(webDavBlock.contains("repository.addWebDavSource("))
        assertTrue(strings.contains("webdav_add_title"))
        assertTrue(strings.contains("webdav_test_success"))
    }

    @Test
    fun webDavSourceDialogRequestsNameInputDefaultFocusForRemoteUse() {
        val source = sourceText("WebDavSourceDialog.kt")

        assertTrue(source.contains("nameInput.post"))
        assertTrue(source.contains("nameInput.requestFocus()"))
    }

    private fun sourceText(name: String): String =
        rootText("app", "src", "main", "java", "com", "example", "openvideo", "ui", "sources", name)

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
