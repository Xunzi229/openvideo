package com.example.openvideo.core.prefs

import android.content.Context

class AppPrefs(context: Context) : PrefsManager(context, PREFS_NAME) {

    var themeMode: ThemeMode
        get() = ThemeMode.fromKey(getString(KEY_THEME_MODE, "dark"))
        set(value) = putString(KEY_THEME_MODE, value.key)

    var language: String
        get() = getString(KEY_LANGUAGE, "system")
        set(value) = putString(KEY_LANGUAGE, value)

    var defaultAspectRatio: AspectRatio
        get() = AspectRatio.fromKey(getString(KEY_DEFAULT_ASPECT_RATIO, "fit"))
        set(value) = putString(KEY_DEFAULT_ASPECT_RATIO, value.key)

    var defaultSpeed: Float
        get() = getFloat(KEY_DEFAULT_SPEED, 1.0f)
        set(value) = putFloat(KEY_DEFAULT_SPEED, value)

    var brightness: Float
        get() = getFloat(KEY_BRIGHTNESS, -1f)
        set(value) = putFloat(KEY_BRIGHTNESS, value)

    var viewMode: String
        get() = getString(KEY_VIEW_MODE, "list")
        set(value) = putString(KEY_VIEW_MODE, value)

    var sortField: String
        get() = getString(KEY_SORT_FIELD, "date")
        set(value) = putString(KEY_SORT_FIELD, value)

    var sortAsc: Boolean
        get() = getBoolean(KEY_SORT_ASC, false)
        set(value) = putBoolean(KEY_SORT_ASC, value)

    companion object {
        private const val PREFS_NAME = "app_settings"

        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_DEFAULT_ASPECT_RATIO = "default_aspect_ratio"
        private const val KEY_DEFAULT_SPEED = "default_speed"
        private const val KEY_BRIGHTNESS = "brightness"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_SORT_FIELD = "sort_field"
        private const val KEY_SORT_ASC = "sort_asc"
    }
}
