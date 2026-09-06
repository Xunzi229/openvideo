package com.openvideo.app.ui.player

import android.app.Application
import com.openvideo.app.R
import com.openvideo.app.core.prefs.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PlayerSettingsFormatterBehaviorTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val formatter get() = PlayerSettingsFormatter(context)

    @Test fun numericLabelsAndSliderBoundariesRemainConsistent() {
        val f = formatter
        for ((speed, label) in listOf(1f to "1x", 1.25f to "1.25x", 0.5f to "0.5x", 5f to "5x")) {
            assertEquals(label, f.playbackSpeedLabelFor(speed))
            assertEquals(speed, f.progressToSpeed(f.speedToProgress(speed)), 0.001f)
        }
        assertEquals(0, f.speedToProgress(-1f))
        assertEquals(18, f.speedToProgress(9f))
        assertEquals(0.5f, f.progressToSpeed(-1), 0f)
        assertEquals(5f, f.progressToSpeed(100), 0f)
        assertEquals(listOf(5, 10, 15, 30), f.seekIntervalChoices().map { it.seconds })
        f.seekIntervalChoices().forEach { assertEquals(context.getString(it.labelRes), f.seekIntervalLabelFor(it.seconds)) }
        assertEquals(context.getString(R.string.player_settings_seek_interval_seconds, 12), f.seekIntervalLabelFor(12))
        f.controlsAutoHideChoiceList().forEach { (seconds, res) -> assertEquals(context.getString(res), f.controlsAutoHideLabel(seconds)) }
        assertEquals(context.getString(R.string.player_settings_seek_interval_seconds, 12), f.controlsAutoHideLabel(12))
        assertEquals(listOf(0, 90, 180, 270), f.rotationDegrees)
        listOf(0 to R.string.settings_rotation_0, 90 to R.string.settings_rotation_90,
            180 to R.string.settings_rotation_180, 270 to R.string.settings_rotation_270,
            -1 to R.string.settings_rotation_0).forEach { (value, res) -> assertEquals(context.getString(res), f.rotationLabel(value)) }
    }

    @Test fun eachSettingUsesItsCorrespondingLocalizedLabel() {
        val f = formatter
        fun <T> check(values: List<T>, resources: List<Int>, label: (T) -> String) {
            assertEquals(resources.map { context.getString(it) }, values.map(label))
        }
        check(LoopMode.entries, listOf(R.string.settings_loop_off, R.string.settings_loop_single, R.string.settings_loop_list), f::loopModeLabel)
        check(listOf(AspectRatio.FIT, AspectRatio.FILL, AspectRatio.CROP, AspectRatio.STRETCH, AspectRatio.RATIO_4_3, AspectRatio.RATIO_16_9),
            listOf(R.string.player_sheet_original_ratio, R.string.player_sheet_fill_screen, R.string.settings_ratio_crop,
                R.string.settings_ratio_stretch, R.string.settings_ratio_4_3, R.string.settings_ratio_16_9), f::aspectLabel)
        check(listOf(DoubleTapAction.PLAY_PAUSE, DoubleTapAction.FORWARD, DoubleTapAction.BACKWARD, DoubleTapAction.NONE),
            listOf(R.string.settings_double_tap_pause, R.string.settings_double_tap_forward, R.string.settings_double_tap_backward,
                R.string.settings_double_tap_none), f::doubleTapLabel)
        check(listOf(LongPressAction.SPEED, LongPressAction.NONE), listOf(R.string.settings_double_tap_playback, R.string.settings_double_tap_none), f::longPressLabel)
        check(PlayerGesturePreset.entries, listOf(R.string.settings_gesture_preset_classic, R.string.settings_gesture_preset_minimal,
            R.string.settings_gesture_preset_binge, R.string.settings_gesture_preset_power_user), f::gesturePresetLabel)
        check(PlaybackEndBehavior.entries, listOf(R.string.settings_playback_end_follow, R.string.settings_playback_end_next,
            R.string.settings_playback_end_replay, R.string.settings_playback_end_stop, R.string.settings_playback_end_return), f::playbackEndBehaviorLabel)
        check(SubtitleBgStyle.entries, listOf(R.string.settings_subtitle_bg_none, R.string.settings_subtitle_bg_semi, R.string.settings_subtitle_bg_opaque), f::subtitleBgLabel)
        check(listOf("modern", "thin", "unknown"), listOf(R.string.player_sheet_modern, R.string.player_sheet_thin, R.string.player_sheet_default), f::progressStyleLabel)
        assertEquals(context.getString(R.string.settings_encoding_auto), f.subtitleEncodingLabel("auto"))
        assertEquals("GBK", f.subtitleEncodingLabel("GBK"))
        assertEquals(context.getString(R.string.player_settings_value_none), f.formatSavedTime(-1))
        assertEquals("00:00", f.formatTime(-1000))
        assertEquals("01:01", f.formatSavedTime(61_000))
        assertEquals("1:01:01", f.formatTime(3_661_000))
    }
}
