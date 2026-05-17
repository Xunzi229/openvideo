package com.example.openvideo.ui.player

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerTouchActionPolicyTest {

    @Test
    fun mapsPrimaryTouchActions() {
        assertEquals(PlayerTouchAction.DOWN, PlayerTouchActionPolicy.fromMotionActionMasked(MotionEvent.ACTION_DOWN))
        assertEquals(PlayerTouchAction.MOVE, PlayerTouchActionPolicy.fromMotionActionMasked(MotionEvent.ACTION_MOVE))
        assertEquals(PlayerTouchAction.UP, PlayerTouchActionPolicy.fromMotionActionMasked(MotionEvent.ACTION_UP))
        assertEquals(PlayerTouchAction.CANCEL, PlayerTouchActionPolicy.fromMotionActionMasked(MotionEvent.ACTION_CANCEL))
    }

    @Test
    fun unknownActionMapsToOther() {
        assertEquals(PlayerTouchAction.OTHER, PlayerTouchActionPolicy.fromMotionActionMasked(MotionEvent.ACTION_OUTSIDE))
    }
}
