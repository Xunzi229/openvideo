package com.openvideo.app.ui.local

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.openvideo.app.R
import com.openvideo.app.core.prefs.LocalContinuePlaybackPosition

object LocalContinuePlaybackLayout {
    fun apply(layout: ConstraintLayout, position: LocalContinuePlaybackPosition) {
        val gap = layout.resources.getDimensionPixelSize(R.dimen.ov_space_sm)
        ConstraintSet().apply {
            clone(layout)
            clear(R.id.row_continue_playback, ConstraintSet.TOP)
            clear(R.id.row_continue_playback, ConstraintSet.BOTTOM)
            clear(R.id.recycler_folders, ConstraintSet.TOP)
            clear(R.id.recycler_folders, ConstraintSet.BOTTOM)
            if (position == LocalContinuePlaybackPosition.BOTTOM) {
                connect(R.id.row_continue_playback, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, gap)
                connect(R.id.recycler_folders, ConstraintSet.TOP, R.id.header_row, ConstraintSet.BOTTOM)
                // Reserve the card's actual height, including larger fonts; scrolling never goes behind it.
                connect(R.id.recycler_folders, ConstraintSet.BOTTOM, R.id.row_continue_playback, ConstraintSet.TOP, gap)
                setGoneMargin(R.id.recycler_folders, ConstraintSet.BOTTOM, 0)
            } else {
                connect(R.id.row_continue_playback, ConstraintSet.TOP, R.id.header_row, ConstraintSet.BOTTOM, gap)
                connect(R.id.recycler_folders, ConstraintSet.TOP, R.id.row_continue_playback, ConstraintSet.BOTTOM)
                connect(R.id.recycler_folders, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            }
            applyTo(layout)
        }
    }
}
