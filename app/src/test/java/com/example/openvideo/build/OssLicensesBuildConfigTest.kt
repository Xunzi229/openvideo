package com.example.openvideo.build

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class OssLicensesBuildConfigTest {

    @Test
    fun ossLicensesArePackagedWithoutBuildTimePluginScan() {
        val rootBuild = rootFile("build.gradle.kts").readText()
        val appBuild = rootFile("app", "build.gradle.kts").readText()
        val versions = rootFile("gradle", "libs.versions.toml").readText()

        assertFalse(rootBuild.contains("gms.oss.licenses"))
        assertFalse(appBuild.contains("gms.oss.licenses"))
        assertFalse(versions.contains("ossLicensesPlugin"))
        assertFalse(versions.contains("gms-oss-licenses"))
        assertTrue(appBuild.contains("implementation(libs.play.services.oss.licenses)"))
        assertTrue(rootFile("app", "src", "main", "res", "raw", "third_party_licenses").hasContent())
        assertTrue(rootFile("app", "src", "main", "res", "raw", "third_party_license_metadata").hasContent())
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun Path.hasContent(): Boolean =
        Files.exists(this) && Files.size(this) > 0L

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
