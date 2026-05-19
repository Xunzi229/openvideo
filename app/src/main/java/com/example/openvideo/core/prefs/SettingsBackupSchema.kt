package com.example.openvideo.core.prefs

import java.time.Instant

/**
 * Phase 0.3 设置备份 JSON 契约：版本号、导出时间、播放器/App 非敏感配置分区。
 *
 * 敏感字段（路径、URL、会话位点等） intentionally 不在 [PlayerSection] / [AppSection] 中，
 * 见 [EXCLUDED_PLAYER_PREF_KEYS] / [EXCLUDED_APP_PREF_KEYS]，由 [SettingsBackupAllowlistPolicy] / [SettingsBackupExporter] enforce。
 */
object SettingsBackupSchema {

    const val SCHEMA_VERSION = 1

    /** 建议文件名后缀，便于文件选择器过滤（0.3.3 导出时使用）。 */
    const val SUGGESTED_FILENAME = "openvideo-settings.json"

    /** [ActivityResultContracts.CreateDocument] MIME 类型。 */
    const val MIME_TYPE = "application/json"

    /**
     * 不参与备份的 PlayerPrefs 键（含私有路径 / token / 会话状态）。
     */
    val EXCLUDED_PLAYER_PREF_KEYS: Set<String> = setOf(
        PlayerPrefs.KEY_EXTERNAL_SUBTITLE,
        "last_stream_url",
        "clip_start_ms",
        "clip_end_ms",
        "clip_loop_preview",
        "bookmark_position_ms"
    )

    /**
     * 不参与备份的 AppPrefs 键（含本地路径集合 / 更新缓存 URL）。
     */
    val EXCLUDED_APP_PREF_KEYS: Set<String> = setOf(
        "pinned_folder_keys",
        "last_github_release_check_ms",
        "github_update_badge_visible",
        "github_pending_download_url"
    )

    data class Document(
        val schemaVersion: Int,
        val exportedAt: String,
        val player: PlayerSection = PlayerSection(),
        val app: AppSection = AppSection()
    )

    data class PlayerSection(
        val speed: Float? = null,
        val loopMode: String? = null,
        val seekInterval: Int? = null,
        val rememberProgress: Boolean? = null,
        val autoPlayNext: Boolean? = null,
        val playbackEndBehavior: String? = null,
        val hwAcceleration: Boolean? = null,
        val pauseOnExit: Boolean? = null,
        val bgAudio: Boolean? = null,
        val bgPlaybackNotificationEnabled: Boolean? = null,
        val skipIntroOutro: Boolean? = null,
        val introSeconds: Int? = null,
        val outroSeconds: Int? = null,
        val keepScreenOn: Boolean? = null,
        val controlsAutoHide: Int? = null,
        val aspectRatio: String? = null,
        val contentFrameMode: String? = null,
        val rotation: Int? = null,
        val mirror: Boolean? = null,
        val autoOrientationByVideo: Boolean? = null,
        val videoDisplayEnabled: Boolean? = null,
        val brightnessAdjustment: Int? = null,
        val contrastAdjustment: Int? = null,
        val saturationAdjustment: Int? = null,
        val progressStyle: String? = null,
        val controlsOpacity: Int? = null,
        val settingsPanelOpacity: Int? = null,
        val settingsSheetBackdropDimPercent: Int? = null,
        val settingsSheetBackdropBlurDp: Int? = null,
        val speedPreservePitch: Boolean? = null,
        val volumeBoost: Boolean? = null,
        val audioChannel: String? = null,
        val audioDelay: Int? = null,
        val audioMuted: Boolean? = null,
        val softwareAudioDecoder: Boolean? = null,
        val audioSyncEnabled: Boolean? = null,
        val subtitleSize: Int? = null,
        val subtitleColor: Int? = null,
        val subtitleBgStyle: String? = null,
        val subtitlePosition: Float? = null,
        val subtitleEncoding: String? = null,
        val subtitleDelayMs: Int? = null,
        val subtitlesEnabled: Boolean? = null,
        val leftVerticalGesture: String? = null,
        val rightVerticalGesture: String? = null,
        val doubleTapAction: String? = null,
        val longPressAction: String? = null,
        val horizontalSwipeAction: String? = null,
        val gestureSensitivity: Int? = null,
        val doubleTapSeconds: Int? = null,
        val longPressSpeed: Float? = null,
        val swipeRange: Int? = null,
        val edgeSwipeBack: Boolean? = null,
        val keyboardShortcuts: Boolean? = null
    )

    data class AppSection(
        val themeMode: String? = null,
        val language: String? = null,
        val defaultAspectRatio: String? = null,
        val defaultSpeed: Float? = null,
        val brightness: Float? = null,
        val viewMode: String? = null,
        val homeAllViewMode: String? = null,
        val homeRecentViewMode: String? = null,
        val homeFavoriteViewMode: String? = null,
        val sortField: String? = null,
        val sortAsc: Boolean? = null
    )

    sealed class ParseResult {
        data class Success(val document: Document) : ParseResult()
        data class Failure(val reason: Reason, val message: String) : ParseResult()
    }

    enum class Reason {
        INVALID_JSON,
        UNSUPPORTED_VERSION,
        MISSING_REQUIRED_FIELD,
        INVALID_EXPORTED_AT
    }

    fun newDocument(exportedAt: Instant = Instant.now()): Document =
        Document(
            schemaVersion = SCHEMA_VERSION,
            exportedAt = formatExportedAt(exportedAt),
            player = PlayerSection(),
            app = AppSection()
        )

    fun formatExportedAt(instant: Instant): String = instant.toString()

    fun encode(document: Document): String =
        SettingsBackupJson.stringify(
            SettingsBackupJson.Value.Object(
                mapOf(
                    "schemaVersion" to SettingsBackupJson.Value.Number(document.schemaVersion.toString()),
                    "exportedAt" to SettingsBackupJson.Value.Text(document.exportedAt),
                    "player" to playerToJson(document.player),
                    "app" to appToJson(document.app)
                )
            )
        )

    fun decode(json: String): ParseResult {
        val root = try {
            SettingsBackupJson.parseObject(json)
        } catch (_: SettingsBackupJson.ParseException) {
            return ParseResult.Failure(Reason.INVALID_JSON, "Malformed JSON")
        } catch (_: Exception) {
            return ParseResult.Failure(Reason.INVALID_JSON, "Malformed JSON")
        }

        val schemaVersion = SettingsBackupJson.intOrNull(root, "schemaVersion")
            ?: return ParseResult.Failure(Reason.MISSING_REQUIRED_FIELD, "Missing schemaVersion")
        if (schemaVersion != SCHEMA_VERSION) {
            return ParseResult.Failure(
                Reason.UNSUPPORTED_VERSION,
                "Unsupported schemaVersion: $schemaVersion"
            )
        }

        val exportedAt = SettingsBackupJson.stringOrNull(root, "exportedAt")
            ?: return ParseResult.Failure(Reason.MISSING_REQUIRED_FIELD, "Missing exportedAt")
        if (!isValidExportedAt(exportedAt)) {
            return ParseResult.Failure(Reason.INVALID_EXPORTED_AT, "Invalid exportedAt: $exportedAt")
        }

        return ParseResult.Success(
            Document(
                schemaVersion = schemaVersion,
                exportedAt = exportedAt,
                player = playerFromJson(SettingsBackupJson.objectOrEmpty(root, "player")),
                app = appFromJson(SettingsBackupJson.objectOrEmpty(root, "app"))
            )
        )
    }

    internal fun isValidExportedAt(value: String): Boolean =
        runCatching { Instant.parse(value) }.isSuccess ||
            ISO_EXPORTED_AT.matches(value)

    private fun playerToJson(section: PlayerSection): SettingsBackupJson.Value.Object =
        jsonObject {
            putFloat("speed", section.speed)
            putString("loopMode", section.loopMode)
            putInt("seekInterval", section.seekInterval)
            putBoolean("rememberProgress", section.rememberProgress)
            putBoolean("autoPlayNext", section.autoPlayNext)
            putString("playbackEndBehavior", section.playbackEndBehavior)
            putBoolean("hwAcceleration", section.hwAcceleration)
            putBoolean("pauseOnExit", section.pauseOnExit)
            putBoolean("bgAudio", section.bgAudio)
            putBoolean("bgPlaybackNotificationEnabled", section.bgPlaybackNotificationEnabled)
            putBoolean("skipIntroOutro", section.skipIntroOutro)
            putInt("introSeconds", section.introSeconds)
            putInt("outroSeconds", section.outroSeconds)
            putBoolean("keepScreenOn", section.keepScreenOn)
            putInt("controlsAutoHide", section.controlsAutoHide)
            putString("aspectRatio", section.aspectRatio)
            putString("contentFrameMode", section.contentFrameMode)
            putInt("rotation", section.rotation)
            putBoolean("mirror", section.mirror)
            putBoolean("autoOrientationByVideo", section.autoOrientationByVideo)
            putBoolean("videoDisplayEnabled", section.videoDisplayEnabled)
            putInt("brightnessAdjustment", section.brightnessAdjustment)
            putInt("contrastAdjustment", section.contrastAdjustment)
            putInt("saturationAdjustment", section.saturationAdjustment)
            putString("progressStyle", section.progressStyle)
            putInt("controlsOpacity", section.controlsOpacity)
            putInt("settingsPanelOpacity", section.settingsPanelOpacity)
            putInt("settingsSheetBackdropDimPercent", section.settingsSheetBackdropDimPercent)
            putInt("settingsSheetBackdropBlurDp", section.settingsSheetBackdropBlurDp)
            putBoolean("speedPreservePitch", section.speedPreservePitch)
            putBoolean("volumeBoost", section.volumeBoost)
            putString("audioChannel", section.audioChannel)
            putInt("audioDelay", section.audioDelay)
            putBoolean("audioMuted", section.audioMuted)
            putBoolean("softwareAudioDecoder", section.softwareAudioDecoder)
            putBoolean("audioSyncEnabled", section.audioSyncEnabled)
            putInt("subtitleSize", section.subtitleSize)
            putInt("subtitleColor", section.subtitleColor)
            putString("subtitleBgStyle", section.subtitleBgStyle)
            putFloat("subtitlePosition", section.subtitlePosition)
            putString("subtitleEncoding", section.subtitleEncoding)
            putInt("subtitleDelayMs", section.subtitleDelayMs)
            putBoolean("subtitlesEnabled", section.subtitlesEnabled)
            putString("leftVerticalGesture", section.leftVerticalGesture)
            putString("rightVerticalGesture", section.rightVerticalGesture)
            putString("doubleTapAction", section.doubleTapAction)
            putString("longPressAction", section.longPressAction)
            putString("horizontalSwipeAction", section.horizontalSwipeAction)
            putInt("gestureSensitivity", section.gestureSensitivity)
            putInt("doubleTapSeconds", section.doubleTapSeconds)
            putFloat("longPressSpeed", section.longPressSpeed)
            putInt("swipeRange", section.swipeRange)
            putBoolean("edgeSwipeBack", section.edgeSwipeBack)
            putBoolean("keyboardShortcuts", section.keyboardShortcuts)
        }

    private fun appToJson(section: AppSection): SettingsBackupJson.Value.Object =
        jsonObject {
            putString("themeMode", section.themeMode)
            putString("language", section.language)
            putString("defaultAspectRatio", section.defaultAspectRatio)
            putFloat("defaultSpeed", section.defaultSpeed)
            putFloat("brightness", section.brightness)
            putString("viewMode", section.viewMode)
            putString("homeAllViewMode", section.homeAllViewMode)
            putString("homeRecentViewMode", section.homeRecentViewMode)
            putString("homeFavoriteViewMode", section.homeFavoriteViewMode)
            putString("sortField", section.sortField)
            putBoolean("sortAsc", section.sortAsc)
        }

    private fun playerFromJson(json: SettingsBackupJson.Value.Object): PlayerSection =
        PlayerSection(
            speed = SettingsBackupJson.floatOrNull(json, "speed"),
            loopMode = SettingsBackupJson.stringOrNull(json, "loopMode"),
            seekInterval = SettingsBackupJson.intOrNull(json, "seekInterval"),
            rememberProgress = SettingsBackupJson.booleanOrNull(json, "rememberProgress"),
            autoPlayNext = SettingsBackupJson.booleanOrNull(json, "autoPlayNext"),
            playbackEndBehavior = SettingsBackupJson.stringOrNull(json, "playbackEndBehavior"),
            hwAcceleration = SettingsBackupJson.booleanOrNull(json, "hwAcceleration"),
            pauseOnExit = SettingsBackupJson.booleanOrNull(json, "pauseOnExit"),
            bgAudio = SettingsBackupJson.booleanOrNull(json, "bgAudio"),
            bgPlaybackNotificationEnabled = SettingsBackupJson.booleanOrNull(json, "bgPlaybackNotificationEnabled"),
            skipIntroOutro = SettingsBackupJson.booleanOrNull(json, "skipIntroOutro"),
            introSeconds = SettingsBackupJson.intOrNull(json, "introSeconds"),
            outroSeconds = SettingsBackupJson.intOrNull(json, "outroSeconds"),
            keepScreenOn = SettingsBackupJson.booleanOrNull(json, "keepScreenOn"),
            controlsAutoHide = SettingsBackupJson.intOrNull(json, "controlsAutoHide"),
            aspectRatio = SettingsBackupJson.stringOrNull(json, "aspectRatio"),
            contentFrameMode = SettingsBackupJson.stringOrNull(json, "contentFrameMode"),
            rotation = SettingsBackupJson.intOrNull(json, "rotation"),
            mirror = SettingsBackupJson.booleanOrNull(json, "mirror"),
            autoOrientationByVideo = SettingsBackupJson.booleanOrNull(json, "autoOrientationByVideo"),
            videoDisplayEnabled = SettingsBackupJson.booleanOrNull(json, "videoDisplayEnabled"),
            brightnessAdjustment = SettingsBackupJson.intOrNull(json, "brightnessAdjustment"),
            contrastAdjustment = SettingsBackupJson.intOrNull(json, "contrastAdjustment"),
            saturationAdjustment = SettingsBackupJson.intOrNull(json, "saturationAdjustment"),
            progressStyle = SettingsBackupJson.stringOrNull(json, "progressStyle"),
            controlsOpacity = SettingsBackupJson.intOrNull(json, "controlsOpacity"),
            settingsPanelOpacity = SettingsBackupJson.intOrNull(json, "settingsPanelOpacity"),
            settingsSheetBackdropDimPercent = SettingsBackupJson.intOrNull(json, "settingsSheetBackdropDimPercent"),
            settingsSheetBackdropBlurDp = SettingsBackupJson.intOrNull(json, "settingsSheetBackdropBlurDp"),
            speedPreservePitch = SettingsBackupJson.booleanOrNull(json, "speedPreservePitch"),
            volumeBoost = SettingsBackupJson.booleanOrNull(json, "volumeBoost"),
            audioChannel = SettingsBackupJson.stringOrNull(json, "audioChannel"),
            audioDelay = SettingsBackupJson.intOrNull(json, "audioDelay"),
            audioMuted = SettingsBackupJson.booleanOrNull(json, "audioMuted"),
            softwareAudioDecoder = SettingsBackupJson.booleanOrNull(json, "softwareAudioDecoder"),
            audioSyncEnabled = SettingsBackupJson.booleanOrNull(json, "audioSyncEnabled"),
            subtitleSize = SettingsBackupJson.intOrNull(json, "subtitleSize"),
            subtitleColor = SettingsBackupJson.intOrNull(json, "subtitleColor"),
            subtitleBgStyle = SettingsBackupJson.stringOrNull(json, "subtitleBgStyle"),
            subtitlePosition = SettingsBackupJson.floatOrNull(json, "subtitlePosition"),
            subtitleEncoding = SettingsBackupJson.stringOrNull(json, "subtitleEncoding"),
            subtitleDelayMs = SettingsBackupJson.intOrNull(json, "subtitleDelayMs"),
            subtitlesEnabled = SettingsBackupJson.booleanOrNull(json, "subtitlesEnabled"),
            leftVerticalGesture = SettingsBackupJson.stringOrNull(json, "leftVerticalGesture"),
            rightVerticalGesture = SettingsBackupJson.stringOrNull(json, "rightVerticalGesture"),
            doubleTapAction = SettingsBackupJson.stringOrNull(json, "doubleTapAction"),
            longPressAction = SettingsBackupJson.stringOrNull(json, "longPressAction"),
            horizontalSwipeAction = SettingsBackupJson.stringOrNull(json, "horizontalSwipeAction"),
            gestureSensitivity = SettingsBackupJson.intOrNull(json, "gestureSensitivity"),
            doubleTapSeconds = SettingsBackupJson.intOrNull(json, "doubleTapSeconds"),
            longPressSpeed = SettingsBackupJson.floatOrNull(json, "longPressSpeed"),
            swipeRange = SettingsBackupJson.intOrNull(json, "swipeRange"),
            edgeSwipeBack = SettingsBackupJson.booleanOrNull(json, "edgeSwipeBack"),
            keyboardShortcuts = SettingsBackupJson.booleanOrNull(json, "keyboardShortcuts")
        )

    private fun appFromJson(json: SettingsBackupJson.Value.Object): AppSection =
        AppSection(
            themeMode = SettingsBackupJson.stringOrNull(json, "themeMode"),
            language = SettingsBackupJson.stringOrNull(json, "language"),
            defaultAspectRatio = SettingsBackupJson.stringOrNull(json, "defaultAspectRatio"),
            defaultSpeed = SettingsBackupJson.floatOrNull(json, "defaultSpeed"),
            brightness = SettingsBackupJson.floatOrNull(json, "brightness"),
            viewMode = SettingsBackupJson.stringOrNull(json, "viewMode"),
            homeAllViewMode = SettingsBackupJson.stringOrNull(json, "homeAllViewMode"),
            homeRecentViewMode = SettingsBackupJson.stringOrNull(json, "homeRecentViewMode"),
            homeFavoriteViewMode = SettingsBackupJson.stringOrNull(json, "homeFavoriteViewMode"),
            sortField = SettingsBackupJson.stringOrNull(json, "sortField"),
            sortAsc = SettingsBackupJson.booleanOrNull(json, "sortAsc")
        )

    private fun jsonObject(
        block: MutableMap<String, SettingsBackupJson.Value>.() -> Unit
    ): SettingsBackupJson.Value.Object {
        val entries = linkedMapOf<String, SettingsBackupJson.Value>()
        entries.block()
        return SettingsBackupJson.Value.Object(entries)
    }

    private fun MutableMap<String, SettingsBackupJson.Value>.putString(key: String, value: String?) {
        if (value != null) this[key] = SettingsBackupJson.Value.Text(value)
    }

    private fun MutableMap<String, SettingsBackupJson.Value>.putInt(key: String, value: Int?) {
        if (value != null) this[key] = SettingsBackupJson.Value.Number(value.toString())
    }

    private fun MutableMap<String, SettingsBackupJson.Value>.putFloat(key: String, value: Float?) {
        if (value != null) this[key] = SettingsBackupJson.Value.Number(value.toString())
    }

    private fun MutableMap<String, SettingsBackupJson.Value>.putBoolean(key: String, value: Boolean?) {
        if (value != null) this[key] = SettingsBackupJson.Value.Boolean(value)
    }

    /** 兼容无纳秒的 UTC 导出时间（例如 2026-05-17T00:00:00Z）。 */
    private val ISO_EXPORTED_AT =
        Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$""")
}
