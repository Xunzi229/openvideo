package com.openvideo.app.ui.player

import com.openvideo.app.core.prefs.GestureAction

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
