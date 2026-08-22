package com.example.openvideo.core.ui

import android.view.View
import com.example.openvideo.R

object GroupedListChrome {
    fun bind(view: View, hairline: View?, position: Int, itemCount: Int) {
        bindHairline(hairline, position, itemCount)
        view.setBackgroundResource(
            when {
                itemCount <= 1 -> R.drawable.bg_grouped_row_single
                position == 0 -> R.drawable.bg_grouped_row_top
                position == itemCount - 1 -> R.drawable.bg_grouped_row_bottom
                else -> R.drawable.bg_grouped_row_middle
            }
        )
    }

    fun bindContained(hairline: View?, position: Int, itemCount: Int) {
        bindHairline(hairline, position, itemCount)
    }

    fun bindStandalone(view: View) {
        view.setBackgroundResource(R.drawable.bg_grouped_row_single)
    }

    private fun bindHairline(hairline: View?, position: Int, itemCount: Int) {
        hairline?.visibility = if (itemCount <= 1 || position == itemCount - 1) View.GONE else View.VISIBLE
    }
}
