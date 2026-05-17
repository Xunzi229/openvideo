package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSubtitleLoadApplyPolicyTest {

    @Test
    fun loadedItems_applyWithoutToastWhenNotRequested() {
        val decision = PlayerSubtitleLoadApplyPolicy.afterLoad(loadedCount = 2, requestedToast = false)

        assertTrue(decision.shouldApplyToPlayer)
        assertEquals(PlayerSubtitleLoadToastKind.NONE, decision.toastKind)
    }

    @Test
    fun loadedItems_applyWithLoadedToastWhenRequested() {
        val decision = PlayerSubtitleLoadApplyPolicy.afterLoad(loadedCount = 1, requestedToast = true)

        assertTrue(decision.shouldApplyToPlayer)
        assertEquals(PlayerSubtitleLoadToastKind.LOADED, decision.toastKind)
    }

    @Test
    fun emptyLoad_showsFailedToastOnlyWhenRequested() {
        val silent = PlayerSubtitleLoadApplyPolicy.afterLoad(loadedCount = 0, requestedToast = false)
        val failed = PlayerSubtitleLoadApplyPolicy.afterLoad(loadedCount = 0, requestedToast = true)

        assertFalse(silent.shouldApplyToPlayer)
        assertEquals(PlayerSubtitleLoadToastKind.NONE, silent.toastKind)
        assertFalse(failed.shouldApplyToPlayer)
        assertEquals(PlayerSubtitleLoadToastKind.FAILED, failed.toastKind)
    }
}
