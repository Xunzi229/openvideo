package com.example.openvideo.ui.player

import android.os.Build

object PlayerPipCompatPolicy {
    fun isInPictureInPictureMode(sdkInt: Int, isInPictureInPictureMode: Boolean): Boolean =
        sdkInt >= Build.VERSION_CODES.N && isInPictureInPictureMode
}
