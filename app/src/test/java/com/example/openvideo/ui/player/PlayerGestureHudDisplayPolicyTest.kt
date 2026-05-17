package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerGestureHudDisplayPolicyTest {

    @Test
    fun indicatorTextUsesPrimaryOnlyWhenSecondaryBlank() {
        val hud = PlayerGestureHud(
            kind = PlayerGestureHudKind.SEEK,
            primaryText = "+0:10",
            secondaryText = ""
        )
        assertEquals("+0:10", PlayerGestureHudDisplayPolicy.indicatorText(hud))
    }

    @Test
    fun indicatorTextJoinsPrimaryAndSecondary() {
        val hud = PlayerGestureHud(
            kind = PlayerGestureHudKind.SEEK,
            primaryText = "+0:10",
            secondaryText = "1:00 / 10:00"
        )
        assertEquals("+0:10\n1:00 / 10:00", PlayerGestureHudDisplayPolicy.indicatorText(hud))
    }
}
