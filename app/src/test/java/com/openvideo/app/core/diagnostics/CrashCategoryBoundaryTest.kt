package com.openvideo.app.core.diagnostics

import org.junit.Assert.*
import org.junit.Test

class CrashCategoryBoundaryTest {
    private class UnrecognizedInputFormatException : Exception()
    private class PlaybackException : Exception()
    private class DecoderException : Exception()
    private class MediaCodecError : Exception()
    private class InflateException : Exception()
    private class NotFoundException : Exception()

    @Test fun exceptionTypesAndStackFramesClassifyIndependently() {
        val types = listOf(UnrecognizedInputFormatException() to CrashCategory.MEDIA_INPUT,
            PlaybackException() to CrashCategory.PLAYBACK, DecoderException() to CrashCategory.PLAYBACK,
            MediaCodecError() to CrashCategory.PLAYBACK, InflateException() to CrashCategory.RESOURCE_INFLATE,
            NotFoundException() to CrashCategory.RESOURCE_INFLATE)
        types.forEach { (error, expected) -> assertEquals(expected, CrashCategoryPolicy.categorize(error)) }
        val frames = mapOf("android.media3.Player" to CrashCategory.PLAYBACK, "exoplayer.Player" to CrashCategory.PLAYBACK,
            "android.media.MediaCodec" to CrashCategory.PLAYBACK, "android.provider.MediaStore" to CrashCategory.MEDIA_STORE,
            "android.content.ContentResolver" to CrashCategory.MEDIA_STORE, "android.view.LayoutInflater" to CrashCategory.RESOURCE_INFLATE,
            "android.content.res.Resources" to CrashCategory.RESOURCE_INFLATE, "com.zte.gameassist.app.GameActivityStub" to CrashCategory.OEM_INTEGRATION)
        frames.forEach { (frame, expected) ->
            val error = Exception().apply { stackTrace = arrayOf(StackTraceElement(frame, "getValue", "Test.kt", 1)) }
            assertEquals(frame, expected, CrashCategoryPolicy.categorize(error))
        }
    }

    @Test fun nullMessagesPartialLifecycleMessagesAndCauseCyclesAreSafe() {
        for (message in listOf(null, "fragment other", "not attached other")) {
            assertEquals(CrashCategory.UNKNOWN, CrashCategoryPolicy.categorize(IllegalStateException(message)))
        }
        assertEquals(CrashCategory.MEDIA_STORE, CrashCategoryPolicy.categorize(Exception("MEDIA STORE failed")))
        assertEquals(CrashCategory.PERMISSION, CrashCategoryPolicy.categorize(Exception("denied")))
        val first = Exception("first")
        val second = Exception("second", first)
        first.initCause(second)
        assertEquals(listOf(first, second), CrashCategoryPolicy.throwableChain(first))
        assertEquals(CrashCategory.UNKNOWN, CrashCategoryPolicy.categorize(first, "unrelated"))
        assertEquals(CrashCategory.MEMORY, CrashCategoryPolicy.categorize(Exception(OutOfMemoryError()), "player"))
    }
}
