package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.LoopMode
import com.example.openvideo.core.prefs.PlaybackEndBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerPlaybackEndPolicyTest {

    @Test
    fun followSettingsStopsAtEndForSingleVideoWithoutLoop() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 1,
            autoPlayNext = true,
            loopMode = LoopMode.OFF,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            endBehavior = PlaybackEndBehavior.FOLLOW_SETTINGS
        )

        assertEquals(PlayerPlaybackEndAction.STOP_AT_END, decision.action)
        assertNull(decision.nextIndex)
        assertNull(decision.seekPositionMs)
    }

    @Test
    fun followSettingsReplaysCurrentVideoWhenSingleLoopEnabled() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 1,
            autoPlayNext = true,
            loopMode = LoopMode.SINGLE,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            endBehavior = PlaybackEndBehavior.FOLLOW_SETTINGS
        )

        assertEquals(PlayerPlaybackEndAction.REPLAY_CURRENT, decision.action)
        assertEquals(0L, decision.seekPositionMs)
        assertNull(decision.nextIndex)
    }

    @Test
    fun followSettingsAdvancesWhenListLoopAndAutoNextAreEnabled() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 3,
            autoPlayNext = true,
            loopMode = LoopMode.LIST,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            endBehavior = PlaybackEndBehavior.FOLLOW_SETTINGS
        )

        assertEquals(PlayerPlaybackEndAction.PLAY_NEXT, decision.action)
        assertEquals(1, decision.nextIndex)
        assertNull(decision.seekPositionMs)
    }

    @Test
    fun activeAbLoopReplaysFromPointABeforeExplicitEndBehavior() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 3,
            autoPlayNext = true,
            loopMode = LoopMode.LIST,
            abLoopState = PlayerAbLoopState.LOOPING,
            abLoopPointA = 42_000L,
            endBehavior = PlaybackEndBehavior.RETURN_TO_LIST
        )

        assertEquals(PlayerPlaybackEndAction.REPLAY_CURRENT, decision.action)
        assertEquals(42_000L, decision.seekPositionMs)
        assertNull(decision.nextIndex)
    }

    @Test
    fun explicitReplayOverridesListLoopAdvance() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 3,
            autoPlayNext = true,
            loopMode = LoopMode.LIST,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            endBehavior = PlaybackEndBehavior.REPLAY
        )

        assertEquals(PlayerPlaybackEndAction.REPLAY_CURRENT, decision.action)
        assertEquals(0L, decision.seekPositionMs)
    }

    @Test
    fun explicitStopOverridesListLoopAdvance() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 3,
            autoPlayNext = true,
            loopMode = LoopMode.LIST,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            endBehavior = PlaybackEndBehavior.STOP
        )

        assertEquals(PlayerPlaybackEndAction.STOP_AT_END, decision.action)
    }

    @Test
    fun explicitPlayNextAdvancesQueueAndWrapsAtEnd() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 2,
            queueSize = 3,
            autoPlayNext = false,
            loopMode = LoopMode.OFF,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            endBehavior = PlaybackEndBehavior.PLAY_NEXT
        )

        assertEquals(PlayerPlaybackEndAction.PLAY_NEXT, decision.action)
        assertEquals(0, decision.nextIndex)
    }

    @Test
    fun explicitReturnToListFinishesPlayback() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 1,
            autoPlayNext = false,
            loopMode = LoopMode.OFF,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            endBehavior = PlaybackEndBehavior.RETURN_TO_LIST
        )

        assertEquals(PlayerPlaybackEndAction.RETURN_TO_LIST, decision.action)
    }
}
