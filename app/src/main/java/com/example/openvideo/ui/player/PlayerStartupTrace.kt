package com.example.openvideo.ui.player

import android.os.SystemClock

data class StartupEvent(
    val name: String,
    val elapsedMs: Long
)

class PlayerStartupTrace(
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val events = mutableListOf<StartupEvent>()
    private val recordedNames = HashSet<String>()
    private var startMs: Long? = null

    fun record(name: String) {
        val now = nowMs()
        val start = startMs ?: now.also { startMs = it }
        events += StartupEvent(name, now - start)
        recordedNames += name
    }

    /** Record at most once; useful for state callbacks that may fire repeatedly. */
    fun recordOnce(name: String) {
        if (name in recordedNames) return
        record(name)
    }

    fun hasRecorded(name: String): Boolean = name in recordedNames

    fun elapsedSinceStart(): Long? = startMs?.let { nowMs() - it }

    fun snapshot(): List<StartupEvent> = events.toList()

    fun format(): String = snapshot().joinToString(separator = "\n") { event ->
        "${event.name}=${event.elapsedMs}ms"
    }

    object Events {
        const val ACTIVITY_CREATED = "activity_created"
        const val PLAYER_INITIALIZED = "player_initialized"
        const val PLAYER_VIEW_ATTACHED = "player_view_attached"
        const val SUBTITLE_SCAN_FINISHED = "subtitle_scan_finished"
        const val PREPARE_READY = "prepare_ready"
        const val FIRST_FRAME_RENDERED = "first_frame_rendered"
        const val FIRST_FRAME_TIMEOUT = "first_frame_timeout"
    }
}
