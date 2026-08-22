package com.example.openvideo.core.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class AppleAlertDialog private constructor(
    context: Context,
    private val title: CharSequence,
    private val message: CharSequence?,
    private val extraContent: View?,
    private val actions: List<AppleAction>,
    private val includeIme: Boolean,
    private val focusView: View?
) : Dialog(context) {

    private var closing = false
    private var content: View? = null
    private var defaultFocusView: View? = null
    private val colors: AppleOverlayColors get() = AppleOverlayColors.from(context)

    override fun onStart() {
        super.onStart()
        val content = buildContent()
        this.content = content
        setContentView(content)
        window?.let { AppleOverlayChrome.configureCenterWindow(it, colors) }
        AppleOverlayChrome.enterCenter(content)
        val focus = focusView ?: defaultFocusView
        focus?.post { focus.requestFocus() }
    }

    override fun dismiss() {
        if (closing) return
        dismissWithAnimation { super.dismiss() }
    }

    private fun buildContent(): View {
        val c = colors
        val host = FrameLayout(context)
        OverlayWindowInsets.bind(host, includeIme = includeIme)

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AppleOverlayChrome.cardBackground(context, c.alert)
            clipToOutline = true
        }

        val textPad = AppleOverlayChrome.dp(context, 20)
        val textArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(textPad, AppleOverlayChrome.dp(context, 19), textPad, AppleOverlayChrome.dp(context, 16))
        }
        textArea.addView(TextView(context).apply {
            text = title
            gravity = Gravity.CENTER
            setTextColor(c.title)
            textSize = AppleOverlayChrome.TITLE_SP
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        if (!message.isNullOrBlank()) {
            textArea.addView(TextView(context).apply {
                text = message
                gravity = Gravity.CENTER
                setTextColor(c.message)
                textSize = AppleOverlayChrome.MESSAGE_SP
                includeFontPadding = false
                setPadding(0, AppleOverlayChrome.dp(context, 4), 0, 0)
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        extraContent?.let { extra ->
            (extra.parent as? ViewGroup)?.removeView(extra)
            val extraPad = AppleOverlayChrome.dp(context, 8)
            extra.setPadding(extra.paddingLeft, extraPad, extra.paddingRight, extra.paddingBottom)
            textArea.addView(extra, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = AppleOverlayChrome.dp(context, 12) })
        }
        card.addView(textArea)
        card.addView(AppleOverlayChrome.hairline(context, c.hairline))
        card.addView(buildButtons(c))

        val width = AppleOverlayChrome.alertWidth(context)
        host.addView(card, FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })
        return host
    }

    private fun buildButtons(colors: AppleOverlayColors): View {
        if (actions.size == 2) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            actions.forEachIndexed { index, action ->
                if (index > 0) row.addView(AppleOverlayChrome.verticalHairline(context, colors.hairline))
                val button = button(action, colors)
                if (action.style == AppleActionStyle.CANCEL || defaultFocusView == null) {
                    defaultFocusView = button
                }
                row.addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            return row
        }
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        actions.forEachIndexed { index, action ->
            if (index > 0) column.addView(AppleOverlayChrome.hairline(context, colors.hairline))
            val button = button(action, colors)
            if (action.style == AppleActionStyle.CANCEL || defaultFocusView == null) {
                defaultFocusView = button
            }
            column.addView(button)
        }
        return column
    }

    private fun button(action: AppleAction, colors: AppleOverlayColors): TextView =
        AppleOverlayChrome.actionRow(
            context = context,
            action = action,
            colors = colors,
            minHeightDp = 44,
            textSizeSp = AppleOverlayChrome.ALERT_BUTTON_SP
        ) {
            if (!action.dismissOnClick) {
                action.onClick()
                return@actionRow
            }
            dismissWithAnimation {
                super.dismiss()
                action.onClick()
            }
        }

    private fun dismissWithAnimation(onComplete: () -> Unit) {
        val content = content ?: return onComplete()
        closing = true
        AppleOverlayChrome.exitCenter(content) {
            onComplete()
            closing = false
        }
    }

    companion object {
        fun show(
            context: Context,
            title: CharSequence,
            message: CharSequence? = null,
            extraContent: View? = null,
            actions: List<AppleAction>,
            includeIme: Boolean = false,
            focusView: View? = null,
            onDismiss: () -> Unit = {}
        ): Dialog = AppleAlertDialog(
            context = context,
            title = title,
            message = message,
            extraContent = extraContent,
            actions = actions,
            includeIme = includeIme,
            focusView = focusView
        ).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(true)
            setOnDismissListener { onDismiss() }
            show()
        }
    }
}
