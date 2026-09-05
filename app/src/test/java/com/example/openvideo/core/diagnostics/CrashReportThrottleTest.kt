package com.example.openvideo.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportThrottleTest {
    private fun failure(method: String = "initialize", message: String = "failed") =
        IllegalStateException(message).apply {
            stackTrace = arrayOf(StackTraceElement("Codec", method, "Codec.java", 12))
        }

    @Test
    fun repeatedNonfatalFailuresAreLimitedWithoutDependingOnMediaNames() {
        val throttle = CrashReportThrottle(intervalMs = 100)
        val source = CrashCategoryPolicy.SOURCE_PLAYER
        assertTrue(throttle.shouldEnqueue(source, failure(message = "video A"), 0))
        assertFalse(throttle.shouldEnqueue(source, failure(message = "video B"), 99))
        assertTrue(throttle.shouldEnqueue(source, failure(message = "video C"), 100))
    }

    @Test
    fun distinctCausesCallSitesAndSourcesRemainObservable() {
        val throttle = CrashReportThrottle()
        assertTrue(throttle.shouldEnqueue(CrashCategoryPolicy.SOURCE_PLAYER, failure(), 0))
        assertTrue(throttle.shouldEnqueue(CrashCategoryPolicy.SOURCE_PLAYER, failure("release"), 1))
        assertTrue(throttle.shouldEnqueue(CrashCategoryPolicy.SOURCE_OEM_THREAD, failure(), 2))
        assertTrue(throttle.shouldEnqueue(CrashCategoryPolicy.SOURCE_PLAYER,
            failure().apply { initCause(NullPointerException("cause")) }, 3))
    }

    @Test
    fun uncaughtCrashesAreNeverThrottled() {
        val throttle = CrashReportThrottle()
        repeat(3) { assertTrue(throttle.shouldEnqueue(CrashCategoryPolicy.SOURCE_UNCAUGHT, failure(), 0)) }
    }

    @Test
    fun historyIsBoundedAndEvictsTheOldestAcceptedFailure() {
        val throttle = CrashReportThrottle(maxEntries = 2)
        val source = CrashCategoryPolicy.SOURCE_PLAYER
        assertTrue(throttle.shouldEnqueue(source, failure("first"), 0))
        assertTrue(throttle.shouldEnqueue(source, failure("second"), 1))
        assertTrue(throttle.shouldEnqueue(source, failure("third"), 2))
        assertFalse(throttle.shouldEnqueue(source, failure("second"), 3))
        assertTrue(throttle.shouldEnqueue(source, failure("first"), 4))
    }

    @Test
    fun clockRollbackDoesNotSuppressReportsIndefinitely() {
        val throttle = CrashReportThrottle()
        assertTrue(throttle.shouldEnqueue(CrashCategoryPolicy.SOURCE_PLAYER, failure(), 100))
        assertTrue(throttle.shouldEnqueue(CrashCategoryPolicy.SOURCE_PLAYER, failure(), 1))
    }
}
