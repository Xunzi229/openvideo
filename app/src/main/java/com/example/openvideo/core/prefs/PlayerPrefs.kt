package com.example.openvideo.core.prefs

import android.content.Context

class PlayerPrefs(context: Context) : PrefsManager(context, PREFS_NAME) {

    // ── 播放 ──

    var speed: Float
        get() = getFloat(KEY_SPEED, 1.0f)
        set(value) = putFloat(KEY_SPEED, value)

    var loopMode: LoopMode
        get() = LoopMode.fromKey(getString(KEY_LOOP_MODE, "list"))
        set(value) = putString(KEY_LOOP_MODE, value.key)

    var seekInterval: Int
        get() = getInt(KEY_SEEK_INTERVAL, 10)
        set(value) = putInt(KEY_SEEK_INTERVAL, value)

    var rememberProgress: Boolean
        get() = getBoolean(KEY_REMEMBER_PROGRESS, true)
        set(value) = putBoolean(KEY_REMEMBER_PROGRESS, value)

    var autoPlayNext: Boolean
        get() = getBoolean(KEY_AUTO_PLAY_NEXT, true)
        set(value) = putBoolean(KEY_AUTO_PLAY_NEXT, value)

    var hwAcceleration: Boolean
        get() = getBoolean(KEY_HW_ACCEL, true)
        set(value) = putBoolean(KEY_HW_ACCEL, value)

    var pauseOnExit: Boolean
        get() = getBoolean(KEY_PAUSE_ON_EXIT, false)
        set(value) = putBoolean(KEY_PAUSE_ON_EXIT, value)

    var bgAudio: Boolean
        get() = getBoolean(KEY_BG_AUDIO, false)
        set(value) = putBoolean(KEY_BG_AUDIO, value)

    var skipIntroOutro: Boolean
        get() = getBoolean(KEY_SKIP_INTRO_OUTRO, false)
        set(value) = putBoolean(KEY_SKIP_INTRO_OUTRO, value)

    var introSeconds: Int
        get() = getInt(KEY_INTRO_SECONDS, 0)
        set(value) = putInt(KEY_INTRO_SECONDS, value)

    var outroSeconds: Int
        get() = getInt(KEY_OUTRO_SECONDS, 0)
        set(value) = putInt(KEY_OUTRO_SECONDS, value)

    var keepScreenOn: Boolean
        get() = getBoolean(KEY_KEEP_SCREEN_ON, true)
        set(value) = putBoolean(KEY_KEEP_SCREEN_ON, value)

    var controlsAutoHide: Int
        get() = getInt(KEY_CONTROLS_AUTO_HIDE, 3)
        set(value) = putInt(KEY_CONTROLS_AUTO_HIDE, value)

    // ── 画面 ──

    var aspectRatio: AspectRatio
        get() = AspectRatio.fromKey(getString(KEY_ASPECT_RATIO, "fit"))
        set(value) = putString(KEY_ASPECT_RATIO, value.key)

    var rotation: Int
        get() = getInt(KEY_ROTATION, 0)
        set(value) = putInt(KEY_ROTATION, value)

    var mirror: Boolean
        get() = getBoolean(KEY_MIRROR, false)
        set(value) = putBoolean(KEY_MIRROR, value)

    var autoOrientationByVideo: Boolean
        get() = getBoolean(KEY_AUTO_ORIENTATION, true)
        set(value) = putBoolean(KEY_AUTO_ORIENTATION, value)

    // ── 声音 ──

    var speedPreservePitch: Boolean
        get() = getBoolean(KEY_SPEED_PRESERVE_PITCH, true)
        set(value) = putBoolean(KEY_SPEED_PRESERVE_PITCH, value)

    var volumeBoost: Boolean
        get() = getBoolean(KEY_VOLUME_BOOST, false)
        set(value) = putBoolean(KEY_VOLUME_BOOST, value)

    var audioChannel: AudioChannel
        get() = AudioChannel.fromKey(getString(KEY_AUDIO_CHANNEL, "stereo"))
        set(value) = putString(KEY_AUDIO_CHANNEL, value.key)

    var audioDelay: Int
        get() = getInt(KEY_AUDIO_DELAY, 0)
        set(value) = putInt(KEY_AUDIO_DELAY, value)

    // ── 字幕 ──

    var subtitleSize: Int
        get() = getInt(KEY_SUBTITLE_SIZE, 18)
        set(value) = putInt(KEY_SUBTITLE_SIZE, value)

    var subtitleColor: Int
        get() = getInt(KEY_SUBTITLE_COLOR, 0xFFFFFFFF.toInt())
        set(value) = putInt(KEY_SUBTITLE_COLOR, value)

    var subtitleBgStyle: SubtitleBgStyle
        get() = SubtitleBgStyle.fromKey(getString(KEY_SUBTITLE_BG, "semi"))
        set(value) = putString(KEY_SUBTITLE_BG, value.key)

    var subtitlePosition: Float
        get() = getFloat(KEY_SUBTITLE_POSITION, 1.0f)
        set(value) = putFloat(KEY_SUBTITLE_POSITION, value)

    var subtitleEncoding: String
        get() = getString(KEY_SUBTITLE_ENCODING, "auto")
        set(value) = putString(KEY_SUBTITLE_ENCODING, value)

    // 外挂字幕 URI（由设置覆盖层写入，播放器可读取并加载）
    var externalSubtitleUri: String
        get() = getString(KEY_EXTERNAL_SUBTITLE, "")
        set(value) = putString(KEY_EXTERNAL_SUBTITLE, value)

    // ── 手势 ──

    var leftVerticalGesture: GestureAction
        get() = GestureAction.fromKey(getString(KEY_LEFT_VERTICAL, "brightness"))
        set(value) = putString(KEY_LEFT_VERTICAL, value.key)

    var rightVerticalGesture: GestureAction
        get() = GestureAction.fromKey(getString(KEY_RIGHT_VERTICAL, "volume"))
        set(value) = putString(KEY_RIGHT_VERTICAL, value.key)

    var doubleTapAction: DoubleTapAction
        get() = DoubleTapAction.fromKey(getString(KEY_DOUBLE_TAP, "play_pause"))
        set(value) = putString(KEY_DOUBLE_TAP, value.key)

    var longPressAction: LongPressAction
        get() = LongPressAction.fromKey(getString(KEY_LONG_PRESS, "speed"))
        set(value) = putString(KEY_LONG_PRESS, value.key)

    var horizontalSwipeAction: GestureAction
        get() = GestureAction.fromKey(getString(KEY_HORIZONTAL_SWIPE, "seek"))
        set(value) = putString(KEY_HORIZONTAL_SWIPE, value.key)

    var gestureSensitivity: Int
        get() = getInt(KEY_GESTURE_SENSITIVITY, 2)
        set(value) = putInt(KEY_GESTURE_SENSITIVITY, value)

    // ── P1 手势增强 ──

    var doubleTapSeconds: Int
        get() = getInt(KEY_DOUBLE_TAP_SECONDS, 10)
        set(value) = putInt(KEY_DOUBLE_TAP_SECONDS, value)

    var longPressSpeed: Float
        get() = getFloat(KEY_LONG_PRESS_SPEED, 2.0f)
        set(value) = putFloat(KEY_LONG_PRESS_SPEED, value)

    var swipeRange: Int
        get() = getInt(KEY_SWIPE_RANGE, 60)
        set(value) = putInt(KEY_SWIPE_RANGE, value)

    var edgeSwipeBack: Boolean
        get() = getBoolean(KEY_EDGE_SWIPE_BACK, false)
        set(value) = putBoolean(KEY_EDGE_SWIPE_BACK, value)

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "player_settings"

        // 播放
        private const val KEY_SPEED = "speed"
        private const val KEY_LOOP_MODE = "loop_mode"
        private const val KEY_SEEK_INTERVAL = "seek_interval"
        private const val KEY_REMEMBER_PROGRESS = "remember_progress"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        private const val KEY_HW_ACCEL = "hw_acceleration"
        private const val KEY_PAUSE_ON_EXIT = "pause_on_exit"
        private const val KEY_BG_AUDIO = "bg_audio"
        private const val KEY_SKIP_INTRO_OUTRO = "skip_intro_outro"
        private const val KEY_INTRO_SECONDS = "intro_seconds"
        private const val KEY_OUTRO_SECONDS = "outro_seconds"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_CONTROLS_AUTO_HIDE = "controls_auto_hide"

        // 画面
        private const val KEY_ASPECT_RATIO = "aspect_ratio"
        private const val KEY_ROTATION = "rotation"
        private const val KEY_MIRROR = "mirror"
        private const val KEY_AUTO_ORIENTATION = "auto_orientation_by_video"

        // 声音
        private const val KEY_SPEED_PRESERVE_PITCH = "speed_preserve_pitch"
        private const val KEY_VOLUME_BOOST = "volume_boost"
        private const val KEY_AUDIO_CHANNEL = "audio_channel"
        private const val KEY_AUDIO_DELAY = "audio_delay"

        // 字幕
        private const val KEY_SUBTITLE_SIZE = "subtitle_size"
        private const val KEY_SUBTITLE_COLOR = "subtitle_color"
        private const val KEY_SUBTITLE_BG = "subtitle_bg"
        private const val KEY_SUBTITLE_ENCODING = "subtitle_encoding"
        // Exposed constant for external subtitle URI so other components can reference it
        const val KEY_EXTERNAL_SUBTITLE = "external_subtitle_uri"
        private const val KEY_SUBTITLE_POSITION = "subtitle_position"

        // 手势
        private const val KEY_LEFT_VERTICAL = "left_vertical_gesture"
        private const val KEY_RIGHT_VERTICAL = "right_vertical_gesture"
        private const val KEY_DOUBLE_TAP = "double_tap_action"
        private const val KEY_LONG_PRESS = "long_press_action"
        private const val KEY_HORIZONTAL_SWIPE = "horizontal_swipe_action"
        private const val KEY_GESTURE_SENSITIVITY = "gesture_sensitivity"

        // P1 手势增强
        private const val KEY_DOUBLE_TAP_SECONDS = "double_tap_seconds"
        private const val KEY_LONG_PRESS_SPEED = "long_press_speed"
        private const val KEY_SWIPE_RANGE = "swipe_range"
        private const val KEY_EDGE_SWIPE_BACK = "edge_swipe_back"
    }
}
