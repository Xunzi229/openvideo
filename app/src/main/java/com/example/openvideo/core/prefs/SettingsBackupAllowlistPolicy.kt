package com.example.openvideo.core.prefs

/**
 * 设置备份导出白名单：只允许 [SettingsBackupSchema] 已建模的非敏感 JSON 字段出现在导出文件中。
 */
object SettingsBackupAllowlistPolicy {

    /** Schema player 分区允许的 JSON 字段名（camelCase）。 */
    val EXPORTABLE_PLAYER_JSON_KEYS: Set<String> = setOf(
        "speed",
        "loopMode",
        "seekInterval",
        "rememberProgress",
        "autoPlayNext",
        "playbackEndBehavior",
        "hwAcceleration",
        "pauseOnExit",
        "bgAudio",
        "bgPlaybackNotificationEnabled",
        "skipIntroOutro",
        "introSeconds",
        "outroSeconds",
        "keepScreenOn",
        "controlsAutoHide",
        "aspectRatio",
        "contentFrameMode",
        "rotation",
        "mirror",
        "autoOrientationByVideo",
        "videoDisplayEnabled",
        "brightnessAdjustment",
        "contrastAdjustment",
        "saturationAdjustment",
        "progressStyle",
        "controlsOpacity",
        "settingsPanelOpacity",
        "settingsSheetBackdropDimPercent",
        "settingsSheetBackdropBlurDp",
        "speedPreservePitch",
        "volumeBoost",
        "audioChannel",
        "audioDelay",
        "audioMuted",
        "softwareAudioDecoder",
        "audioSyncEnabled",
        "subtitleSize",
        "subtitleColor",
        "subtitleBgStyle",
        "subtitlePosition",
        "subtitleEncoding",
        "subtitleDelayMs",
        "subtitlesEnabled",
        "leftVerticalGesture",
        "rightVerticalGesture",
        "doubleTapAction",
        "longPressAction",
        "horizontalSwipeAction",
        "gestureSensitivity",
        "doubleTapSeconds",
        "longPressSpeed",
        "swipeRange",
        "edgeSwipeBack",
        "keyboardShortcuts"
    )

    /** Schema app 分区允许的 JSON 字段名。 */
    val EXPORTABLE_APP_JSON_KEYS: Set<String> = setOf(
        "themeMode",
        "language",
        "defaultAspectRatio",
        "defaultSpeed",
        "brightness",
        "viewMode",
        "homeAllViewMode",
        "homeRecentViewMode",
        "homeFavoriteViewMode",
        "sortField",
        "sortAsc"
    )

    private val BLOCKED_SUBSTRINGS: Set<String> = buildSet {
        addAll(SettingsBackupSchema.EXCLUDED_PLAYER_PREF_KEYS)
        addAll(SettingsBackupSchema.EXCLUDED_APP_PREF_KEYS)
        add("externalSubtitleUri")
        add("lastStreamUrl")
        add("githubPendingDownloadUrl")
        add("pinnedFolderKeys")
        add("token")
        add("password")
        add("cookie")
        add("authorization")
        add("header")
        add("/storage/emulated/")
        add("content://")
    }

    fun isExportablePlayerPrefKey(key: String): Boolean =
        key !in SettingsBackupSchema.EXCLUDED_PLAYER_PREF_KEYS

    fun isExportableAppPrefKey(key: String): Boolean =
        key !in SettingsBackupSchema.EXCLUDED_APP_PREF_KEYS

    fun findLeakedSensitiveMarkers(exportJson: String): List<String> =
        BLOCKED_SUBSTRINGS.filter { marker ->
            exportJson.contains(marker, ignoreCase = true)
        }

    fun assertExportContainsNoSensitiveKeys(exportJson: String) {
        val leaked = findLeakedSensitiveMarkers(exportJson)
        check(leaked.isEmpty()) {
            "Export JSON must not contain sensitive markers: ${leaked.joinToString()}"
        }
    }
}
