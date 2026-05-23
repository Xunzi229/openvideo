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

    @Test
    fun recordOnceIgnoresRepeatedNames() {
        var now = 10_000L
        val trace = PlayerStartupTrace(nowMs = { now })

        trace.recordOnce(PlayerStartupTrace.Events.PREPARE_READY)
        now = 10_500L
        trace.recordOnce(PlayerStartupTrace.Events.PREPARE_READY)
        now = 11_000L
        trace.recordOnce(PlayerStartupTrace.Events.FIRST_FRAME_RENDERED)

        val events = trace.snapshot()

        assertEquals(2, events.size)
        assertEquals(PlayerStartupTrace.Events.PREPARE_READY, events[0].name)
        assertEquals(0L, events[0].elapsedMs)
        assertEquals(PlayerStartupTrace.Events.FIRST_FRAME_RENDERED, events[1].name)
        assertEquals(1_000L, events[1].elapsedMs)
        assertTrue(trace.hasRecorded(PlayerStartupTrace.Events.PREPARE_READY))
    }

    @Test
    fun eventConstantsExposeCanonicalNames() {
        assertEquals("activity_created", PlayerStartupTrace.Events.ACTIVITY_CREATED)
        assertEquals("player_initialized", PlayerStartupTrace.Events.PLAYER_INITIALIZED)
        assertEquals("player_view_attached", PlayerStartupTrace.Events.PLAYER_VIEW_ATTACHED)
        assertEquals("subtitle_scan_finished", PlayerStartupTrace.Events.SUBTITLE_SCAN_FINISHED)
        assertEquals("prepare_ready", PlayerStartupTrace.Events.PREPARE_READY)
        assertEquals("ready_after_buffering", PlayerStartupTrace.Events.READY_AFTER_BUFFERING)
        assertEquals("first_frame_rendered", PlayerStartupTrace.Events.FIRST_FRAME_RENDERED)
        assertEquals("first_frame_timeout", PlayerStartupTrace.Events.FIRST_FRAME_TIMEOUT)
    }
}
