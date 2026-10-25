package com.openvideo.app.core.prefs

enum class LocalContinuePlaybackPosition(val key: String) {
    TOP("top"),
    BOTTOM("bottom");

    companion object {
        fun fromKey(key: String): LocalContinuePlaybackPosition =
            entries.firstOrNull { it.key == key } ?: TOP
    }
}
