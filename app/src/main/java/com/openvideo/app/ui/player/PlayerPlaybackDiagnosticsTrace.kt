package com.openvideo.app.ui.player

/** Application-thread-only, bounded history; never stores media titles, URIs or headers. */
class PlayerPlaybackDiagnosticsTrace(
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private val events = ArrayDeque<String>()
    private var startMs: Long? = null

    fun record(event: String) {
        val now = nowMs()
        val start = startMs ?: now.also { startMs = it }
        if (events.size == MAX_EVENTS) events.removeFirst()
        events.addLast("${now - start}ms $event")
    }

    fun snapshot(): List<String> = events.toList()

    fun clear() {
        events.clear()
        startMs = null
    }

    private companion object {
        const val MAX_EVENTS = 24
    }
}
