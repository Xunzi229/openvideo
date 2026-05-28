package com.example.openvideo.ui.player

import android.content.Context
import com.example.openvideo.R
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.DoubleTapAction
import com.example.openvideo.core.prefs.LongPressAction
import com.example.openvideo.core.prefs.LoopMode
import com.example.openvideo.core.prefs.PlaybackEndBehavior
import com.example.openvideo.core.prefs.SubtitleBgStyle
import java.util.Locale
import kotlin.math.round

data class PlayerSettingsSeekIntervalChoice(val seconds: Int, val labelRes: Int)

class PlayerSettingsFormatter(private val context: Context) {
    val rotationDegrees: List<Int> = listOf(0, 90, 180, 270)

    fun playbackSpeedLabelFor(speed: Float): String =
        "${String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')}x"

    fun seekIntervalChoices(): List<PlayerSettingsSeekIntervalChoice> = listOf(
        PlayerSettingsSeekIntervalChoice(5, R.string.settings_seek_5s),
        PlayerSettingsSeekIntervalChoice(10, R.string.settings_seek_10s),
        PlayerSettingsSeekIntervalChoice(15, R.string.settings_seek_15s),
        PlayerSettingsSeekIntervalChoice(30, R.string.settings_seek_30s)
    )

    fun seekIntervalLabelFor(seconds: Int): String =
        seekIntervalChoices().firstOrNull { it.seconds == seconds }?.let { context.getString(it.labelRes) }
            ?: context.getString(R.string.player_settings_seek_interval_seconds, seconds)

    fun rotationLabel(deg: Int): String = when (deg) {
        0 -> context.getString(R.string.settings_rotation_0)
        90 -> context.getString(R.string.settings_rotation_90)
        180 -> context.getString(R.string.settings_rotation_180)
        270 -> context.getString(R.string.settings_rotation_270)
        else -> context.getString(R.string.settings_rotation_0)
    }

    fun controlsAutoHideChoiceList(): List<Pair<Int, Int>> = listOf(
        0 to R.string.settings_hide_never,
        3 to R.string.settings_hide_3s,
        5 to R.string.settings_hide_5s,
        8 to R.string.settings_hide_8s
    )

    fun controlsAutoHideLabel(seconds: Int): String =
        controlsAutoHideChoiceList().firstOrNull { it.first == seconds }?.let { (_, res) ->
            context.getString(res)
        } ?: context.getString(R.string.player_settings_seek_interval_seconds, seconds)

    fun speedToProgress(speed: Float): Int =
        round(((speed.coerceIn(SPEED_MIN, SPEED_MAX) - SPEED_MIN) / SPEED_STEP).toDouble()).toInt()

    fun progressToSpeed(progress: Int): Float =
        (SPEED_MIN + progress.coerceIn(0, speedToProgress(SPEED_MAX)) * SPEED_STEP)
            .coerceIn(SPEED_MIN, SPEED_MAX)

    fun loopModeLabel(value: LoopMode): String = when (value) {
        LoopMode.OFF -> context.getString(R.string.settings_loop_off)
        LoopMode.SINGLE -> context.getString(R.string.settings_loop_single)
        LoopMode.LIST -> context.getString(R.string.settings_loop_list)
    }

    fun playbackEndBehaviorLabel(value: PlaybackEndBehavior): String =
        PlayerPlaybackEndBehaviorUi.label(context, value)

    fun aspectLabel(value: AspectRatio): String = when (value) {
        AspectRatio.FIT -> context.getString(R.string.player_sheet_original_ratio)
        AspectRatio.FILL -> context.getString(R.string.player_sheet_fill_screen)
        AspectRatio.CROP -> context.getString(R.string.settings_ratio_crop)
        AspectRatio.STRETCH -> context.getString(R.string.settings_ratio_stretch)
        AspectRatio.RATIO_4_3 -> context.getString(R.string.settings_ratio_4_3)
        AspectRatio.RATIO_16_9 -> context.getString(R.string.settings_ratio_16_9)
    }

    fun doubleTapLabel(value: DoubleTapAction): String = when (value) {
        DoubleTapAction.PLAY_PAUSE -> context.getString(R.string.settings_double_tap_pause)
        DoubleTapAction.FORWARD -> context.getString(R.string.settings_double_tap_forward)
        DoubleTapAction.BACKWARD -> context.getString(R.string.settings_double_tap_backward)
        DoubleTapAction.NONE -> context.getString(R.string.settings_double_tap_none)
    }

    fun longPressLabel(value: LongPressAction): String = when (value) {
        LongPressAction.SPEED -> context.getString(R.string.settings_double_tap_playback)
        LongPressAction.NONE -> context.getString(R.string.settings_double_tap_none)
    }

    fun gesturePresetLabel(value: PlayerGesturePreset): String = when (value) {
        PlayerGesturePreset.CLASSIC -> context.getString(R.string.settings_gesture_preset_classic)
        PlayerGesturePreset.MINIMAL -> context.getString(R.string.settings_gesture_preset_minimal)
        PlayerGesturePreset.BINGE -> context.getString(R.string.settings_gesture_preset_binge)
        PlayerGesturePreset.POWER_USER -> context.getString(R.string.settings_gesture_preset_power_user)
    }

    fun formatSavedTime(ms: Long): String =
        if (ms >= 0L) formatTime(ms) else context.getString(R.string.player_settings_value_none)

    fun formatTime(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSec = safeMs / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    fun progressStyleLabel(value: String): String = when (value) {
        "modern" -> context.getString(R.string.player_sheet_modern)
        "thin" -> context.getString(R.string.player_sheet_thin)
        else -> context.getString(R.string.player_sheet_default)
    }

    fun subtitleBgLabel(value: SubtitleBgStyle): String = when (value) {
        SubtitleBgStyle.NONE -> context.getString(R.string.settings_subtitle_bg_none)
        SubtitleBgStyle.SEMI_TRANSPARENT -> context.getString(R.string.settings_subtitle_bg_semi)
        SubtitleBgStyle.OPAQUE -> context.getString(R.string.settings_subtitle_bg_opaque)
    }

    fun subtitleEncodingLabel(value: String): String =
        if (value == "auto") context.getString(R.string.settings_encoding_auto) else value

    companion object {
        const val SPEED_MIN = 0.5f
        const val SPEED_MAX = 5.0f
        private const val SPEED_STEP = 0.25f
    }
}
