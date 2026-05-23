package com.example.openvideo.ui.player

object PlayerSmartCropPixelPolicy {
    private const val BLACK_CHANNEL_MAX = 32

    fun isBlackArgb(pixel: Int): Boolean {
        val alpha = pixel ushr 24
        val red = pixel ushr 16 and 0xff
        val green = pixel ushr 8 and 0xff
        val blue = pixel and 0xff
        return alpha == 0 ||
            red <= BLACK_CHANNEL_MAX &&
            green <= BLACK_CHANNEL_MAX &&
            blue <= BLACK_CHANNEL_MAX
    }
}
