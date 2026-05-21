package com.example.openvideo.ui.playlist

import android.widget.TextView

/** 文本过长时启用系统 marquee 横向滚动；未溢出则不滚动。 */
object PlaylistMarqueeTextPolicy {

    fun apply(textView: TextView, text: CharSequence) {
        textView.text = text
        textView.isSelected = false
        textView.post {
            val available = textView.width - textView.paddingLeft - textView.paddingRight
            if (available <= 0) return@post
            val overflows = textView.paint.measureText(text.toString()) > available
            textView.isSelected = overflows
        }
    }
}
