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

    @Test
    fun finishPresentationKeepsExitTimingPolicyDriven() {
        val presentation = PlayerExitPolicy.finishPresentation()

        assertEquals(250L, presentation.releaseDelayMs)
        assertEquals(180L, presentation.finishDelayMs)
    }

    @Test
    fun transitionStrategyUsesModernApiOnlyOnAndroid14AndAbove() {
        assertEquals(
            PlayerExitTransitionStrategy.OVERRIDE_PENDING_TRANSITION,
            PlayerExitPolicy.transitionStrategyFor(android.os.Build.VERSION_CODES.TIRAMISU)
        )
        assertEquals(
            PlayerExitTransitionStrategy.OVERRIDE_ACTIVITY_TRANSITION,
            PlayerExitPolicy.transitionStrategyFor(android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        )
    }

    @Test
    fun exitFrameDecision_skipsWhenPlayerViewNotInitialized() {
        val decision = PlayerExitPolicy.exitFrameDecision(playerViewInitialized = false)

        assertFalse(decision.shouldPrepare)
        assertFalse(decision.hidePlayerView)
        assertFalse(decision.cancelPlayerViewAnimation)
    }

    @Test
    fun exitFrameDecision_hidesPlayerViewWithAppBaseBackdrop() {
        val decision = PlayerExitPolicy.exitFrameDecision(playerViewInitialized = true)

        assertTrue(decision.shouldPrepare)
        assertTrue(decision.hidePlayerView)
        assertTrue(decision.cancelPlayerViewAnimation)
        assertEquals(PlayerExitBackdrop.APP_BASE, decision.backdrop)
    }
}
