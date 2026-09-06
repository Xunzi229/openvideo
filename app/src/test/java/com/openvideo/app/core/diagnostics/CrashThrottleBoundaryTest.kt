package com.openvideo.app.core.diagnostics

import org.junit.Assert.*
import org.junit.Test

class CrashThrottleBoundaryTest {
    @Test fun invalidLimitsFailAndClockRollbackDoesNotSuppressReports() {
        assertThrows(IllegalArgumentException::class.java) { CrashReportThrottle(intervalMs = 0) }
        assertThrows(IllegalArgumentException::class.java) { CrashReportThrottle(maxEntries = 0) }
        val error = Exception().apply { stackTrace = emptyArray() }
        val throttle = CrashReportThrottle(intervalMs = 10, maxEntries = 1)
        assertTrue(throttle.shouldEnqueue("player", error, 100))
        assertFalse(throttle.shouldEnqueue("player", error, 109))
        assertTrue(throttle.shouldEnqueue("player", error, 99))
        assertTrue(throttle.shouldEnqueue("player", error, 109))
        assertTrue(throttle.shouldEnqueue("other", error, 110))
        assertTrue(throttle.shouldEnqueue("player", error, 111))
    }
}
