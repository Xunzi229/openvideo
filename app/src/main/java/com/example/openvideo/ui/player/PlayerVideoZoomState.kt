package com.example.openvideo.ui.player

/** Session-only manual pinch zoom / pan layered on top of P9-1b content-frame transform. */
data class PlayerVideoZoomState(
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
) {
    companion object {
        val IDENTITY = PlayerVideoZoomState()
    }
}
