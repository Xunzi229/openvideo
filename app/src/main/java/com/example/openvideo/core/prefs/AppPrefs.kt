package com.example.openvideo.core.prefs

import android.content.Context

class AppPrefs(context: Context) : PrefsManager(context, PREFS_NAME) {

    var themeMode: ThemeMode
        get() = ThemeMode.fromKey(getString(KEY_THEME_MODE, "dark"))
        set(value) = putString(KEY_THEME_MODE, value.key)

    var language: String
        get() = normalizeLanguage(getString(KEY_LANGUAGE, "system"))
        set(value) {
            val v = normalizeLanguage(value)
            // commit() so prefs + AppCompatDelegate see one canonical value before any recreate
            prefs.edit().putString(KEY_LANGUAGE, v).commit()
        }

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

    var homeAllViewMode: String
        get() = getString(KEY_HOME_ALL_VIEW_MODE, viewMode)
        set(value) = putString(KEY_HOME_ALL_VIEW_MODE, value)

    var homeRecentViewMode: String
        get() = getString(KEY_HOME_RECENT_VIEW_MODE, viewMode)
        set(value) = putString(KEY_HOME_RECENT_VIEW_MODE, value)

    var homeFavoriteViewMode: String
        get() = getString(KEY_HOME_FAVORITE_VIEW_MODE, viewMode)
        set(value) = putString(KEY_HOME_FAVORITE_VIEW_MODE, value)

    var sortField: String
        get() = getString(KEY_SORT_FIELD, "date")
        set(value) = putString(KEY_SORT_FIELD, value)

    var sortAsc: Boolean
        get() = getBoolean(KEY_SORT_ASC, false)
        set(value) = putBoolean(KEY_SORT_ASC, value)

    /** Last successful GitHub release API check (epoch ms). */
    var lastGitHubReleaseCheckMs: Long
        get() = getLong(KEY_LAST_GITHUB_RELEASE_CHECK_MS, 0L)
        set(value) = putLong(KEY_LAST_GITHUB_RELEASE_CHECK_MS, value)

    /**
     * True when last check found a release newer than the installed app.
     * Shown as a badge next to "Check for updates" (no dialog).
     */
    var githubUpdateBadgeVisible: Boolean
        get() = getBoolean(KEY_GITHUB_UPDATE_BADGE, false)
        set(value) = putBoolean(KEY_GITHUB_UPDATE_BADGE, value)

    /** Preferred APK or release page URL from last successful newer-than-local check. */
    var githubPendingDownloadUrl: String
        get() = getString(KEY_GITHUB_PENDING_DOWNLOAD_URL, "")
        set(value) = putString(KEY_GITHUB_PENDING_DOWNLOAD_URL, value)

    companion object {
        private const val PREFS_NAME = "app_settings"

        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"

        /** Canonical keys: system | zh | en. Unknown legacy values map to system so UI cycling stays stable. */
        internal fun normalizeLanguage(raw: String): String = when (raw) {
            "zh", "zh-CN", "zh-rCN", "zh_CN" -> "zh"
            "en", "en-US", "en-rUS", "en_GB" -> "en"
            "system" -> "system"
            else -> "system"
        }
        private const val KEY_DEFAULT_ASPECT_RATIO = "default_aspect_ratio"
        private const val KEY_DEFAULT_SPEED = "default_speed"
        private const val KEY_BRIGHTNESS = "brightness"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_HOME_ALL_VIEW_MODE = "home_all_view_mode"
        private const val KEY_HOME_RECENT_VIEW_MODE = "home_recent_view_mode"
        private const val KEY_HOME_FAVORITE_VIEW_MODE = "home_favorite_view_mode"
        private const val KEY_SORT_FIELD = "sort_field"
        private const val KEY_SORT_ASC = "sort_asc"
        private const val KEY_LAST_GITHUB_RELEASE_CHECK_MS = "last_github_release_check_ms"
        private const val KEY_GITHUB_UPDATE_BADGE = "github_update_badge_visible"
        private const val KEY_GITHUB_PENDING_DOWNLOAD_URL = "github_pending_download_url"
    }
}
