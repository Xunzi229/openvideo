package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerAbLoopPolicyTest {

    @Test
    fun firstClickStoresPointA() {
        val result = PlayerAbLoopPolicy.onToggle(
            state = PlayerAbLoopState.IDLE,
            pointA = -1,
            pointB = -1,
            currentPositionMs = 12_000
        )

        assertEquals(PlayerAbLoopState.POINT_A_SET, result.state)
        assertEquals(12_000L, result.pointA)
        assertEquals(-1L, result.pointB)
        assertEquals(PlayerAbLoopEvent.POINT_A_SET, result.event)
    }

    @Test
    fun secondClickAfterPointAStartsLoopWhenPointBIsAfterA() {
        val result = PlayerAbLoopPolicy.onToggle(
            state = PlayerAbLoopState.POINT_A_SET,
            pointA = 12_000,
            pointB = -1,
            currentPositionMs = 20_000
        )

        assertEquals(PlayerAbLoopState.LOOPING, result.state)
        assertEquals(12_000L, result.pointA)
        assertEquals(20_000L, result.pointB)
        assertEquals(PlayerAbLoopEvent.LOOP_STARTED, result.event)
    }

    @Test
    fun secondClickBeforePointAResetsLoopAsInvalid() {
        val result = PlayerAbLoopPolicy.onToggle(
            state = PlayerAbLoopState.POINT_A_SET,
            pointA = 12_000,
            pointB = -1,
            currentPositionMs = 10_000
        )

        assertEquals(PlayerAbLoopState.IDLE, result.state)
        assertEquals(-1L, result.pointA)
        assertEquals(-1L, result.pointB)
        assertEquals(PlayerAbLoopEvent.INVALID_POINT_B, result.event)
    }

    @Test
    fun clickWhileLoopingCancelsLoop() {
        val result = PlayerAbLoopPolicy.onToggle(
            state = PlayerAbLoopState.LOOPING,
            pointA = 12_000,
            pointB = 20_000,
            currentPositionMs = 15_000
        )

        assertEquals(PlayerAbLoopState.IDLE, result.state)
        assertEquals(-1L, result.pointA)
        assertEquals(-1L, result.pointB)
        assertEquals(PlayerAbLoopEvent.CANCELLED, result.event)
    }

    @Test
    fun loopingTickSeeksBackToPointAOnlyForValidActiveLoop() {
        assertEquals(
            12_000L,
            PlayerAbLoopPolicy.loopSeekTarget(
                state = PlayerAbLoopState.LOOPING,
                pointA = 12_000,
                pointB = 20_000,
                currentPositionMs = 20_000
            )
        )
        assertNull(
            PlayerAbLoopPolicy.loopSeekTarget(
                state = PlayerAbLoopState.LOOPING,
                pointA = 12_000,
                pointB = 20_000,
                currentPositionMs = 19_999
            )
        )
        assertNull(
            PlayerAbLoopPolicy.loopSeekTarget(
                state = PlayerAbLoopState.POINT_A_SET,
                pointA = 12_000,
                pointB = 20_000,
                currentPositionMs = 20_000
            )
        )
    }
}
