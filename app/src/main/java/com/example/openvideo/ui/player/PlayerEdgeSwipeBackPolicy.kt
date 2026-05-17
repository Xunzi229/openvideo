package com.example.openvideo.ui.player

object PlayerEdgeSwipeBackPolicy {
    /** Minimum horizontal drag (px) from the left edge to trigger back. */
    const val MIN_FINISH_DRAG_PX = 100f

    fun shouldFinish(
        edgeSwipeBackEnabled: Boolean,
        isEdgeSwipe: Boolean,
        isHorizontalSwipe: Boolean,
        dragDxPx: Float
    ): Boolean =
        edgeSwipeBackEnabled &&
            isEdgeSwipe &&
            isHorizontalSwipe &&
            dragDxPx > MIN_FINISH_DRAG_PX
}
