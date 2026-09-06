package com.openvideo.app.ui.player

import com.openvideo.app.R

/**
 * Maps subtitle load outcomes to user-visible toast resources.
 * Activity only shows the toast; loading and apply decisions live in ViewModel.
 */
object PlayerSubtitleLoadToastPolicy {

    fun messageRes(toastKind: PlayerSubtitleLoadToastKind): Int? =
        when (toastKind) {
            PlayerSubtitleLoadToastKind.LOADED -> R.string.player_subtitle_loaded
            PlayerSubtitleLoadToastKind.FAILED -> R.string.player_subtitle_load_failed
            PlayerSubtitleLoadToastKind.NONE -> null
        }
}
