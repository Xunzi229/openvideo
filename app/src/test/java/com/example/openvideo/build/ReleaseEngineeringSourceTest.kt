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
    fun releaseModuleCentralizesVersionAndArtifacts() {
        val module = rootFile("scripts", "OpenVideo.Release.psm1").readText()

        assertTrue(module.contains("Get-OpenVideoProjectVersion"))
        assertTrue(module.contains("Resolve-OpenVideoVersionName"))
        assertTrue(module.contains("Write-OpenVideoReleaseChecksums"))
        assertTrue(module.contains("Write-OpenVideoReleaseNotes"))
        assertTrue(module.contains("gradle.properties"))
    }

    @Test
    fun packageHelperUsesSharedReleaseModule() {
        val helper = rootFile("scripts", "package-helper.ps1").readText()

        assertTrue(helper.contains("OpenVideo.Release.psm1"))
        assertTrue(helper.contains("Write-OpenVideoReleaseChecksums"))
        assertTrue(helper.contains("Write-OpenVideoReleaseNotes"))
        assertFalse(helper.contains("Resolve-VersionNameForArtifact"))
    }

    @Test
    fun signingScriptUsesSharedReleaseModule() {
        val script = rootFile("scripts", "sign-release.ps1").readText()

        assertTrue(script.contains("OpenVideo.Release.psm1"))
        assertTrue(script.contains("Resolve-OpenVideoVersionName"))
        assertFalse(script.contains("Resolve-AppVersionName"))
    }

    @Test
    fun releaseScriptTestsAndCiAreWired() {
        val testScript = rootFile("scripts", "tests", "Test-OpenVideoRelease.ps1").readText()
        val workflow = rootFile(".github", "workflows", "android-ci.yml").readText()

        assertTrue(testScript.contains("Get-OpenVideoProjectVersion"))
        assertTrue(testScript.contains("SHA256SUMS.txt"))
        assertTrue(workflow.contains("Test-OpenVideoRelease.ps1"))
        assertTrue(workflow.contains("--warning-mode fail"))
    }

    @Test
    fun androidGradlePluginAvoidsGradle10MultiStringDependencyDeprecation() {
        val versions = rootFile("gradle", "libs.versions.toml").readText()

        assertTrue(versions.contains("agp = \"9.0.1\""))
        assertTrue(versions.contains("kotlin = \"2.2.10\""))
        assertTrue(versions.contains("ksp = \"2.2.10-2.0.2\""))
        assertTrue(versions.contains("hilt = \"2.59.1\""))
        assertTrue(versions.contains("room = \"2.8.3\""))
        assertFalse(versions.contains("agp = \"8.7.3\""))

        val rootBuild = rootFile("build.gradle.kts").readText()
        val appBuild = rootFile("app", "build.gradle.kts").readText()
        val gradleProperties = rootFile("gradle.properties").readText()
        assertFalse(rootBuild.contains("libs.plugins.kotlin.android"))
        assertFalse(appBuild.contains("libs.plugins.kotlin.android"))
        assertFalse(appBuild.contains("compilerOptions"))
        assertTrue(gradleProperties.contains("android.disallowKotlinSourceSets=false"))
        assertFalse(gradleProperties.contains("android.builtInKotlin=false"))
    }

    @Test
    fun releaseEngineeringNotesTrackGradle10FollowUp() {
        val notes = rootFile("docs", "release-engineering.md").readText()

        assertTrue(notes.contains("Gradle 9.5"))
        assertTrue(notes.contains("Gradle 10"))
        assertTrue(notes.contains("AGP 9.0.1"))
        assertTrue(notes.contains("Kotlin 2.2.10"))
        assertTrue(notes.contains("KSP 2.2.10-2.0.2"))
        assertTrue(notes.contains("Room 2.8.3"))
        assertTrue(notes.contains("Dagger/Hilt 2.59.1"))
        assertTrue(notes.contains("android.disallowKotlinSourceSets=false"))
        assertTrue(notes.contains("--warning-mode fail"))
    }

    @Test
    fun publicReadmesTrackCurrentBuildBaseline() {
        val english = rootFile("README.md").readText()
        val chinese = rootFile("README.zh-CN.md").readText()

        listOf(english, chinese).forEach { readme ->
            assertTrue(readme.contains("Gradle **9.5**"))
            assertTrue(readme.contains("9.0.1"))
            assertTrue(readme.contains("2.2.10"))
            assertTrue(readme.contains("2.2.10-2.0.2"))
            assertTrue(readme.contains("android.disallowKotlinSourceSets=false"))
        }
    }

    @Test
    fun roadmapTracksRemainingBuildCompatibilityFollowUp() {
        val roadmap = rootFile("docs", "roadmap", "player-optimization-roadmap.md").readText()

        assertTrue(roadmap.contains("--warning-mode fail"))
        assertTrue(roadmap.contains("android.disallowKotlinSourceSets=false"))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
