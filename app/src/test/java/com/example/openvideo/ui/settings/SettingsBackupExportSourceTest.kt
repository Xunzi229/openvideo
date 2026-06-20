package com.example.openvideo.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SettingsBackupExportSourceTest {

    @Test
    fun settingsFragmentKeepsBackupInfrastructureBehindHiddenUiSwitch() {
        val fragment = settingsFragmentSource()
        val viewModel = settingsViewModelSource()
        val policy = settingsBackupUiPolicySource()
        val deferredRule = readRootFile("design", "rules", "settings-backup-deferred.md")
        val phasePlan = rootFile(
            "docs",
            "roadmap",
            "phases",
            "phase-0-stability-architecture",
            "README.md"
        ).let { String(Files.readAllBytes(it)) }

        assertTrue(fragment.contains("bindBackupSection(view, tvMode = (activity as? MainActivity)?.isTvMode == true)"))
        assertTrue(fragment.contains("SettingsBackupUiPolicy.SETTINGS_EXPORT_ENTRY_VISIBLE"))
        assertTrue(policy.contains("SETTINGS_EXPORT_ENTRY_VISIBLE = false"))
        assertTrue(policy.contains("SETTINGS_IMPORT_ENTRY_VISIBLE = false"))
        assertTrue(deferredRule.contains("SETTINGS_EXPORT_ENTRY_VISIBLE = false"))
        assertTrue(deferredRule.contains("SETTINGS_IMPORT_ENTRY_VISIBLE = false"))
        assertTrue(phasePlan.contains("Sprint 0.3 / 0.3.3 导出/导入实现"))
        assertTrue(phasePlan.contains("设置页入口暂时关闭"))
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
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            *parts
        )
        return String(Files.readAllBytes(rootFile(relativePath)))
    }

    private fun rootFile(vararg parts: String): Path =
        rootFile(parts.fold(Paths.get("")) { path, part -> path.resolve(part) })

    private fun readRootFile(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(relativePath: Path): Path =
        sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath),
            Paths.get("..").resolve(relativePath)
        ).first(Files::exists)
}
