package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStartupTraceTest {

    @Test
    fun recordsEventsRelativeToFirstEvent() {
        var now = 1_000L
        val trace = PlayerStartupTrace(nowMs = { now })

        trace.record("activity_created")
        now = 1_120L
        trace.record("player_initialized")
        now = 1_450L
        trace.record("first_frame")

        val events = trace.snapshot()

        assertEquals(
            listOf(
                StartupEvent("activity_created", 0),
                StartupEvent("player_initialized", 120),
                StartupEvent("first_frame", 450)
            ),
            events
        )
    }

    @Test
    fun formatsTraceForDiagnosticLogs() {
        var now = 5_000L
        val trace = PlayerStartupTrace(nowMs = { now })

        trace.record("prepare_requested")
        now = 5_250L
        trace.record("subtitle_loaded")

        val log = trace.format()

        assertTrue(log.contains("prepare_requested=0ms"))
        assertTrue(log.contains("subtitle_loaded=250ms"))
    }
}
