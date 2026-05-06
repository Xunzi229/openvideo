package com.example.openvideo.ui.settings

import com.example.openvideo.core.prefs.AspectRatio

object DefaultPlayerSettings {
    val supportedSpeeds: List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    fun supportedSpeedOrDefault(speed: Float): Float {
        return supportedSpeeds.firstOrNull { it == speed } ?: 1.0f
    }

    fun aspectRatioOrDefault(aspectRatio: AspectRatio): AspectRatio {
        return aspectRatio
    }
}
