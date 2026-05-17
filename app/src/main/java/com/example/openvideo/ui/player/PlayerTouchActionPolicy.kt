package com.example.openvideo.ui.player

import android.view.MotionEvent

/**
 * Maps [MotionEvent.getActionMasked] values to [PlayerTouchAction] for lock-mode and gesture routing.
 */
object PlayerTouchActionPolicy {

    fun fromMotionActionMasked(actionMasked: Int): PlayerTouchAction = when (actionMasked) {
        MotionEvent.ACTION_DOWN -> PlayerTouchAction.DOWN
        MotionEvent.ACTION_MOVE -> PlayerTouchAction.MOVE
        MotionEvent.ACTION_UP -> PlayerTouchAction.UP
        MotionEvent.ACTION_CANCEL -> PlayerTouchAction.CANCEL
        else -> PlayerTouchAction.OTHER
    }
}
