package com.example.openvideo.ui.player

import android.widget.TextView
import com.example.openvideo.R
import com.example.openvideo.core.prefs.ContentFrameMode
import com.example.openvideo.core.prefs.PlayerPrefs

object PlayerDisplayContentFrameControls {

    val modes: Array<ContentFrameMode> = ContentFrameMode.entries.toTypedArray()

    fun labelRes(mode: ContentFrameMode): Int = when (mode) {
        ContentFrameMode.OFF -> R.string.settings_content_frame_off
        ContentFrameMode.CENTER_16_9 -> R.string.settings_content_frame_center_16_9
        ContentFrameMode.CENTER_4_3 -> R.string.settings_content_frame_center_4_3
    }

    fun bind(
        tvValue: TextView,
        playerPrefs: PlayerPrefs,
        getIndex: () -> Int,
        setIndex: (Int) -> Unit,
        onApplied: (() -> Unit)? = null
    ) {
        fun updateText() {
            tvValue.setText(labelRes(modes[getIndex()]))
        }
        updateText()
        tvValue.setOnClickListener {
            val nextIndex = (getIndex() + 1) % modes.size
            val selection = PlayerContentFrameSettingsPolicy.onModeSelected(
                mode = modes[nextIndex],
                currentAspectRatio = playerPrefs.aspectRatio
            )
            setIndex(modes.indexOf(selection.mode).coerceAtLeast(0))
            playerPrefs.contentFrameMode = selection.mode
            selection.aspectRatioOverride?.let { playerPrefs.aspectRatio = it }
            updateText()
            onApplied?.invoke()
        }
    }
}
