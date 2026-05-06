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
    private var startMs: Long? = null

    fun record(name: String) {
        val now = nowMs()
        val start = startMs ?: now.also { startMs = it }
        events += StartupEvent(name, now - start)
    }

    fun snapshot(): List<StartupEvent> = events.toList()

    fun format(): String = snapshot().joinToString(separator = "\n") { event ->
        "${event.name}=${event.elapsedMs}ms"
    }
}
