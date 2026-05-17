package com.example.openvideo.ui.player

object PlayerAbLoopButtonStylePolicy {
    fun shouldHighlight(event: PlayerAbLoopEvent): Boolean =
        event == PlayerAbLoopEvent.POINT_A_SET || event == PlayerAbLoopEvent.LOOP_STARTED

    fun shouldClearHighlight(event: PlayerAbLoopEvent): Boolean =
        event == PlayerAbLoopEvent.INVALID_POINT_B || event == PlayerAbLoopEvent.CANCELLED
}
