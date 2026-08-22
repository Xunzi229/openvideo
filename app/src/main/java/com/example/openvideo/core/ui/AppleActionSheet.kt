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
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import com.example.openvideo.R

class AppleActionSheet private constructor(
    context: Context,
    private val title: CharSequence?,
    private val message: CharSequence?,
    private val actions: List<AppleAction>,
    private val cancelTitle: CharSequence?,
    private val defaultFocusCancel: Boolean
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
        window?.let { AppleOverlayChrome.configureBottomWindow(it, context, colors, blur = true) }
        AppleOverlayChrome.enterFromBottom(content)
        constrainToSafeHeight(content)
        defaultFocusView?.post { defaultFocusView?.requestFocus() }
    }

    override fun dismiss() {
        if (closing) return
        dismissWithAnimation { super.dismiss() }
    }

    private fun buildContent(): View {
        val c = colors
        val sheetWidth = AppleOverlayChrome.sheetWidth(context)
        val host = FrameLayout(context)
        OverlayWindowInsets.bind(host, extraBottomPx = AppleOverlayChrome.dp(context, 8))
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipToPadding = false
        }

        val actionCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AppleOverlayChrome.cardBackground(context, c.card)
            clipToOutline = true
        }
        var added = false
        if (!title.isNullOrBlank() || !message.isNullOrBlank()) {
            actionCard.addView(header(c))
            added = true
        }
        actions.forEach { action ->
            if (added) actionCard.addView(AppleOverlayChrome.hairline(context, c.hairline))
            val row = AppleOverlayChrome.actionRow(context, action, c) {
                runAction(action)
            }
            if (!defaultFocusCancel && defaultFocusView == null) {
                defaultFocusView = row
            }
            actionCard.addView(row)
            added = true
        }

        if (added) {
            val scroll = NestedScrollView(context)
            scroll.addView(actionCard, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            root.addView(scroll, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        if (!cancelTitle.isNullOrBlank()) {
            val cancelCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = AppleOverlayChrome.cardBackground(context, c.card)
                clipToOutline = true
            }
            val cancelAction = AppleAction(cancelTitle, AppleActionStyle.CANCEL, bold = true)
            val cancelRow = AppleOverlayChrome.actionRow(context, cancelAction, c) {
                dismissWithAnimation { super.dismiss() }
            }
            if (defaultFocusCancel) defaultFocusView = cancelRow
            cancelCard.addView(cancelRow)
            root.addView(cancelCard, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = AppleOverlayChrome.dp(context, 10) })
        }

        host.addView(root, FrameLayout.LayoutParams(sheetWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        })
        return host
    }

    private fun header(colors: AppleOverlayColors): LinearLayout {
        val block = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val pad = AppleOverlayChrome.dp(context, 18)
            setPadding(pad, AppleOverlayChrome.dp(context, 17), pad, AppleOverlayChrome.dp(context, 15))
        }
        if (!title.isNullOrBlank()) {
            block.addView(TextView(context).apply {
                text = title
                gravity = Gravity.CENTER
                setTextColor(colors.title)
                textSize = AppleOverlayChrome.TITLE_SP
                typeface = Typeface.DEFAULT_BOLD
                includeFontPadding = false
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        if (!message.isNullOrBlank()) {
            block.addView(TextView(context).apply {
                text = message
                gravity = Gravity.CENTER
                setTextColor(colors.message)
                textSize = if (title.isNullOrBlank()) 14f else AppleOverlayChrome.MESSAGE_SP
                includeFontPadding = false
                if (!title.isNullOrBlank()) {
                    setPadding(0, AppleOverlayChrome.dp(context, 7), 0, 0)
                }
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return block
    }

    private fun runAction(action: AppleAction) {
        if (!action.dismissOnClick) {
            action.onClick()
            return
        }
        dismissWithAnimation {
            super.dismiss()
            action.onClick()
        }
    }

    private fun constrainToSafeHeight(host: View) {
        host.doOnLayout {
            val scroll = findScroll(host) ?: return@doOnLayout
            val sheet = (host as? ViewGroup)?.getChildAt(0) ?: return@doOnLayout
            val insets = ViewCompat.getRootWindowInsets(host)?.let { OverlayWindowInsets.edgesFrom(it) }
                ?: SystemBarInsetsPolicy.Edges(0, 0, 0, 0)
            val screenHeight = context.resources.displayMetrics.heightPixels
            val maxHeight = SystemBarInsetsPolicy.overlayMaxHeight(
                containerHeight = screenHeight,
                topOffset = (screenHeight * 0.22f).toInt(),
                bottomInset = insets.bottom,
                extraBottom = AppleOverlayChrome.dp(context, 8)
            )
            if (sheet.height <= maxHeight) return@doOnLayout
            val chrome = sheet.height - scroll.height
            val scrollMax = (maxHeight - chrome).coerceAtLeast(AppleOverlayChrome.dp(context, 120))
            scroll.updateLayoutParams {
                height = scrollMax
            }
        }
    }

    private fun findScroll(view: View): NestedScrollView? {
        if (view is NestedScrollView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findScroll(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }

    private fun dismissWithAnimation(onComplete: () -> Unit) {
        val content = content ?: return onComplete()
        closing = true
        AppleOverlayChrome.exitToBottom(content) {
            onComplete()
            closing = false
        }
    }

    companion object {
        fun show(
            context: Context,
            title: CharSequence? = null,
            message: CharSequence? = null,
            actions: List<AppleAction>,
            cancelTitle: CharSequence? = context.getString(R.string.action_cancel),
            defaultFocusCancel: Boolean = true,
            onDismiss: () -> Unit = {}
        ): Dialog = AppleActionSheet(
            context = context,
            title = title,
            message = message,
            actions = actions,
            cancelTitle = cancelTitle,
            defaultFocusCancel = defaultFocusCancel
        ).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(true)
            setOnDismissListener { onDismiss() }
            show()
        }
    }
}
