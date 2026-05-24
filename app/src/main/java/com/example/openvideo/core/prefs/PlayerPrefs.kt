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

    var playbackEndBehavior: PlaybackEndBehavior
        get() = PlaybackEndBehavior.fromKey(getString(KEY_PLAYBACK_END_BEHAVIOR, PlaybackEndBehavior.FOLLOW_SETTINGS.key))
        set(value) = putString(KEY_PLAYBACK_END_BEHAVIOR, value.key)

    var hwAcceleration: Boolean
        get() = getBoolean(KEY_HW_ACCEL, true)
        set(value) = putBoolean(KEY_HW_ACCEL, value)

    var pauseOnExit: Boolean
        get() = getBoolean(KEY_PAUSE_ON_EXIT, false)
        set(value) = putBoolean(KEY_PAUSE_ON_EXIT, value)

    var bgAudio: Boolean
        get() = getBoolean(KEY_BG_AUDIO, false)
        set(value) = putBoolean(KEY_BG_AUDIO, value)

    /** 后台播放时是否在通知栏显示控制条；默认 true。 */
    var bgPlaybackNotificationEnabled: Boolean
        get() = getBoolean(KEY_BG_NOTIFICATION_ENABLED, true)
        set(value) = putBoolean(KEY_BG_NOTIFICATION_ENABLED, value)

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

    var seekThumbnailEnabled: Boolean
        get() = getBoolean(KEY_SEEK_THUMBNAIL_ENABLED, true)
        set(value) = putBoolean(KEY_SEEK_THUMBNAIL_ENABLED, value)

    // ── 画面 ──

    var aspectRatio: AspectRatio
        get() = AspectRatio.fromKey(getString(KEY_ASPECT_RATIO, "fit"))
        set(value) = putString(KEY_ASPECT_RATIO, value.key)

    /** Center-window zoom for nested landscape/portrait content; requires FIT-style aspect ratio. */
    var contentFrameMode: ContentFrameMode
        get() = ContentFrameMode.fromKey(getString(KEY_CONTENT_FRAME_MODE, ContentFrameMode.OFF.key))
        set(value) = putString(KEY_CONTENT_FRAME_MODE, value.key)

    var rotation: Int
        get() = getInt(KEY_ROTATION, 0)
        set(value) = putInt(KEY_ROTATION, value)

    var mirror: Boolean
        get() = getBoolean(KEY_MIRROR, false)
        set(value) = putBoolean(KEY_MIRROR, value)

    var autoOrientationByVideo: Boolean
        get() = getBoolean(KEY_AUTO_ORIENTATION, true)
        set(value) = putBoolean(KEY_AUTO_ORIENTATION, value)

    var videoDisplayEnabled: Boolean
        get() = getBoolean(KEY_VIDEO_DISPLAY_ENABLED, true)
        set(value) = putBoolean(KEY_VIDEO_DISPLAY_ENABLED, value)

    var brightnessAdjustment: Int
        get() = getInt(KEY_BRIGHTNESS_ADJUSTMENT, 0)
        set(value) = putInt(KEY_BRIGHTNESS_ADJUSTMENT, value)

    var contrastAdjustment: Int
        get() = getInt(KEY_CONTRAST_ADJUSTMENT, 0)
        set(value) = putInt(KEY_CONTRAST_ADJUSTMENT, value)

    var saturationAdjustment: Int
        get() = getInt(KEY_SATURATION_ADJUSTMENT, 0)
        set(value) = putInt(KEY_SATURATION_ADJUSTMENT, value)

    var progressStyle: String
        get() = getString(KEY_PROGRESS_STYLE, "default")
        set(value) = putString(KEY_PROGRESS_STYLE, value)

    var controlsOpacity: Int
        get() = getInt(KEY_CONTROLS_OPACITY, 85)
        set(value) = putInt(KEY_CONTROLS_OPACITY, value)

    /**
     * 播放器设置面板（含二级、三级页）滑块：**数值表示不透明度（实心程度）**。
     * - **100**：完全不透明（透明度最低；不能再更不透明）
     * - **0**：最透明（可视作原先语境下的「100% 透明度」）
     * 默认 100。视图 `alpha = settingsPanelOpacity / 100f`。
     */
    var settingsPanelOpacity: Int
        get() = when {
            prefs.contains(KEY_SETTINGS_PANEL_OPACITY) ->
                prefs.getInt(KEY_SETTINGS_PANEL_OPACITY, 100).coerceIn(0, 100)
            prefs.contains(KEY_SETTINGS_SHEET_TRANSPARENCY_LEGACY) -> {
                val t = prefs.getInt(KEY_SETTINGS_SHEET_TRANSPARENCY_LEGACY, 100).coerceIn(0, 100)
                (100 - t).coerceIn(0, 100)
            }
            prefs.contains(KEY_SETTINGS_SHEET_OPACITY_LEGACY) ->
                prefs.getInt(KEY_SETTINGS_SHEET_OPACITY_LEGACY, 100).coerceIn(0, 100)
            else -> 60
        }
        set(value) {
            val v = value.coerceIn(0, 100)
            prefs.edit()
                .putInt(KEY_SETTINGS_PANEL_OPACITY, v)
                .remove(KEY_SETTINGS_SHEET_TRANSPARENCY_LEGACY)
                .remove(KEY_SETTINGS_SHEET_OPACITY_LEGACY)
                .apply()
        }

    /** 设置面板窗口背后压暗：0–100，对应 `Window.setDimAmount(value/100f)`，默认 40 */
    var settingsSheetBackdropDimPercent: Int
        get() = getInt(KEY_SETTINGS_SHEET_BACKDROP_DIM, 40)
        set(value) = putInt(KEY_SETTINGS_SHEET_BACKDROP_DIM, value.coerceIn(0, 100))

    /** 设置面板窗口背景模糊半径（dp），0=关闭；默认 18；仅 Android 12+ `setBackgroundBlurRadius` 生效 */
    var settingsSheetBackdropBlurDp: Int
        get() = getInt(KEY_SETTINGS_SHEET_BACKDROP_BLUR_DP, 18)
        set(value) = putInt(KEY_SETTINGS_SHEET_BACKDROP_BLUR_DP, value.coerceIn(0, 64))

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

    var audioMuted: Boolean
        get() = getBoolean(KEY_AUDIO_MUTED, false)
        set(value) = putBoolean(KEY_AUDIO_MUTED, value)

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

    var subtitleDelayMs: Int
        get() = getInt(KEY_SUBTITLE_DELAY_MS, 0)
        set(value) = putInt(KEY_SUBTITLE_DELAY_MS, value)

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

    var keyboardShortcuts: Boolean
        get() = getBoolean(KEY_KEYBOARD_SHORTCUTS, true)
        set(value) = putBoolean(KEY_KEYBOARD_SHORTCUTS, value)

    var subtitlesEnabled: Boolean
        get() = getBoolean(KEY_SUBTITLES_ENABLED, true)
        set(value) = putBoolean(KEY_SUBTITLES_ENABLED, value)

    var softwareAudioDecoder: Boolean
        get() = getBoolean(KEY_SOFTWARE_AUDIO_DECODER, false)
        set(value) = putBoolean(KEY_SOFTWARE_AUDIO_DECODER, value)

    var audioSyncEnabled: Boolean
        get() = getBoolean(KEY_AUDIO_SYNC_ENABLED, true)
        set(value) = putBoolean(KEY_AUDIO_SYNC_ENABLED, value)

    var lastStreamUrl: String
        get() = getString(KEY_LAST_STREAM_URL, "")
        set(value) = putString(KEY_LAST_STREAM_URL, value)

    var clipStartMs: Long
        get() = getLong(KEY_CLIP_START_MS, -1L)
        set(value) = putLong(KEY_CLIP_START_MS, value)

    var clipEndMs: Long
        get() = getLong(KEY_CLIP_END_MS, -1L)
        set(value) = putLong(KEY_CLIP_END_MS, value)

    var clipLoopPreview: Boolean
        get() = getBoolean(KEY_CLIP_LOOP_PREVIEW, false)
        set(value) = putBoolean(KEY_CLIP_LOOP_PREVIEW, value)

    var bookmarkPositionMs: Long
        get() = getLong(KEY_BOOKMARK_POSITION_MS, -1L)
        set(value) = putLong(KEY_BOOKMARK_POSITION_MS, value)

    fun resetToDefaults() {
        // commit() so the next read + Activity apply sees an empty store immediately
        prefs.edit().clear().commit()
    }

    companion object {
        private const val PREFS_NAME = "player_settings"

        // 播放
        private const val KEY_SPEED = "speed"
        private const val KEY_LOOP_MODE = "loop_mode"
        private const val KEY_SEEK_INTERVAL = "seek_interval"
        private const val KEY_REMEMBER_PROGRESS = "remember_progress"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        private const val KEY_PLAYBACK_END_BEHAVIOR = "playback_end_behavior"
        private const val KEY_HW_ACCEL = "hw_acceleration"
        private const val KEY_PAUSE_ON_EXIT = "pause_on_exit"
        private const val KEY_BG_AUDIO = "bg_audio"
        const val KEY_BG_NOTIFICATION_ENABLED = "bg_playback_notification_enabled"
        private const val KEY_SKIP_INTRO_OUTRO = "skip_intro_outro"
        private const val KEY_INTRO_SECONDS = "intro_seconds"
        private const val KEY_OUTRO_SECONDS = "outro_seconds"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_CONTROLS_AUTO_HIDE = "controls_auto_hide"
        private const val KEY_SEEK_THUMBNAIL_ENABLED = "seek_thumbnail_enabled"

        // 画面
        private const val KEY_ASPECT_RATIO = "aspect_ratio"
        private const val KEY_CONTENT_FRAME_MODE = "content_frame_mode"
        private const val KEY_ROTATION = "rotation"
        private const val KEY_MIRROR = "mirror"
        private const val KEY_AUTO_ORIENTATION = "auto_orientation_by_video"
        private const val KEY_VIDEO_DISPLAY_ENABLED = "video_display_enabled"
        const val KEY_BRIGHTNESS_ADJUSTMENT = "brightness_adjustment"
        private const val KEY_CONTRAST_ADJUSTMENT = "contrast_adjustment"
        private const val KEY_SATURATION_ADJUSTMENT = "saturation_adjustment"
        private const val KEY_PROGRESS_STYLE = "progress_style"
        private const val KEY_CONTROLS_OPACITY = "controls_opacity"
        private const val KEY_SETTINGS_PANEL_OPACITY = "settings_panel_opacity"
        /** 旧版：存的是「透明度」且 alpha=1-T/100，迁移为新语义（越大越不透明） */
        private const val KEY_SETTINGS_SHEET_TRANSPARENCY_LEGACY = "settings_sheet_transparency"
        /** 更早：存的是不透明度，与新语义一致 */
        private const val KEY_SETTINGS_SHEET_OPACITY_LEGACY = "settings_sheet_opacity"
        private const val KEY_SETTINGS_SHEET_BACKDROP_DIM = "settings_sheet_backdrop_dim"
        private const val KEY_SETTINGS_SHEET_BACKDROP_BLUR_DP = "settings_sheet_backdrop_blur_dp"

        // 声音
        private const val KEY_SPEED_PRESERVE_PITCH = "speed_preserve_pitch"
        private const val KEY_VOLUME_BOOST = "volume_boost"
        private const val KEY_AUDIO_CHANNEL = "audio_channel"
        private const val KEY_AUDIO_DELAY = "audio_delay"
        private const val KEY_AUDIO_MUTED = "audio_muted"

        // 字幕
        private const val KEY_SUBTITLE_SIZE = "subtitle_size"
        private const val KEY_SUBTITLE_COLOR = "subtitle_color"
        private const val KEY_SUBTITLE_BG = "subtitle_bg"
        private const val KEY_SUBTITLE_ENCODING = "subtitle_encoding"
        private const val KEY_SUBTITLE_DELAY_MS = "subtitle_delay_ms"
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
        private const val KEY_KEYBOARD_SHORTCUTS = "keyboard_shortcuts"
        private const val KEY_SUBTITLES_ENABLED = "subtitles_enabled"
        private const val KEY_SOFTWARE_AUDIO_DECODER = "software_audio_decoder"
        private const val KEY_AUDIO_SYNC_ENABLED = "audio_sync_enabled"
        private const val KEY_LAST_STREAM_URL = "last_stream_url"
        private const val KEY_CLIP_START_MS = "clip_start_ms"
        private const val KEY_CLIP_END_MS = "clip_end_ms"
        private const val KEY_CLIP_LOOP_PREVIEW = "clip_loop_preview"
        private const val KEY_BOOKMARK_POSITION_MS = "bookmark_position_ms"

        fun requiresImmediatePlayerApply(key: String?): Boolean {
            return key in setOf(
                KEY_SPEED,
                KEY_LOOP_MODE,
                KEY_KEEP_SCREEN_ON,
                KEY_ASPECT_RATIO,
                KEY_CONTENT_FRAME_MODE,
                KEY_ROTATION,
                KEY_MIRROR,
                KEY_VIDEO_DISPLAY_ENABLED,
                KEY_CONTROLS_OPACITY,
                KEY_SPEED_PRESERVE_PITCH,
                KEY_VOLUME_BOOST,
                KEY_AUDIO_CHANNEL,
                KEY_AUDIO_DELAY,
                KEY_AUDIO_MUTED,
                KEY_SUBTITLES_ENABLED,
                KEY_SUBTITLE_SIZE,
                KEY_SUBTITLE_COLOR,
                KEY_SUBTITLE_BG,
                KEY_SUBTITLE_DELAY_MS,
                KEY_SUBTITLE_POSITION,
                KEY_CONTROLS_AUTO_HIDE
            )
        }
    }
}
