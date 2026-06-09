package com.example.openvideo.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MainActivityTvModeSourceTest {

    @Test
    fun mainActivityComputesTvModeFromDeviceConfigurationAndLeanbackFeature() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "MainActivity.kt")
        )

        assertTrue(source.contains("import android.content.pm.PackageManager"))
        assertTrue(source.contains("var isTvMode: Boolean = false"))
        assertTrue(source.contains("MainActivityTvModePolicy.isTvMode("))
        assertTrue(source.contains("resources.configuration.uiMode"))
        assertTrue(source.contains("packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)"))
        assertTrue(source.contains("isTvMode = computeTvMode()"))
        assertTrue(source.contains("override fun onConfigurationChanged(newConfig: android.content.res.Configuration)"))
    }

    @Test
    fun tvModePolicyUsesTelevisionUiModeAndLeanbackFeatureOnly() {
        val source = loadText(
            Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "MainActivityTvModePolicy.kt")
        )

        assertTrue(source.contains("Configuration.UI_MODE_TYPE_MASK"))
        assertTrue(source.contains("Configuration.UI_MODE_TYPE_TELEVISION"))
        assertTrue(source.contains("hasLeanbackFeature"))
    }

    private fun loadText(relativePath: Path): String {
        val path = sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
