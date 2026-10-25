package com.openvideo.app.core.prefs

enum class PlaybackOrientation(val key: String) {
    DEFAULT("default"),
    PORTRAIT("portrait"),
    LANDSCAPE("landscape");

    companion object {
        fun fromKey(key: String): PlaybackOrientation =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
