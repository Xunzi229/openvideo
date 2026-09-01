package com.example.openvideo.ui.player

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import com.example.openvideo.core.diagnostics.CrashCategory

/** Separates bad media input from player/runtime failures without dropping either report. */
@OptIn(UnstableApi::class)
object PlayerFailureClassificationPolicy {

    fun isMediaInputFailure(errorCode: Int, cause: Throwable?): Boolean =
        errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
            errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
            generateSequence(cause) { it.cause }.any { it is UnrecognizedInputFormatException }

    fun category(errorCode: Int, cause: Throwable?): CrashCategory =
        if (isMediaInputFailure(errorCode, cause)) CrashCategory.MEDIA_INPUT else CrashCategory.PLAYBACK
}
