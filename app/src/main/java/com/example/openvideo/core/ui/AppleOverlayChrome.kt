package com.example.openvideo.core.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.openvideo.R

object AppleOverlayChrome {
    const val SHEET_ENTER_MS = 220L
    const val SHEET_EXIT_MS = 160L
    const val ALERT_ENTER_MS = 180L
    const val ALERT_EXIT_MS = 140L
    const val ROW_TEXT_SP = 19f
    const val ALERT_BUTTON_SP = 17f
    const val TITLE_SP = 17f
    const val MESSAGE_SP = 13f

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    fun sheetWidth(context: Context): Int {
        val dm = context.resources.displayMetrics
        val max = context.resources.getDimensionPixelSize(R.dimen.ov_overlay_sheet_max_width)
        val gutter = context.resources.getDimensionPixelSize(R.dimen.ov_overlay_sheet_gutter) * 2
        return minOf(dm.widthPixels - gutter, max).coerceAtLeast(dm.widthPixels / 3)
    }

    fun alertWidth(context: Context): Int {
        val target = context.resources.getDimensionPixelSize(R.dimen.ov_alert_width)
        val max = context.resources.displayMetrics.widthPixels -
            context.resources.getDimensionPixelSize(R.dimen.ov_overlay_sheet_gutter) * 2
        return minOf(target, max)
    }

    fun cardBackground(context: Context, fill: Int, radiusDp: Int = 14): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, radiusDp).toFloat()
            setColor(fill)
        }

    fun hairline(context: Context, color: Int): View =
        View(context).apply {
            setBackgroundColor(color)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        }

    fun verticalHairline(context: Context, color: Int): View =
        View(context).apply {
            setBackgroundColor(color)
            layoutParams = LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT)
        }

    fun inputField(
        context: Context,
        colors: AppleOverlayColors,
        hint: CharSequence? = null,
        inputType: Int
    ): EditText = EditText(context).apply {
        this.hint = hint
        this.inputType = inputType
        setSingleLine(true)
        setTextColor(colors.title)
        setHintTextColor(colors.message)
        textSize = 17f
        background = cardBackground(context, colors.input, radiusDp = 12)
        setPadding(dp(context, 14), 0, dp(context, 14), 0)
        minHeight = dp(context, 44)
    }

    fun formNav(
        context: Context,
        colors: AppleOverlayColors,
        title: CharSequence,
        cancelTitle: CharSequence,
        confirmTitle: CharSequence,
        onCancel: () -> Unit,
        onConfirm: () -> Unit
    ): LinearLayout {
        val nav = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(context, 44)
        }
        nav.addView(TextView(context).apply {
            text = cancelTitle
            setTextColor(colors.accent)
            textSize = ALERT_BUTTON_SP
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
            isClickable = true
            isFocusable = true
            foreground = selectableForeground(context)
            setPadding(dp(context, 12), 0, dp(context, 12), 0)
            setOnClickListener { onCancel() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        nav.addView(TextView(context).apply {
            text = title
            setTextColor(colors.title)
            textSize = TITLE_SP
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(TextView(context).apply {
            text = confirmTitle
            setTextColor(colors.accent)
            textSize = ALERT_BUTTON_SP
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            includeFontPadding = false
            isClickable = true
            isFocusable = true
            foreground = selectableForeground(context)
            setPadding(dp(context, 12), 0, dp(context, 12), 0)
            maxLines = 1
            setOnClickListener { onConfirm() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return nav
    }

    fun selectableForeground(context: Context): Drawable? {
        val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        return typedArray.getDrawable(0).also { typedArray.recycle() }
    }

    fun actionRow(
        context: Context,
        action: AppleAction,
        colors: AppleOverlayColors,
        minHeightDp: Int = 56,
        textSizeSp: Float = ROW_TEXT_SP,
        onClick: () -> Unit
    ): TextView = TextView(context).apply {
        text = action.title
        gravity = Gravity.CENTER
        setTextColor(colors.colorFor(action.style))
        textSize = textSizeSp
        typeface = if (action.bold || action.style == AppleActionStyle.CANCEL) {
            Typeface.DEFAULT_BOLD
        } else {
            Typeface.DEFAULT
        }
        includeFontPadding = false
        minHeight = dp(context, minHeightDp)
        isClickable = true
        isFocusable = true
        foreground = selectableForeground(context)
        setOnClickListener {
            if (action.style == AppleActionStyle.DESTRUCTIVE) {
                AppleHaptics.light(this)
            }
            onClick()
        }
    }

    fun configureBottomWindow(
        window: Window,
        context: Context,
        colors: AppleOverlayColors,
        blur: Boolean
    ) {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setGravity(Gravity.BOTTOM)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.apply { dimAmount = colors.dimAmount }
        if (blur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(dp(context, 20))
        }
    }

    fun configureCenterWindow(window: Window, colors: AppleOverlayColors) {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setGravity(Gravity.CENTER)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.apply { dimAmount = colors.dimAmount }
    }

    fun enterFromBottom(content: View) {
        content.alpha = 0f
        content.translationY = dp(content.context, 18).toFloat()
        content.animate().alpha(1f).translationY(0f).setDuration(SHEET_ENTER_MS).start()
    }

    fun exitToBottom(content: View, onComplete: () -> Unit) {
        content.animate()
            .alpha(0f)
            .translationY(dp(content.context, 18).toFloat())
            .setDuration(SHEET_EXIT_MS)
            .withEndAction(onComplete)
            .start()
    }

    fun enterCenter(content: View) {
        content.alpha = 0f
        content.scaleX = 0.94f
        content.scaleY = 0.94f
        content.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ALERT_ENTER_MS)
            .start()
    }

    fun exitCenter(content: View, onComplete: () -> Unit) {
        content.animate()
            .alpha(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(ALERT_EXIT_MS)
            .withEndAction(onComplete)
            .start()
    }
}
