package com.openvideo.app.ui.player

/** Main-thread request generation; cancellation alone cannot stop blocking platform IO. */
internal class LatestPlayerRequest {
    private var generation = 0L
    fun next(): Long = ++generation
    fun accepts(token: Long): Boolean = token == generation
}
