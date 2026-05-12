package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerExitPolicyTest {

    @Test
    fun firstFinishRequestStartsFinishAndMarksState() {
        val decision = PlayerExitPolicy.requestFinish(PlayerExitState())

        assertTrue(decision.shouldFinish)
        assertEquals(PlayerExitState(isFinishing = true), decision.nextState)
    }

    @Test
    fun repeatedFinishRequestIsIgnored() {
        val decision = PlayerExitPolicy.requestFinish(PlayerExitState(isFinishing = true))

        assertFalse(decision.shouldFinish)
        assertEquals(PlayerExitState(isFinishing = true), decision.nextState)
    }

    @Test
    fun firstReleaseRequestRunsReleaseAndMarksState() {
        val decision = PlayerExitPolicy.requestRelease(PlayerExitState())

        assertTrue(decision.shouldRelease)
        assertEquals(PlayerExitState(hasReleased = true), decision.nextState)
    }

    @Test
    fun repeatedReleaseRequestIsIgnored() {
        val decision = PlayerExitPolicy.requestRelease(PlayerExitState(hasReleased = true))

        assertFalse(decision.shouldRelease)
        assertEquals(PlayerExitState(hasReleased = true), decision.nextState)
    }

    @Test
    fun releaseRequestPreservesFinishingFlag() {
        val decision = PlayerExitPolicy.requestRelease(PlayerExitState(isFinishing = true))

        assertTrue(decision.nextState.isFinishing)
        assertTrue(decision.nextState.hasReleased)
    }
}
