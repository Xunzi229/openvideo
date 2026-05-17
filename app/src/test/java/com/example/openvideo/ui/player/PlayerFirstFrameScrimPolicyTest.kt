package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerFirstFrameScrimPolicyTest {

    @Test
    fun showPresentationIsVisible() {
        val presentation = PlayerFirstFrameScrimPolicy.presentation(
            PlayerFirstFramePolicy.onShowForNewMedia()
        )
        assertNotNull(presentation)
        assertTrue(presentation!!.visible)
        assertEquals(PlayerFirstFrameScrimPolicy.SCRIM_ALPHA, presentation.alpha, 0.001f)
    }

    @Test
    fun hidePresentationIsGone() {
        val presentation = PlayerFirstFrameScrimPolicy.presentation(
            PlayerFirstFramePolicy.onRenderedFirstFrame(isAwaitingFirstFrame = true)
        )
        assertNotNull(presentation)
        assertFalse(presentation!!.visible)
    }

    @Test
    fun noOpDecisionHasNoPresentation() {
        assertNull(
            PlayerFirstFrameScrimPolicy.presentation(
                PlayerFirstFrameDecision(nextAwaitingFirstFrame = false)
            )
        )
    }
}
