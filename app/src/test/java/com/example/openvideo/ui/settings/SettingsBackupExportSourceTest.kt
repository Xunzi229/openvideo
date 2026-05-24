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
        val policy = settingsBackupUiPolicySource()
        val deferredRule = rootFile("design", "rules", "settings-backup-deferred.md").readText()
        val phasePlan = rootFile(
            "docs",
            "roadmap",
            "phases",
            "phase-0-stability-architecture",
            "README.md"
        ).readText()

        assertTrue(fragment.contains("bindBackupSection(view)"))
        assertTrue(fragment.contains("SettingsBackupUiPolicy.SETTINGS_EXPORT_ENTRY_VISIBLE"))
        assertTrue(policy.contains("SETTINGS_EXPORT_ENTRY_VISIBLE = true"))
        assertTrue(policy.contains("SETTINGS_IMPORT_ENTRY_VISIBLE = true"))
        assertTrue(deferredRule.contains("SETTINGS_EXPORT_ENTRY_VISIBLE = true"))
        assertTrue(deferredRule.contains("SETTINGS_IMPORT_ENTRY_VISIBLE = true"))
        assertTrue(phasePlan.contains("Sprint 0.3 / 0.3.3 导出/导入实现"))
        assertTrue(phasePlan.contains("设置页入口已开放"))
        assertTrue(fragment.contains("ActivityResultContracts.CreateDocument(SettingsBackupSchema.MIME_TYPE)"))
        assertTrue(fragment.contains("exportSettingsLauncher.launch(SettingsBackupSchema.SUGGESTED_FILENAME)"))
        assertTrue(fragment.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(fragment.contains("importSettingsLauncher.launch(arrayOf(SettingsBackupSchema.MIME_TYPE, \"*/*\"))"))
        assertTrue(fragment.contains("viewModel.writeSettingsExportTo(requireContext(), uri)"))
        assertTrue(fragment.contains("viewModel.readAndImportSettings(requireContext(), uri)"))
        assertTrue(fragment.contains("R.id.settings_backup_section"))
        assertTrue(fragment.contains("R.id.row_export_settings"))
        assertTrue(fragment.contains("R.id.row_import_settings"))
        assertTrue(viewModel.contains("SettingsBackupExporter.exportJson(playerPrefs, appPrefs)"))
        assertTrue(viewModel.contains("SettingsBackupFileWriter.writeJson(context.contentResolver, uri, json)"))
        assertTrue(viewModel.contains("SettingsBackupImporter.apply(result.document, playerPrefs, appPrefs)"))
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

    private fun settingsBackupUiPolicySource(): String = loadSource(
        "ui", "settings", "SettingsBackupUiPolicy.kt"
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

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)

    private fun Path.readText(): String =
        String(Files.readAllBytes(this))
}
