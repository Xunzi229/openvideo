package com.example.openvideo.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SettingsBackupExportSourceTest {

    @Test
    fun settingsFragmentUsesCreateDocumentExportFlow() {
        val fragment = settingsFragmentSource()
        val viewModel = settingsViewModelSource()

        assertTrue(fragment.contains("ActivityResultContracts.CreateDocument(SettingsBackupSchema.MIME_TYPE)"))
        assertTrue(fragment.contains("exportSettingsLauncher.launch(SettingsBackupSchema.SUGGESTED_FILENAME)"))
        assertTrue(fragment.contains("viewModel.writeSettingsExportTo(requireContext(), uri)"))
        assertTrue(fragment.contains("R.string.settings_toast_export_success"))
        assertTrue(fragment.contains("R.string.settings_toast_export_failed"))
        assertTrue(fragment.contains("R.id.row_export_settings"))
        assertTrue(viewModel.contains("SettingsBackupExporter.exportJson(playerPrefs, appPrefs)"))
        assertTrue(viewModel.contains("SettingsBackupFileWriter.writeJson(context.contentResolver, uri, json)"))
    }

    @Test
    fun fileWriterUsesContentResolverOutputStream() {
        val source = fileWriterSource()
        assertTrue(source.contains("openOutputStream(uri)"))
        assertTrue(source.contains("Charsets.UTF_8"))
    }

    private fun settingsFragmentSource(): String = loadSource(
        "ui", "settings", "SettingsFragment.kt"
    )

    private fun settingsViewModelSource(): String = loadSource(
        "ui", "settings", "SettingsViewModel.kt"
    )

    private fun fileWriterSource(): String = loadSource(
        "core", "prefs", "SettingsBackupFileWriter.kt"
    )

    private fun loadSource(vararg parts: String): String {
        val relative = Paths.get("src", "main", "java", "com", "example", "openvideo", *parts)
        val path = sequenceOf(
            relative,
            Paths.get("app").resolve(relative)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
