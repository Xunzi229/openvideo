package com.example.openvideo.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SettingsCrashReportingSourceTest {

    @Test
    fun crashReportingIsBuildConfiguredAndNotExposedAsAUserToggle() {
        val layout = rootText("app", "src", "main", "res", "layout", "fragment_settings.xml")
        val fragment = rootText(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "settings",
            "SettingsFragment.kt"
        )
        val viewModel = rootText(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "settings",
            "SettingsViewModel.kt"
        )
        val crashLogger = rootText(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "diagnostics",
            "CrashLogger.kt"
        )
        val appPrefs = rootText(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "prefs",
            "AppPrefs.kt"
        )

        listOf(layout, fragment, viewModel).forEach { source ->
            assertFalse(source.contains("switch_crash_reporting"))
            assertFalse(source.contains("remoteCrashReportingEnabled"))
        }
        assertTrue(crashLogger.contains("BuildConfig.REMOTE_CRASH_REPORTING_ENABLED"))
        assertTrue(crashLogger.contains("BuildConfig.FEISHU_WEBHOOK_URL.isNotBlank()"))
        assertFalse(crashLogger.contains("AppPrefs("))
        assertFalse(appPrefs.contains("remote_crash_reporting_enabled"))
    }

    private fun rootText(vararg parts: String): String = String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
