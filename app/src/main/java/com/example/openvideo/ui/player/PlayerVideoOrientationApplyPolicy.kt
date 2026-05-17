package com.example.openvideo.ui.player

object PlayerVideoOrientationApplyPolicy {
    fun shouldApply(
        autoOrientationByVideo: Boolean,
        userOverrodeOrientation: Boolean,
        width: Int,
        height: Int
    ): Boolean =
        autoOrientationByVideo &&
            !userOverrodeOrientation &&
            width > 0 &&
            height > 0
}
