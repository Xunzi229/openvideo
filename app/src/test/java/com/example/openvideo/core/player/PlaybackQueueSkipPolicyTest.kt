package com.example.openvideo.core.player

import com.example.openvideo.core.prefs.LoopMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackQueueSkipPolicyTest {

    @Test
    fun nextIndex_returnsAdjacentWhenNotAtEnd() {
        assertEquals(1, PlaybackQueueSkipPolicy.nextIndex(0, 3, LoopMode.OFF))
        assertEquals(2, PlaybackQueueSkipPolicy.nextIndex(1, 3, LoopMode.OFF))
    }

    @Test
    fun nextIndex_nullAtEndWhenNotLoopingList() {
        assertNull(PlaybackQueueSkipPolicy.nextIndex(2, 3, LoopMode.OFF))
        assertNull(PlaybackQueueSkipPolicy.nextIndex(2, 3, LoopMode.SINGLE))
    }

    @Test
    fun nextIndex_wrapsToStartInListLoopMode() {
        assertEquals(0, PlaybackQueueSkipPolicy.nextIndex(2, 3, LoopMode.LIST))
    }

    @Test
    fun nextIndex_nullForSingleItemOrInvalidIndex() {
        assertNull(PlaybackQueueSkipPolicy.nextIndex(0, 1, LoopMode.LIST))
        assertNull(PlaybackQueueSkipPolicy.nextIndex(-1, 3, LoopMode.LIST))
    }

    @Test
    fun previousIndex_returnsAdjacentWhenNotAtStart() {
        assertEquals(0, PlaybackQueueSkipPolicy.previousIndex(1, 3, LoopMode.OFF))
        assertEquals(1, PlaybackQueueSkipPolicy.previousIndex(2, 3, LoopMode.OFF))
    }

    @Test
    fun previousIndex_nullAtStartWhenNotLoopingList() {
        assertNull(PlaybackQueueSkipPolicy.previousIndex(0, 3, LoopMode.OFF))
    }

    @Test
    fun previousIndex_wrapsToEndInListLoopMode() {
        assertEquals(2, PlaybackQueueSkipPolicy.previousIndex(0, 3, LoopMode.LIST))
    }
}
