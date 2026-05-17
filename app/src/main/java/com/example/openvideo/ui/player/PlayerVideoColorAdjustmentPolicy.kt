package com.example.openvideo.ui.player

data class PlayerVideoColorAdjustments(
    val contrast: Float,
    val saturation: Float
)

object PlayerVideoColorAdjustmentPolicy {
    fun fromPercent(contrastPercent: Int, saturationPercent: Int): PlayerVideoColorAdjustments =
        PlayerVideoColorAdjustments(
            contrast = contrastPercent / 100f,
            saturation = saturationPercent / 100f
        )
}
