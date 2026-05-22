package com.example.openvideo.ui.player

import com.example.openvideo.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerSubtitleLoadToastPolicyTest {

    @Test
    fun loadedToastMapsToLoadedString() {
        assertEquals(
            R.string.player_subtitle_loaded,
            PlayerSubtitleLoadToastPolicy.messageRes(PlayerSubtitleLoadToastKind.LOADED)
        )
    }

    @Test
    fun failedToastMapsToFailedString() {
        assertEquals(
            R.string.player_subtitle_load_failed,
            PlayerSubtitleLoadToastPolicy.messageRes(PlayerSubtitleLoadToastKind.FAILED)
        )
    }

    @Test
    fun noneToastHasNoMessage() {
        assertNull(PlayerSubtitleLoadToastPolicy.messageRes(PlayerSubtitleLoadToastKind.NONE))
    }
}
