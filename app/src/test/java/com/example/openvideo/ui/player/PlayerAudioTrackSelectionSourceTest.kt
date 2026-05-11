package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerAudioTrackSelectionSourceTest {

    @Test
    fun playerManagerExposesAndSelectsMedia3AudioTracks() {
        val source = String(Files.readAllBytes(sourceFile("core", "player", "PlayerManager.kt")))

        assertTrue(source.contains("fun currentAudioTracks()"))
        assertTrue(source.contains("currentTracks.groups"))
        assertTrue(source.contains("C.TRACK_TYPE_AUDIO"))
        assertTrue(source.contains("fun selectAudioTrack("))
        assertTrue(source.contains("TrackSelectionOverride"))
        assertTrue(source.contains("clearOverridesOfType(C.TRACK_TYPE_AUDIO)"))
        assertTrue(source.contains("setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)"))
        assertTrue(source.contains("fun disableAudioTrack()"))
        assertTrue(source.contains("setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)"))
    }

    @Test
    fun playerSettingsAudioPageUsesRealTracksInsteadOfHardcodedEnglishTrack() {
        val source = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerSettingsDialog.kt")))
        val audioPage = source
            .substringAfter("private fun buildAudioPage()")
            .substringBefore("\n    private fun buildSubtitlePage()")

        assertTrue(audioPage.contains("viewModel.audioTracks()"))
        assertTrue(audioPage.contains("audioTrackLabel("))
        assertTrue(audioPage.contains("viewModel.selectAudioTrack("))
        assertTrue(audioPage.contains("viewModel.disableAudioTrack()"))
        assertFalse(audioPage.contains("player_sheet_audio_track_english"))
    }

    @Test
    fun infoPageIncludesCurrentAudioTrackDiagnostics() {
        val source = String(Files.readAllBytes(sourceFile("ui", "player", "PlayerSettingsDialog.kt")))
        val infoRows = source
            .substringAfter("private fun videoInfoRows()")
            .substringBefore("\n    @OptIn")

        assertTrue(infoRows.contains("viewModel.selectedAudioTrack()"))
        assertTrue(infoRows.contains("player_settings_info_current_audio_track"))
        assertTrue(infoRows.contains("player_settings_info_audio_decoder"))
    }

    private fun sourceFile(vararg parts: String): Path {
        val relativePath = parts.fold(Paths.get("src", "main", "java", "com", "example", "openvideo")) { path, part ->
            path.resolve(part)
        }
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
