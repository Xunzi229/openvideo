package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.GestureAction

object PlayerGestureDispatchPolicy {
    fun onHorizontalSwipe(
        action: GestureAction,
        onSeek: () -> Unit
    ) {
        if (action == GestureAction.SEEK) {
            onSeek()
        }
    }
}
