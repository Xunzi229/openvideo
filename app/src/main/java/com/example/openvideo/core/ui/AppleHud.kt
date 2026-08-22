package com.example.openvideo.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.example.openvideo.R

object AppleHud {
    const val SHORT_MS = 1800L
    const val LONG_MS = 3500L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var current: PopupWindow? = null
    private var dismissToken: Any? = null

    fun show(context: Context, @StringRes textRes: Int, long: Boolean = false) {
        show(context, context.getText(textRes), long = long)
    }

    fun show(
        context: Context,
        text: CharSequence,
        long: Boolean = false,
        actionTitle: CharSequence? = null,
        onAction: (() -> Unit)? = null
    ) {
        val duration = if (long || onAction != null) LONG_MS else SHORT_MS
        val activity = context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Toast.makeText(
                context.applicationContext,
                text,
                if (duration > SHORT_MS) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
            return
        }

        dismiss()
        val content = buildContent(activity, text, actionTitle) {
            onAction?.invoke()
            dismiss()
        }
        val popup = PopupWindow(
            content,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = true
            isFocusable = false
            elevation = AppleOverlayChrome.dp(activity, 8).toFloat()
        }
        current = popup
        val root = activity.window.decorView
        root.post {
            if (activity.isFinishing || activity.isDestroyed || current !== popup) return@post
            popup.showAtLocation(root, Gravity.CENTER, 0, 0)
        }
        val token = Any()
        dismissToken = token
        mainHandler.postDelayed({
            if (dismissToken === token) dismiss()
        }, duration)
    }

    fun dismiss() {
        dismissToken = null
        current?.dismiss()
        current = null
    }

    private fun buildContent(
        context: Context,
        text: CharSequence,
        actionTitle: CharSequence?,
        onActionClick: () -> Unit
    ): View {
        val padH = AppleOverlayChrome.dp(context, 18)
        val padV = AppleOverlayChrome.dp(context, 12)
        val bg = GradientDrawable().apply {
            cornerRadius = AppleOverlayChrome.dp(context, 14).toFloat()
            setColor(ContextCompat.getColor(context, R.color.ov_hud_bg))
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bg
            setPadding(padH, padV, padH, padV)
            elevation = AppleOverlayChrome.dp(context, 8).toFloat()
        }
        val message = TextView(context).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, R.color.ov_hud_text))
            textSize = 15f
            gravity = Gravity.CENTER
            maxWidth = AppleOverlayChrome.dp(context, 240)
        }
        row.addView(message)
        if (!actionTitle.isNullOrBlank()) {
            val action = TextView(context).apply {
                this.text = actionTitle
                setTextColor(ContextCompat.getColor(context, R.color.ov_accent_blue))
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(AppleOverlayChrome.dp(context, 14), 0, 0, 0)
                isClickable = true
                isFocusable = true
                setOnClickListener { onActionClick() }
            }
            row.addView(action)
        }
        return row
    }

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
