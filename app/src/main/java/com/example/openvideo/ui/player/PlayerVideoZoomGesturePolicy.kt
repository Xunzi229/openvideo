package com.example.openvideo.ui.player

object PlayerVideoZoomGesturePolicy {

    fun interceptsSingleFingerGestures(
        zoomAllowed: Boolean,
        manual: PlayerVideoZoomState
    ): Boolean = zoomAllowed && PlayerVideoZoomPolicy.isActive(manual)

    fun handlesMultiTouch(zoomAllowed: Boolean, pointerCount: Int): Boolean =
        zoomAllowed && pointerCount >= 2

    fun doubleTapResetsZoom(
        zoomAllowed: Boolean,
        manual: PlayerVideoZoomState
    ): Boolean = zoomAllowed && PlayerVideoZoomPolicy.isActive(manual)
}
