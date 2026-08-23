package com.example.openvideo.ui.home

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.openvideo.R
import com.example.openvideo.core.ui.AppleOverlayChrome

object AppleFilterChrome {
    fun pill(
        context: Context,
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ): TextView = TextView(context).apply {
        this.text = text
        isClickable = true
        isFocusable = true
        isSingleLine = true
        gravity = Gravity.CENTER
        textSize = 13f
        setPadding(
            AppleOverlayChrome.dp(context, 12),
            AppleOverlayChrome.dp(context, 6),
            AppleOverlayChrome.dp(context, 12),
            AppleOverlayChrome.dp(context, 6)
        )
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            AppleOverlayChrome.dp(context, 32)
        )
        params.marginEnd = AppleOverlayChrome.dp(context, 8)
        layoutParams = params
        bindPill(this, selected)
        setOnClickListener { onClick() }
        onLongClick?.let { handler ->
            setOnLongClickListener {
                handler()
                true
            }
        }
    }

    fun bindPill(view: TextView, selected: Boolean) {
        view.isSelected = selected
        view.setBackgroundResource(R.drawable.bg_filter_pill)
        view.setTypeface(Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL)
        view.setTextColor(
            ContextCompat.getColor(
                view.context,
                if (selected) R.color.ov_text_primary else R.color.ov_text_secondary
            )
        )
    }

    fun optionRow(
        context: Context,
        text: String,
        selected: Boolean,
        onClick: () -> Unit
    ): TextView = TextView(context).apply {
        this.text = text
        isClickable = true
        isFocusable = true
        gravity = Gravity.CENTER_VERTICAL
        minHeight = AppleOverlayChrome.dp(context, 44)
        textSize = 17f
        setPadding(0, 0, 0, 0)
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            if (selected) R.drawable.ic_check else 0,
            0
        )
        compoundDrawablePadding = AppleOverlayChrome.dp(context, 8)
        setTextColor(ContextCompat.getColor(context, R.color.ov_overlay_title))
        compoundDrawableTintList = ContextCompat.getColorStateList(context, R.color.ov_accent_blue)
        setBackgroundResource(R.drawable.bg_ov_pressed)
        setOnClickListener { onClick() }
    }
}
