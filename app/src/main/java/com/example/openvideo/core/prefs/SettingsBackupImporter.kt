package com.example.openvideo.core.prefs

/**
 * 从 [SettingsBackupSchema.Document] 恢复配置到 [PlayerPrefs] / [AppPrefs]。
 *
 * 恢复策略：
 * - 宽松（lenient）：Document 中 `null` 的字段**不**覆盖现有值，让未记录的偏好保持不变。
 * - 枚举字段通过 `fromKey()` 解析，无效值静默跳过（不覆盖）。
 * - 不做 IO，不持有 Context，便于 JVM 单测全量覆盖。
 */
object SettingsBackupImporter {

    /**
     * 将 [document] 中所有非 null 字段写回 [playerPrefs] / [appPrefs]。
     */
    fun apply(
        document: SettingsBackupSchema.Document,
        playerPrefs: PlayerPrefs,
        appPrefs: AppPrefs
    ) {
        applyPlayer(document.player, playerPrefs)
        applyApp(document.app, appPrefs)
    }

    private fun applyPlayer(section: SettingsBackupSchema.PlayerSection, prefs: PlayerPrefs) {
        section.speed?.let { prefs.speed = it }
        section.loopMode?.let { key -> LoopMode.fromKey(key).let { prefs.loopMode = it } }
        section.seekInterval?.let { prefs.seekInterval = it }
        section.rememberProgress?.let { prefs.rememberProgress = it }
        section.autoPlayNext?.let { prefs.autoPlayNext = it }
        section.playbackEndBehavior?.let { key ->
            prefs.playbackEndBehavior = PlaybackEndBehavior.fromKey(key)
        }
        section.hwAcceleration?.let { prefs.hwAcceleration = it }
        section.pauseOnExit?.let { prefs.pauseOnExit = it }
        section.bgAudio?.let { prefs.bgAudio = it }
        section.bgPlaybackNotificationEnabled?.let { prefs.bgPlaybackNotificationEnabled = it }
        section.skipIntroOutro?.let { prefs.skipIntroOutro = it }
        section.introSeconds?.let { prefs.introSeconds = it }
        section.outroSeconds?.let { prefs.outroSeconds = it }
        section.keepScreenOn?.let { prefs.keepScreenOn = it }
        section.controlsAutoHide?.let { prefs.controlsAutoHide = it }
        section.aspectRatio?.let { key -> prefs.aspectRatio = AspectRatio.fromKey(key) }
        section.contentFrameMode?.let { key -> prefs.contentFrameMode = ContentFrameMode.fromKey(key) }
        section.rotation?.let { prefs.rotation = it }
        section.mirror?.let { prefs.mirror = it }
        section.autoOrientationByVideo?.let { prefs.autoOrientationByVideo = it }
        section.videoDisplayEnabled?.let { prefs.videoDisplayEnabled = it }
        section.brightnessAdjustment?.let { prefs.brightnessAdjustment = it }
        section.contrastAdjustment?.let { prefs.contrastAdjustment = it }
        section.saturationAdjustment?.let { prefs.saturationAdjustment = it }
        section.progressStyle?.let { prefs.progressStyle = it }
        section.controlsOpacity?.let { prefs.controlsOpacity = it }
        section.settingsPanelOpacity?.let { prefs.settingsPanelOpacity = it }
        section.settingsSheetBackdropDimPercent?.let { prefs.settingsSheetBackdropDimPercent = it }
        section.settingsSheetBackdropBlurDp?.let { prefs.settingsSheetBackdropBlurDp = it }
        section.speedPreservePitch?.let { prefs.speedPreservePitch = it }
        section.volumeBoost?.let { prefs.volumeBoost = it }
        section.audioChannel?.let { key -> prefs.audioChannel = AudioChannel.fromKey(key) }
        section.audioDelay?.let { prefs.audioDelay = it }
        section.audioMuted?.let { prefs.audioMuted = it }
        section.softwareAudioDecoder?.let { prefs.softwareAudioDecoder = it }
        section.audioSyncEnabled?.let { prefs.audioSyncEnabled = it }
        section.subtitleSize?.let { prefs.subtitleSize = it }
        section.subtitleColor?.let { prefs.subtitleColor = it }
        section.subtitleBgStyle?.let { key -> prefs.subtitleBgStyle = SubtitleBgStyle.fromKey(key) }
        section.subtitlePosition?.let { prefs.subtitlePosition = it }
        section.subtitleEncoding?.let { prefs.subtitleEncoding = it }
        section.subtitleDelayMs?.let { prefs.subtitleDelayMs = it }
        section.subtitlesEnabled?.let { prefs.subtitlesEnabled = it }
        section.leftVerticalGesture?.let { key -> prefs.leftVerticalGesture = GestureAction.fromKey(key) }
        section.rightVerticalGesture?.let { key -> prefs.rightVerticalGesture = GestureAction.fromKey(key) }
        section.doubleTapAction?.let { key -> prefs.doubleTapAction = DoubleTapAction.fromKey(key) }
        section.longPressAction?.let { key -> prefs.longPressAction = LongPressAction.fromKey(key) }
        section.horizontalSwipeAction?.let { key -> prefs.horizontalSwipeAction = GestureAction.fromKey(key) }
        section.gestureSensitivity?.let { prefs.gestureSensitivity = it }
        section.doubleTapSeconds?.let { prefs.doubleTapSeconds = it }
        section.longPressSpeed?.let { prefs.longPressSpeed = it }
        section.swipeRange?.let { prefs.swipeRange = it }
        section.edgeSwipeBack?.let { prefs.edgeSwipeBack = it }
        section.keyboardShortcuts?.let { prefs.keyboardShortcuts = it }
    }

    private fun applyApp(section: SettingsBackupSchema.AppSection, prefs: AppPrefs) {
        section.themeMode?.let { key -> prefs.themeMode = ThemeMode.fromKey(key) }
        section.language?.let { lang -> prefs.language = lang }
        section.defaultAspectRatio?.let { key -> prefs.defaultAspectRatio = AspectRatio.fromKey(key) }
        section.defaultSpeed?.let { prefs.defaultSpeed = it }
        section.brightness?.let { prefs.brightness = it }
        section.viewMode?.let { prefs.viewMode = it }
        section.homeAllViewMode?.let { prefs.homeAllViewMode = it }
        section.homeRecentViewMode?.let { prefs.homeRecentViewMode = it }
        section.homeFavoriteViewMode?.let { prefs.homeFavoriteViewMode = it }
        section.sortField?.let { prefs.sortField = it }
        section.sortAsc?.let { prefs.sortAsc = it }
    }
}
