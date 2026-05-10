package com.example.openvideo.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SettingsVersionSourceTest {

    @Test
    fun settingsFragmentBindsDisplayedVersionFromInstalledPackage() {
        val source = String(Files.readAllBytes(settingsFragmentSource()))

        assertTrue(source.contains("R.id.tv_version"))
        assertTrue(source.contains("viewModel.installedVersionName()"))
    }

    @Test
    fun settingsLayoutDoesNotHardcodeVersionName() {
        val source = String(Files.readAllBytes(settingsLayoutSource()))

        assertFalse(source.contains("""android:text="1.0.0""""))
    }

    private fun settingsFragmentSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "settings",
            "SettingsFragment.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun settingsLayoutSource(): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", "fragment_settings.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
