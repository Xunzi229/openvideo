package com.example.openvideo.core.ui

import android.view.View
import com.example.openvideo.R

object AppleEmptyState {
    fun setVisible(emptyLabel: View, visible: Boolean) {
        val container = (emptyLabel.parent as? View)?.takeIf { it.id == R.id.empty_state } ?: emptyLabel
        container.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
