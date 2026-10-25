package com.openvideo.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlaybackDiagnosticsTraceTest {
    @Test
    fun keepsOnlyTheLatestEventsWithMonotonicRelativeTimes() {
        var nowMs = 100L
        val trace = PlayerPlaybackDiagnosticsTrace { nowMs }
        repeat(30) { index ->
            trace.record("event=$index")
            nowMs += 10L
        }

        val snapshot = trace.snapshot()
        assertEquals(24, snapshot.size)
        assertEquals("60ms event=6", snapshot.first())
        assertEquals("290ms event=29", snapshot.last())
    }

    @Test
    fun snapshotSurvivesClearingAndRecordingTheNextSession() {
        var nowMs = 100L
        val trace = PlayerPlaybackDiagnosticsTrace { nowMs }
        trace.record("buffering")
        val captured = trace.snapshot()
        trace.clear()

        assertTrue(trace.snapshot().isEmpty())
        nowMs = 1_000L
        trace.record("attached")
        assertEquals(listOf("0ms buffering"), captured)
        assertEquals(listOf("0ms attached"), trace.snapshot())
    }
}
