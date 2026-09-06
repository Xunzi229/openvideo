package com.openvideo.app.core.ui

import android.view.View
import com.openvideo.app.R

object AppleEmptyState {
    fun setVisible(emptyLabel: View, visible: Boolean) {
        val container = (emptyLabel.parent as? View)?.takeIf { it.id == R.id.empty_state } ?: emptyLabel
        container.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
