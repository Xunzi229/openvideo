package com.example.openvideo.core.prefs

enum class LoopMode(val key: String) {
    OFF("off"),
    SINGLE("single"),
    LIST("list");

    companion object {
        fun fromKey(key: String): LoopMode =
            entries.find { it.key == key } ?: LIST
    }
}

enum class AspectRatio(val key: String) {
    FIT("fit"),
    FILL("fill"),
    CROP("crop"),
    STRETCH("stretch"),
    RATIO_4_3("4_3"),
    RATIO_16_9("16_9");

    companion object {
        fun fromKey(key: String): AspectRatio =
            entries.find { it.key == key } ?: FIT
    }
}

enum class AudioChannel(val key: String) {
    STEREO("stereo"),
    LEFT("left"),
    RIGHT("right");

    companion object {
        fun fromKey(key: String): AudioChannel =
            entries.find { it.key == key } ?: STEREO
    }
}

enum class SubtitleBgStyle(val key: String) {
    NONE("none"),
    SEMI_TRANSPARENT("semi"),
    OPAQUE("opaque");

    companion object {
        fun fromKey(key: String): SubtitleBgStyle =
            entries.find { it.key == key } ?: SEMI_TRANSPARENT
    }
}

enum class GestureAction(val key: String) {
    BRIGHTNESS("brightness"),
    VOLUME("volume"),
    SEEK("seek"),
    NONE("none");

    companion object {
        fun fromKey(key: String): GestureAction =
            entries.find { it.key == key } ?: NONE
    }
}

enum class DoubleTapAction(val key: String) {
    PLAY_PAUSE("play_pause"),
    FORWARD("forward"),
    BACKWARD("backward"),
    NONE("none");

    companion object {
        fun fromKey(key: String): DoubleTapAction =
            entries.find { it.key == key } ?: PLAY_PAUSE
    }
}

enum class LongPressAction(val key: String) {
    SPEED("speed"),
    NONE("none");

    companion object {
        fun fromKey(key: String): LongPressAction =
            entries.find { it.key == key } ?: SPEED
    }
}

enum class ThemeMode(val key: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system");

    companion object {
        fun fromKey(key: String): ThemeMode =
            entries.find { it.key == key } ?: DARK
    }
}
