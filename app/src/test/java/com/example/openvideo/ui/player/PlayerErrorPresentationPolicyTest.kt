package com.example.openvideo.ui.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerErrorPresentationPolicyTest {

    // --- isDecoderError ---

    @Test
    fun decoderInitFailedIsDecoderError() {
        assertTrue(
            PlayerErrorPresentationPolicy.isDecoderError(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
            )
        )
    }

    @Test
    fun decodingFailedIsDecoderError() {
        assertTrue(
            PlayerErrorPresentationPolicy.isDecoderError(
                PlaybackException.ERROR_CODE_DECODING_FAILED
            )
        )
    }

    @Test
    fun audioTrackInitFailedIsDecoderError() {
        assertTrue(
            PlayerErrorPresentationPolicy.isDecoderError(
                PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
            )
        )
    }

    @Test
    fun ioErrorIsNotDecoderError() {
        assertFalse(
            PlayerErrorPresentationPolicy.isDecoderError(
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
            )
        )
    }

    // --- isIoError ---

    @Test
    fun fileNotFoundIsIoError() {
        assertTrue(
            PlayerErrorPresentationPolicy.isIoError(
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
            )
        )
    }

    @Test
    fun networkConnectionFailedIsIoError() {
        assertTrue(
            PlayerErrorPresentationPolicy.isIoError(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            )
        )
    }

    @Test
    fun decoderErrorIsNotIoError() {
        assertFalse(
            PlayerErrorPresentationPolicy.isIoError(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
            )
        )
    }

    // --- present() action sets ---

    @Test
    fun decoderErrorIncludesSwitchSoftwareDecode() {
        val presentation = PlayerErrorPresentationPolicy.present(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)
        assertTrue(
            PlayerErrorPresentationPolicy.ErrorAction.SWITCH_SOFTWARE_DECODER in presentation.actions
        )
    }

    @Test
    fun decoderErrorIncludesRetry() {
        val presentation = PlayerErrorPresentationPolicy.present(PlaybackException.ERROR_CODE_DECODING_FAILED)
        assertTrue(
            PlayerErrorPresentationPolicy.ErrorAction.RETRY in presentation.actions
        )
    }

    @Test
    fun ioErrorDoesNotIncludeSwitchSoftwareDecode() {
        val presentation = PlayerErrorPresentationPolicy.present(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
        assertFalse(
            PlayerErrorPresentationPolicy.ErrorAction.SWITCH_SOFTWARE_DECODER in presentation.actions
        )
    }

    @Test
    fun ioErrorIncludesRetryAndGoBack() {
        val presentation = PlayerErrorPresentationPolicy.present(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
        assertTrue(PlayerErrorPresentationPolicy.ErrorAction.RETRY in presentation.actions)
        assertTrue(PlayerErrorPresentationPolicy.ErrorAction.GO_BACK in presentation.actions)
    }

    @Test
    fun generalErrorActionsDoNotIncludeSoftDecode() {
        // ERROR_CODE_UNSPECIFIED is a generic non-decoder, non-IO error
        val presentation = PlayerErrorPresentationPolicy.present(PlaybackException.ERROR_CODE_UNSPECIFIED)
        assertFalse(
            PlayerErrorPresentationPolicy.ErrorAction.SWITCH_SOFTWARE_DECODER in presentation.actions
        )
    }

    @Test
    fun allPresentationActionsIncludeCopyDiagnosticsAndGoBack() {
        val codes = listOf(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_UNSPECIFIED
        )
        for (code in codes) {
            val presentation = PlayerErrorPresentationPolicy.present(code)
            val actions = presentation.actions
            assertTrue(
                "COPY_DIAGNOSTICS missing for code $code",
                PlayerErrorPresentationPolicy.ErrorAction.COPY_DIAGNOSTICS in actions
            )
            assertTrue(
                "GO_BACK missing for code $code",
                PlayerErrorPresentationPolicy.ErrorAction.GO_BACK in actions
            )
        }
    }

    // --- title / desc StringRes sanity check ---

    @Test
    fun decoderAndIoAndGeneralHaveDifferentTitles() {
        val decoderTitle = PlayerErrorPresentationPolicy.present(
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
        ).titleRes
        val ioTitle = PlayerErrorPresentationPolicy.present(
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        ).titleRes
        val generalTitle = PlayerErrorPresentationPolicy.present(
            PlaybackException.ERROR_CODE_UNSPECIFIED
        ).titleRes

        assertEquals(3, setOf(decoderTitle, ioTitle, generalTitle).size)
    }

    @Test
    fun portraitAndLandscapePlayerLayoutsBothExposeErrorHudViews() {
        val portrait = readResource("layout", "activity_player.xml")
        val landscape = readResource("layout-land", "activity_player.xml")

        listOf(portrait, landscape).forEach { source ->
            assertTrue(source.contains("@+id/player_error_hud"))
            assertTrue(source.contains("@+id/tv_error_title"))
            assertTrue(source.contains("@+id/tv_error_desc"))
            assertTrue(source.contains("@+id/btn_error_soft_decode"))
            assertTrue(source.contains("@+id/btn_error_retry"))
            assertTrue(source.contains("@+id/btn_error_copy_diag"))
            assertTrue(source.contains("@+id/btn_error_back"))
        }
    }

    @Test
    fun softwareDecodeRetryDoesNotPersistGlobalDecodePreference() {
        val source = String(Files.readAllBytes(playerErrorHudControllerSource()))
        val block = source.substringAfter("softDecodeButtonProvider()?.setOnClickListener {")
            .substringBefore("\n        }")

        assertTrue(block.contains("viewModel.setDecodeMode(DecodeMode.SOFT)"))
        assertFalse(block.contains("playerPrefs.hwAcceleration"))
        assertFalse(block.contains("playerPrefs.softwareAudioDecoder"))
    }

    private fun readResource(dir: String, file: String): String =
        String(Files.readAllBytes(resource(dir, file)))

    private fun playerActivitySource(): Path {
        return kotlinSource("PlayerActivity.kt")
    }

    private fun playerErrorHudControllerSource(): Path {
        return kotlinSource("PlayerErrorHudController.kt")
    }

    private fun kotlinSource(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            name
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun resource(dir: String, file: String): Path {
        val relativePath = Paths.get("src", "main", "res", dir, file)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
