package com.example.openvideo.build

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ReleaseEngineeringSourceTest {

    @Test
    fun repositoryHasLicenseFile() {
        val license = rootFile("LICENSE").readText()

        assertTrue(license.contains("MIT License"))
        assertTrue(license.contains("OpenVideo contributors"))
    }

    @Test
    fun githubAndScriptsAreTrackableButLocalSecretsStayIgnored() {
        val gitignore = rootFile(".gitignore").readText()

        assertFalse(gitignore.lineSequence().any { it.trim() == ".github" })
        assertFalse(gitignore.lineSequence().any { it.trim() == "scripts" })
        assertTrue(gitignore.contains("*.jks"))
        assertTrue(gitignore.contains("*.keystore"))
    }

    @Test
    fun versionAndReleaseSigningAreConfiguredFromPropertiesAndEnvironment() {
        val appBuild = rootFile("app", "build.gradle.kts").readText()
        val gradleProperties = rootFile("gradle.properties").readText()

        assertTrue(gradleProperties.contains("VERSION_CODE="))
        assertTrue(gradleProperties.contains("VERSION_NAME="))
        assertTrue(appBuild.contains("providers.gradleProperty(\"VERSION_CODE\")"))
        assertTrue(appBuild.contains("providers.gradleProperty(\"VERSION_NAME\")"))
        assertTrue(appBuild.contains("OPENVIDEO_RELEASE_STORE_FILE"))
        assertTrue(appBuild.contains("OPENVIDEO_RELEASE_KEY_ALIAS"))
        assertTrue(appBuild.contains("OPENVIDEO_RELEASE_STORE_PASSWORD"))
        assertTrue(appBuild.contains("OPENVIDEO_RELEASE_KEY_PASSWORD"))
    }

    @Test
    fun packageHelperGeneratesReleaseNotesAndChecksums() {
        val helper = rootFile("scripts", "package-helper.ps1").readText()

        assertTrue(helper.contains("RELEASE_NOTES.md"))
        assertTrue(helper.contains("SHA256SUMS.txt"))
        assertTrue(helper.contains("Get-FileHash"))
        assertTrue(helper.contains("Resolve-VersionNameForArtifact"))
    }

    @Test
    fun releaseEngineeringNotesTrackGradle10FollowUp() {
        val notes = rootFile("docs", "release-engineering.md").readText()

        assertTrue(notes.contains("Gradle 9.5"))
        assertTrue(notes.contains("Gradle 10"))
        assertTrue(notes.contains("AGP 8.7.3"))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
