package com.example.openvideo.build

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FeaturePackagingBuildConfigTest {

    @Test
    fun sourcesNavigationIsDisabledByDefaultAndConfigurableForPackaging() {
        val appBuild = rootFile("app", "build.gradle.kts").readText()
        val previewWorkflow = rootFile(".github", "workflows", "preview.yml").readText()
        val releaseWorkflow = rootFile(".github", "workflows", "release.yml").readText()

        assertTrue(appBuild.contains("OPENVIDEO_SOURCES_NAV_ENABLED"))
        assertTrue(appBuild.contains("buildConfigField(\"Boolean\", \"SOURCES_NAV_ENABLED\""))
        assertTrue(appBuild.contains("OPENVIDEO_SOURCES_NAV_ENABLED\", \"false\""))
        assertTrue(previewWorkflow.contains("vars.OPENVIDEO_SOURCES_NAV_ENABLED || 'false'"))
        assertTrue(releaseWorkflow.contains("vars.OPENVIDEO_SOURCES_NAV_ENABLED || 'false'"))
    }

    private fun Path.readText(): String = String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
