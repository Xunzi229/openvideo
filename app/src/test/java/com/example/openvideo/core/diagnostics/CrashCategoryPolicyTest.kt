package com.example.openvideo.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class CrashCategoryPolicyTest {

    @Test
    fun genericPlayerSourceFailureMapsToPlayback() {
        val category = CrashCategoryPolicy.categorize(
            throwable = RuntimeException("anything"),
            source = CrashCategoryPolicy.SOURCE_PLAYER
        )
        assertEquals(CrashCategory.PLAYBACK, category)
    }

    @Test
    fun detachedFragmentMapsToLifecycleBeforeUnknownFallback() {
        val category = CrashCategoryPolicy.categorize(
            IllegalStateException("Fragment SettingsFragment{abc} not attached to an activity.")
        )
        assertEquals(CrashCategory.LIFECYCLE, category)
    }

    @Test
    fun lifecycleClassificationRequiresTheSameExceptionToCarryTheDetachedFragmentMessage() {
        val throwable = IllegalStateException(
            "outer state failure",
            RuntimeException("Fragment SettingsFragment{abc} not attached to an activity.")
        )

        assertEquals(CrashCategory.UNKNOWN, CrashCategoryPolicy.categorize(throwable))
    }

    @Test
    fun unrecognizedMediaInputIsNotMisclassifiedAsPlayerCrash() {
        val category = CrashCategoryPolicy.categorize(
            RuntimeException("None of the available extractors could read the stream"),
            source = CrashCategoryPolicy.SOURCE_PLAYER
        )
        assertEquals(CrashCategory.MEDIA_INPUT, category)
    }

    @Test
    fun outOfMemoryErrorMapsToMemoryBeforeInspectingLargeStackCollections() {
        assertEquals(
            CrashCategory.MEMORY,
            CrashCategoryPolicy.categorize(OutOfMemoryError("allocation failed"))
        )
    }

    @Test
    fun zteGameAssistStackMapsToOemIntegration() {
        val throwable = NullPointerException("KeyMapObserver.stop failed").apply {
            stackTrace = arrayOf(
                StackTraceElement(
                    "com.zte.gameassist.app.GameActivityStub",
                    "unregisterObserver",
                    "GameActivityStub.java",
                    137
                )
            )
        }

        assertEquals(CrashCategory.OEM_INTEGRATION, CrashCategoryPolicy.categorize(throwable))
    }

    @Test
    fun securityExceptionMapsToPermission() {
        val category = CrashCategoryPolicy.categorize(SecurityException("Permission denied"))
        assertEquals(CrashCategory.PERMISSION, category)
    }

    @Test
    fun mediaStoreInStackMapsToMediaStore() {
        val throwable = RuntimeException("oops").apply {
            stackTrace = arrayOf(
                StackTraceElement("android.provider.MediaStore", "query", "MediaStore.java", 1)
            )
        }
        val category = CrashCategoryPolicy.categorize(throwable)
        assertEquals(CrashCategory.MEDIA_STORE, category)
    }

    @Test
    fun layoutInflateMapsToResourceInflate() {
        val throwable = RuntimeException("Binary XML inflate failed").apply {
            stackTrace = arrayOf(
                StackTraceElement("android.view.LayoutInflater", "inflate", "LayoutInflater.java", 1)
            )
        }
        val category = CrashCategoryPolicy.categorize(throwable)
        assertEquals(CrashCategory.RESOURCE_INFLATE, category)
    }

    @Test
    fun causeChainExposesPlaybackException() {
        val cause = RuntimeException("media3 exoplayer decoder failed").apply {
            stackTrace = arrayOf(
                StackTraceElement("androidx.media3.exoplayer.ExoPlayerImpl", "release", "ExoPlayerImpl.java", 1)
            )
        }
        val throwable = RuntimeException("wrapped", cause)
        val category = CrashCategoryPolicy.categorize(throwable)
        assertEquals(CrashCategory.PLAYBACK, category)
    }

    @Test
    fun unknownFallback() {
        val category = CrashCategoryPolicy.categorize(IllegalStateException("nothing matches here"))
        assertEquals(CrashCategory.UNKNOWN, category)
    }

    @Test
    fun releaseScriptSourceMapsToReleaseScript() {
        val category = CrashCategoryPolicy.categorize(
            throwable = IllegalStateException("missing keystore"),
            source = "release_script_main"
        )
        assertEquals(CrashCategory.RELEASE_SCRIPT, category)
    }
}
