package com.example.openvideo.core.prefs

import java.time.Instant

/**
 * 从 [PlayerPrefs] / [AppPrefs] 读取当前配置，映射为 [SettingsBackupSchema] 并导出 JSON。
 * 仅通过显式字段映射导出，敏感 pref 键不会进入备份文件。
 */
object SettingsBackupExporter {

    fun exportDocument(
        playerPrefs: PlayerPrefs,
        appPrefs: AppPrefs,
        exportedAt: Instant = Instant.now()
    ): SettingsBackupSchema.Document =
        SettingsBackupSchema.Document(
            schemaVersion = SettingsBackupSchema.SCHEMA_VERSION,
            exportedAt = SettingsBackupSchema.formatExportedAt(exportedAt),
            player = playerSectionFrom(playerPrefs),
            app = appSectionFrom(appPrefs)
        )

    fun exportJson(
        playerPrefs: PlayerPrefs,
        appPrefs: AppPrefs,
        exportedAt: Instant = Instant.now()
    ): String {
        val json = SettingsBackupSchema.encode(exportDocument(playerPrefs, appPrefs, exportedAt))
        SettingsBackupAllowlistPolicy.assertExportContainsNoSensitiveKeys(json)
        return json
    }

    internal fun playerSectionFrom(playerPrefs: PlayerPrefs): SettingsBackupSchema.PlayerSection =
        SettingsBackupSchema.PlayerSection(
            speed = playerPrefs.speed,
            loopMode = playerPrefs.loopMode.key,
            seekInterval = playerPrefs.seekInterval,
            rememberProgress = playerPrefs.rememberProgress,
            autoPlayNext = playerPrefs.autoPlayNext,
            playbackEndBehavior = playerPrefs.playbackEndBehavior.key,
            hwAcceleration = playerPrefs.hwAcceleration,
            pauseOnExit = playerPrefs.pauseOnExit,
            bgAudio = playerPrefs.bgAudio,
            bgPlaybackNotificationEnabled = playerPrefs.bgPlaybackNotificationEnabled,
            skipIntroOutro = playerPrefs.skipIntroOutro,
            introSeconds = playerPrefs.introSeconds,
            outroSeconds = playerPrefs.outroSeconds,
            keepScreenOn = playerPrefs.keepScreenOn,
            controlsAutoHide = playerPrefs.controlsAutoHide,
            aspectRatio = playerPrefs.aspectRatio.key,
            contentFrameMode = playerPrefs.contentFrameMode.key,
            rotation = playerPrefs.rotation,
            mirror = playerPrefs.mirror,
            autoOrientationByVideo = playerPrefs.autoOrientationByVideo,
            videoDisplayEnabled = playerPrefs.videoDisplayEnabled,
            brightnessAdjustment = playerPrefs.brightnessAdjustment,
            contrastAdjustment = playerPrefs.contrastAdjustment,
            saturationAdjustment = playerPrefs.saturationAdjustment,
            progressStyle = playerPrefs.progressStyle,
            controlsOpacity = playerPrefs.controlsOpacity,
            settingsPanelOpacity = playerPrefs.settingsPanelOpacity,
            settingsSheetBackdropDimPercent = playerPrefs.settingsSheetBackdropDimPercent,
            settingsSheetBackdropBlurDp = playerPrefs.settingsSheetBackdropBlurDp,
            speedPreservePitch = playerPrefs.speedPreservePitch,
            volumeBoost = playerPrefs.volumeBoost,
            audioChannel = playerPrefs.audioChannel.key,
            audioDelay = playerPrefs.audioDelay,
            audioMuted = playerPrefs.audioMuted,
            softwareAudioDecoder = playerPrefs.softwareAudioDecoder,
            audioSyncEnabled = playerPrefs.audioSyncEnabled,
            subtitleSize = playerPrefs.subtitleSize,
            subtitleColor = playerPrefs.subtitleColor,
            subtitleBgStyle = playerPrefs.subtitleBgStyle.key,
            subtitlePosition = playerPrefs.subtitlePosition,
            subtitleEncoding = playerPrefs.subtitleEncoding,
            subtitleDelayMs = playerPrefs.subtitleDelayMs,
            subtitlesEnabled = playerPrefs.subtitlesEnabled,
            leftVerticalGesture = playerPrefs.leftVerticalGesture.key,
            rightVerticalGesture = playerPrefs.rightVerticalGesture.key,
            doubleTapAction = playerPrefs.doubleTapAction.key,
            longPressAction = playerPrefs.longPressAction.key,
            horizontalSwipeAction = playerPrefs.horizontalSwipeAction.key,
            gestureSensitivity = playerPrefs.gestureSensitivity,
            doubleTapSeconds = playerPrefs.doubleTapSeconds,
            longPressSpeed = playerPrefs.longPressSpeed,
            swipeRange = playerPrefs.swipeRange,
            edgeSwipeBack = playerPrefs.edgeSwipeBack,
            keyboardShortcuts = playerPrefs.keyboardShortcuts
        )

    internal fun appSectionFrom(appPrefs: AppPrefs): SettingsBackupSchema.AppSection =
        SettingsBackupSchema.AppSection(
            themeMode = appPrefs.themeMode.key,
            language = appPrefs.language,
            defaultAspectRatio = appPrefs.defaultAspectRatio.key,
            defaultSpeed = appPrefs.defaultSpeed,
            brightness = appPrefs.brightness,
            viewMode = appPrefs.viewMode,
            homeAllViewMode = appPrefs.homeAllViewMode,
            homeRecentViewMode = appPrefs.homeRecentViewMode,
            homeFavoriteViewMode = appPrefs.homeFavoriteViewMode,
            sortField = appPrefs.sortField,
            sortAsc = appPrefs.sortAsc
        )
}
