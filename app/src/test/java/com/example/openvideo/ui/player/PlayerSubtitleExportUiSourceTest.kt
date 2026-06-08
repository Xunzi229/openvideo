package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSubtitleExportUiSourceTest {

    @Test
    fun subtitleSettingsSheetWiresUtf8ExportThroughSafCreateDocument() {
        val sheet = sourceText("PlayerSubtitleSettingsSheet.kt")
        val layout = layoutText("activity_player_subtitle_settings.xml")

        assertTrue(sheet.contains("private val viewModel: PlayerViewModel by activityViewModels()"))
        assertTrue(sheet.contains("ActivityResultContracts.CreateDocument(\"application/x-subrip\")"))
        assertTrue(sheet.contains("exportSubtitleLauncher.launch(viewModel.suggestedSubtitleExportFileName())"))
        assertTrue(sheet.contains("viewModel.writeCurrentSubtitleUtf8ExportTo(requireContext(), uri)"))
        assertTrue(sheet.contains("PlayerViewModel.SubtitleExportResult.NoSubtitles"))
        assertTrue(layout.contains("@+id/btn_export_subtitle_utf8"))
        assertTrue(layout.contains("@string/player_settings_subtitle_export_utf8"))
    }

    @Test
    fun subtitleSettingsSheetWiresDelayCorrectionExportThroughSafCreateDocument() {
        val sheet = sourceText("PlayerSubtitleSettingsSheet.kt")
        val layout = layoutText("activity_player_subtitle_settings.xml")

        assertTrue(sheet.contains("delayCorrectionExportLauncher.launch(viewModel.suggestedSubtitleDelayCorrectionExportFileName())"))
        assertTrue(sheet.contains("viewModel.writeCurrentSubtitleDelayCorrectionExportTo(requireContext(), uri)"))
        assertTrue(sheet.contains("PlayerViewModel.SubtitleExportResult.NoDelay"))
        assertTrue(layout.contains("@+id/btn_export_subtitle_delay_corrected"))
        assertTrue(layout.contains("@string/player_settings_subtitle_export_delay_corrected"))
    }

    @Test
    fun playerViewModelExportsCurrentSubtitlesViaCoreUtf8PlanAndWriter() {
        val source = sourceText("PlayerViewModel.kt")
        val block = source.substringAfter("fun writeCurrentSubtitleUtf8ExportTo(")
            .substringBefore("\n    fun suggestedSubtitleExportFileName()")

        assertTrue(source.contains("sealed class SubtitleExportResult"))
        assertTrue(block.contains("_uiState.value.subtitles"))
        assertTrue(block.contains("SubtitleUtf8ExportPolicy.planSrtCopy("))
        assertTrue(block.contains("sourceName = currentVideoSource()"))
        assertTrue(block.contains("context.contentResolver.openOutputStream(uri)"))
        assertTrue(block.contains("SubtitleExportWriter.writePlanToOutputStream(out, plan)"))
        assertTrue(block.contains("SubtitleExportResult.NoSubtitles"))
        assertTrue(block.contains("SubtitleExportResult.OpenStreamFailed"))
        assertTrue(block.contains("SubtitleExportResult.WriteFailed"))
    }

    @Test
    fun playerViewModelExportsDelayCorrectedSubtitlesViaCorePoliciesAndWriter() {
        val source = sourceText("PlayerViewModel.kt")
        val block = source.substringAfter("fun writeCurrentSubtitleDelayCorrectionExportTo(")
            .substringBefore("\n    fun suggestedSubtitleDelayCorrectionExportFileName()")

        assertTrue(block.contains("_uiState.value.subtitles"))
        assertTrue(block.contains("playerPrefs.subtitleDelayMs"))
        assertTrue(block.contains("SubtitleExportResult.NoDelay"))
        assertTrue(block.contains("SubtitleDelayCorrectionPolicy.planShiftedCopy("))
        assertTrue(block.contains("deltaMs = playerPrefs.subtitleDelayMs"))
        assertTrue(block.contains("SubtitleUtf8ExportPolicy.planSrtCopy("))
        assertTrue(block.contains("items = delayPlan.items"))
        assertTrue(block.contains("context.contentResolver.openOutputStream(uri)"))
        assertTrue(block.contains("SubtitleExportWriter.writePlanToOutputStream(out, exportPlan)"))
    }

    @Test
    fun subtitleExportsBlockUnconfirmedOriginalOverwriteBeforeOpeningStream() {
        val viewModel = sourceText("PlayerViewModel.kt")
        val sheet = sourceText("PlayerSubtitleSettingsSheet.kt")
        val strings = valuesText("strings.xml")
        val utf8Block = viewModel.substringAfter("fun writeCurrentSubtitleUtf8ExportTo(")
            .substringBefore("\n    fun suggestedSubtitleExportFileName()")
        val delayBlock = viewModel.substringAfter("fun writeCurrentSubtitleDelayCorrectionExportTo(")
            .substringBefore("\n    fun suggestedSubtitleDelayCorrectionExportFileName()")

        assertTrue(viewModel.contains("data object OriginalOverwriteBlocked : SubtitleExportResult()"))
        assertTrue(
            utf8Block.substringBefore("context.contentResolver.openOutputStream(uri)")
                .contains("SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(")
        )
        assertTrue(
            delayBlock.substringBefore("context.contentResolver.openOutputStream(uri)")
                .contains("SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(")
        )
        assertTrue(sheet.contains("PlayerViewModel.SubtitleExportResult.OriginalOverwriteBlocked"))
        assertTrue(strings.contains("player_settings_subtitle_export_original_blocked"))
    }

    private fun sourceText(name: String): String =
        String(Files.readAllBytes(rootFile(Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", name))))

    private fun layoutText(name: String): String =
        String(Files.readAllBytes(rootFile(Paths.get("src", "main", "res", "layout", name))))

    private fun valuesText(name: String): String =
        String(Files.readAllBytes(rootFile(Paths.get("src", "main", "res", "values", name))))

    private fun rootFile(relativePath: Path): Path =
        sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
}
