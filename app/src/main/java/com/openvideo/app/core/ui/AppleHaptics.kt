package com.openvideo.app.core.ui

import android.view.HapticFeedbackConstants
import android.view.View

object AppleHaptics {
    fun light(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
}
