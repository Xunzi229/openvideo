package com.openvideo.app.core.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingEnumsTest {
    @Test fun loopModes() = checkKeys(LoopMode.entries, { it.key }, LoopMode::fromKey, LoopMode.LIST)
    @Test fun playbackEndBehaviors() = checkKeys(PlaybackEndBehavior.entries, { it.key }, PlaybackEndBehavior::fromKey, PlaybackEndBehavior.FOLLOW_SETTINGS)
    @Test fun aspectRatios() = checkKeys(AspectRatio.entries, { it.key }, AspectRatio::fromKey, AspectRatio.FIT)
    @Test fun contentFrames() = checkKeys(ContentFrameMode.entries, { it.key }, ContentFrameMode::fromKey, ContentFrameMode.OFF)
    @Test fun audioChannels() = checkKeys(AudioChannel.entries, { it.key }, AudioChannel::fromKey, AudioChannel.STEREO)
    @Test fun subtitleBackgrounds() = checkKeys(SubtitleBgStyle.entries, { it.key }, SubtitleBgStyle::fromKey, SubtitleBgStyle.SEMI_TRANSPARENT)
    @Test fun gestureActions() = checkKeys(GestureAction.entries, { it.key }, GestureAction::fromKey, GestureAction.NONE)
    @Test fun doubleTapActions() = checkKeys(DoubleTapAction.entries, { it.key }, DoubleTapAction::fromKey, DoubleTapAction.PLAY_PAUSE)
    @Test fun longPressActions() = checkKeys(LongPressAction.entries, { it.key }, LongPressAction::fromKey, LongPressAction.SPEED)
    @Test fun themes() = checkKeys(ThemeMode.entries, { it.key }, ThemeMode::fromKey, ThemeMode.DARK)

    private fun <T> checkKeys(values: List<T>, key: (T) -> String, parse: (String) -> T, fallback: T) {
        values.forEach { assertEquals(key(it), it, parse(key(it))) }
        listOf("", "unknown", " ", "removed-setting").forEach { assertEquals(it, fallback, parse(it)) }
    }
}
