package com.example.openvideo.ui.player

object PlayerQuickEntryDialogPolicy {
    const val SHEET_PADDING_DP = 8

    fun sheetPaddingPx(density: Float): Int =
        (SHEET_PADDING_DP * density).toInt()
}
