package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.LoopMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerQueueSkipPolicyTest {

    @Test
    fun nextIndex_returnsAdjacentWhenNotAtEnd() {
        assertEquals(1, PlayerQueueSkipPolicy.nextIndex(0, 3))
        assertEquals(2, PlayerQueueSkipPolicy.nextIndex(1, 3))
    }

    @Test
    fun nextIndex_nullAtEndOrSingleItemWhenNotLoopingList() {
        assertNull(PlayerQueueSkipPolicy.nextIndex(2, 3))
        assertNull(PlayerQueueSkipPolicy.nextIndex(0, 1))
        assertNull(PlayerQueueSkipPolicy.nextIndex(-1, 3))
    }

    @Test
    fun nextIndex_wrapsToStartInListLoopMode() {
        assertEquals(0, PlayerQueueSkipPolicy.nextIndex(2, 3, LoopMode.LIST))
    }

    @Test
    fun previousIndex_returnsAdjacentWhenNotAtStart() {
        assertEquals(0, PlayerQueueSkipPolicy.previousIndex(1, 3))
        assertEquals(1, PlayerQueueSkipPolicy.previousIndex(2, 3))
    }

    @Test
    fun previousIndex_nullAtStartOrSingleItemWhenNotLoopingList() {
        assertNull(PlayerQueueSkipPolicy.previousIndex(0, 3))
        assertNull(PlayerQueueSkipPolicy.previousIndex(0, 1))
    }

    @Test
    fun previousIndex_wrapsToEndInListLoopMode() {
        assertEquals(2, PlayerQueueSkipPolicy.previousIndex(0, 3, LoopMode.LIST))
    }
}
