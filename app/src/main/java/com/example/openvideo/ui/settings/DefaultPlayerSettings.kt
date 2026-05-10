package com.example.openvideo.ui.settings

import com.example.openvideo.core.prefs.AspectRatio

object DefaultPlayerSettings {
    val supportedSpeeds: List<Float> = List(19) { index -> 0.5f + index * 0.25f }

    fun supportedSpeedOrDefault(speed: Float): Float {
        return supportedSpeeds.firstOrNull { it == speed } ?: 1.0f
    }

    fun aspectRatioOrDefault(aspectRatio: AspectRatio): AspectRatio {
        return aspectRatio
    }
}
