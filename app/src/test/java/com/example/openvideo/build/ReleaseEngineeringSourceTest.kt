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
        assertTrue(appBuild.contains("debug {"))
        assertTrue(appBuild.contains("signingConfig = signingConfigs.getByName(\"release\")"))
        assertTrue(appBuild.contains("runningOnCi && !releaseSigningConfigured"))
        assertTrue(appBuild.contains("CI packaging must use GitHub Actions OPENVIDEO_RELEASE_* secrets"))
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
    fun releaseScriptTestsCoverReleaseArtifacts() {
        val testScript = rootFile("scripts", "tests", "Test-OpenVideoRelease.ps1").readText()

        assertTrue(testScript.contains("Get-OpenVideoProjectVersion"))
        assertTrue(testScript.contains("SHA256SUMS.txt"))
    }

    @Test
    fun githubPreviewWorkflowBuildsAndUploadsSignedReleaseApk() {
        val appBuild = rootFile("app", "build.gradle.kts").readText()
        val workflow = rootFile(".github", "workflows", "preview.yml").readText()

        assertTrue(workflow.contains("workflow_dispatch:"))
        assertTrue(workflow.contains("codex/**"))
        assertTrue(workflow.contains(":app:testDebugUnitTest"))
        assertTrue(workflow.contains("OPENVIDEO_RELEASE_KEYSTORE_BASE64"))
        assertTrue(workflow.contains("OPENVIDEO_RELEASE_CERT_SHA256"))
        assertTrue(workflow.contains("agentRulesRequireOfficialReleaseSigningOnCiPackaging"))
        assertTrue(workflow.contains("OverlayInsetsSourceTest"))
        assertTrue(appBuild.contains("runningOnCi && !releaseSigningConfigured"))
        assertTrue(workflow.contains(":app:assembleRelease"))
        assertTrue(workflow.contains("actions/upload-artifact@v4"))
        assertTrue(workflow.contains("apksigner verify"))
        assertTrue(workflow.contains("preview/*.apk"))
        assertTrue(workflow.contains("arm64-v8a"))
        assertTrue(workflow.contains("x86_64"))
        assertTrue(workflow.contains("gh run download"))
        assertTrue(workflow.contains("GH_TOKEN"))
        assertTrue(workflow.contains("publish_release:"))
        assertTrue(workflow.contains("github.event_name == 'workflow_dispatch' && inputs.publish_release"))
        assertTrue(workflow.contains("\"--prerelease\""))
        assertTrue(workflow.contains("gh @arguments"))
        assertTrue(workflow.contains("openvideo-preview-${'$'}shortSha-${'$'}label.apk"))
        assertFalse(workflow.contains("openvideo-preview-${'$'}env:GITHUB_SHA-${'$'}label.apk"))
        assertFalse(workflow.contains(":app:assembleDebug"))
        assertFalse(workflow.contains("app-debug.apk"))
        assertTrue(appBuild.contains("baseline = file(\"lint-baseline.xml\")"))
        assertTrue(appBuild.contains("OPENVIDEO_ABI_SPLITS"))
        assertTrue(appBuild.contains("isEnable = abiSplitsEnabled"))
        assertTrue(appBuild.contains("include(\"armeabi-v7a\", \"arm64-v8a\", \"x86\", \"x86_64\")"))
        assertTrue(appBuild.contains("isUniversalApk = true"))
        assertTrue(workflow.contains("-POPENVIDEO_ABI_SPLITS=true"))
    }

    @Test
    fun agentRulesRequireOfficialReleaseSigningOnCiPackaging() {
        val rule = rootFile(".cursor", "rules", "apk-release-signing.mdc").readText()
        val agents = rootFile("AGENTS.md").readText()
        val claude = rootFile("CLAUDE.md").readText()
        val previewWorkflow = rootFile(".github", "workflows", "preview.yml").readText()
        val releaseWorkflow = rootFile(".github", "workflows", "release.yml").readText()

        assertTrue(rule.contains("alwaysApply: true"))
        assertTrue(rule.contains("OPENVIDEO_RELEASE_KEYSTORE_BASE64"))
        assertTrue(rule.contains(":app:assembleRelease"))
        assertTrue(rule.contains("assembleDebug"))
        assertTrue(agents.contains("OPENVIDEO_RELEASE_*"))
        assertTrue(claude.contains("OPENVIDEO_RELEASE_*"))
        assertTrue(agents.contains(".cursor/rules/apk-release-signing.mdc"))
        assertTrue(claude.contains(".cursor/rules/apk-release-signing.mdc"))
        assertTrue(previewWorkflow.contains("OPENVIDEO_RELEASE_KEYSTORE_BASE64"))
        assertTrue(releaseWorkflow.contains("OPENVIDEO_RELEASE_KEYSTORE_BASE64"))
        assertFalse(previewWorkflow.contains(":app:assembleDebug"))
        assertFalse(releaseWorkflow.contains(":app:assembleDebug"))
    }

    @Test
    fun githubReleaseWorkflowRestoresSigningKeyAndPublishesSignedApk() {
        val appBuild = rootFile("app", "build.gradle.kts").readText()
        val localPropertiesExample = rootFile("local.properties.example").readText()
        val previewWorkflow = rootFile(".github", "workflows", "preview.yml").readText()
        val workflow = rootFile(".github", "workflows", "release.yml").readText()
        val webhookSecret = "FEISHU_WEBHOOK_URL: ${'$'}{{ secrets.FEISHU_WEBHOOK_URL }}"

        assertTrue(appBuild.contains("providers.environmentVariable(\"FEISHU_WEBHOOK_URL\")"))
        assertTrue(appBuild.contains("localProperties.getProperty(\"FEISHU_WEBHOOK_URL\", \"\")"))
        assertTrue(localPropertiesExample.lineSequence().any { it.trim() == "FEISHU_WEBHOOK_URL=" })
        assertFalse(localPropertiesExample.contains("open-apis/bot"))
        assertTrue(previewWorkflow.contains(webhookSecret))
        assertTrue(workflow.contains(webhookSecret))
        assertTrue(workflow.contains("\"FEISHU_WEBHOOK_URL\","))
        assertTrue(workflow.contains("OPENVIDEO_RELEASE_KEYSTORE_BASE64"))
        assertTrue(workflow.contains("OPENVIDEO_RELEASE_STORE_PASSWORD"))
        assertTrue(workflow.contains("OPENVIDEO_RELEASE_KEY_ALIAS"))
        assertTrue(workflow.contains("OPENVIDEO_RELEASE_KEY_PASSWORD"))
        assertTrue(workflow.contains("OPENVIDEO_RELEASE_CERT_SHA256"))
        assertTrue(workflow.contains(":app:assembleRelease"))
        assertTrue(workflow.contains("-POPENVIDEO_ABI_SPLITS=true"))
        assertTrue(workflow.contains("apksigner verify"))
        assertTrue(workflow.contains("gh @arguments"))
        assertTrue(workflow.contains("release/*.apk"))
        assertTrue(workflow.contains("publish_release:"))
        assertTrue(workflow.contains("replace_existing_release:"))
        assertTrue(workflow.contains("if: github.event_name == 'push' || inputs.publish_release"))
        assertTrue(workflow.contains("Select-Object -First 1"))
        assertFalse(workflow.contains("Select-Object -Single"))
        assertTrue(workflow.contains("gh release delete"))
        assertTrue(workflow.contains("gh release view"))
        assertTrue(workflow.contains("git ls-remote --exit-code --tags origin"))
        assertTrue(workflow.contains("git push origin --delete"))
        assertFalse(workflow.contains("--cleanup-tag"))
        assertTrue(workflow.contains("Open Video ${'$'}env:RELEASE_TAG"))
        assertFalse(Regex("STORE_PASSWORD:\\s*\"[^\"]+\"").containsMatchIn(workflow))
        assertFalse(Regex("KEY_PASSWORD:\\s*\"[^\"]+\"").containsMatchIn(workflow))
    }

    @Test
    fun localSigningWrapperDoesNotContainSigningPasswords() {
        val wrapper = rootFile("scripts", "sign-release-default.ps1").readText()
        val secretSetup = rootFile("scripts", "configure-github-release-secrets.ps1").readText()

        assertTrue(wrapper.contains("OPENVIDEO_RELEASE_STORE_PASSWORD"))
        assertTrue(wrapper.contains("OPENVIDEO_RELEASE_KEY_PASSWORD"))
        assertFalse(Regex("StorePassword\\s*=\\s*\"[^\"]+\"").containsMatchIn(wrapper))
        assertFalse(Regex("KeyPassword\\s*=\\s*\"[^\"]+\"").containsMatchIn(wrapper))
        assertTrue(secretSetup.contains("OPENVIDEO_RELEASE_KEYSTORE_BASE64"))
        assertTrue(secretSetup.contains("Read-Host -AsSecureString"))
    }

    @Test
    fun androidGradlePluginAvoidsGradle10MultiStringDependencyDeprecation() {
        val versions = rootFile("gradle", "libs.versions.toml").readText()

        assertTrue(versions.contains("agp = \"9.0.1\""))
        assertTrue(versions.contains("kotlin = \"2.2.10\""))
        assertTrue(versions.contains("ksp = \"2.3.7\""))
        assertTrue(versions.contains("hilt = \"2.59.1\""))
        assertTrue(versions.contains("room = \"2.8.3\""))
        assertFalse(versions.contains("agp = \"8.7.3\""))

        val rootBuild = rootFile("build.gradle.kts").readText()
        val appBuild = rootFile("app", "build.gradle.kts").readText()
        val gradleProperties = rootFile("gradle.properties").readText()
        assertFalse(rootBuild.contains("libs.plugins.kotlin.android"))
        assertFalse(appBuild.contains("libs.plugins.kotlin.android"))
        assertFalse(appBuild.contains("compilerOptions"))
        assertFalse(gradleProperties.contains("android.disallowKotlinSourceSets"))
        assertFalse(gradleProperties.contains("android.builtInKotlin=false"))
    }

    @Test
    fun releaseEngineeringNotesTrackGradle10FollowUp() {
        val notes = rootFile("docs", "release-engineering.md").readText()

        assertTrue(notes.contains("Gradle 9.5"))
        assertTrue(notes.contains("Gradle 10"))
        assertTrue(notes.contains("AGP 9.0.1"))
        assertTrue(notes.contains("Kotlin 2.2.10"))
        assertTrue(notes.contains("KSP 2.3.7"))
        assertTrue(notes.contains("Room 2.8.3"))
        assertTrue(notes.contains("Dagger/Hilt 2.59.1"))
        assertFalse(notes.contains("android.disallowKotlinSourceSets"))
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
            assertTrue(readme.contains("2.3.7"))
            assertFalse(readme.contains("android.disallowKotlinSourceSets"))
        }
    }

    @Test
    fun roadmapTracksRemainingBuildCompatibilityFollowUp() {
        val roadmap = rootFile("docs", "roadmap", "player-optimization-roadmap.md").readText()

        assertTrue(roadmap.contains("--warning-mode fail"))
        assertFalse(roadmap.contains("android.disallowKotlinSourceSets"))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
