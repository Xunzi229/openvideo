package com.example.openvideo.build

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CrashReportingBuildConfigTest {

    @Test
    fun feishuWebhookIsInjectedFromLocalPropertiesOrEnvironment() {
        val appBuild = rootFile("app", "build.gradle.kts").readText()

        assertTrue(appBuild.contains("buildConfig = true"))
        assertTrue(appBuild.contains("Properties()"))
        assertTrue(appBuild.contains("local.properties"))
        assertTrue(appBuild.contains("providers.environmentVariable(\"FEISHU_WEBHOOK_URL\")"))
        assertTrue(appBuild.contains("buildConfigField(\"String\", \"FEISHU_WEBHOOK_URL\""))
        assertTrue(appBuild.contains("buildConfigField(\"Boolean\", \"REMOTE_CRASH_REPORTING_ENABLED\""))
    }

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
