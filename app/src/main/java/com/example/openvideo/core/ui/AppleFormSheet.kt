package com.example.openvideo.core.ui

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView

class AppleFormSheet private constructor(
    context: Context,
    private val contentView: View,
    private val includeIme: Boolean,
    private val title: CharSequence?,
    private val cancelTitle: CharSequence?,
    private val confirmTitle: CharSequence?,
    private val onConfirm: (() -> Boolean)?,
    private val defaultFocus: View?
) : Dialog(context) {

    private var closing = false
    private var content: View? = null
    private val colors: AppleOverlayColors get() = AppleOverlayColors.from(context)

    override fun onStart() {
        super.onStart()
        val content = buildContent()
        this.content = content
        setContentView(content)
        window?.let { AppleOverlayChrome.configureBottomWindow(it, context, colors, blur = true) }
        AppleOverlayChrome.enterFromBottom(content)
        constrainToSafeHeight(content)
        defaultFocus?.post { defaultFocus.requestFocus() }
    }

    override fun dismiss() {
        if (closing) return
        dismissWithAnimation { super.dismiss() }
    }

    private fun buildContent(): View {
        val c = colors
        val sheetWidth = AppleOverlayChrome.sheetWidth(context)
        val host = FrameLayout(context)
        OverlayWindowInsets.bind(
            host,
            extraBottomPx = AppleOverlayChrome.dp(context, 8),
            includeIme = includeIme
        )
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipToPadding = false
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AppleOverlayChrome.cardBackground(context, c.card)
            clipToOutline = true
        }
        if (!title.isNullOrBlank() && !cancelTitle.isNullOrBlank() && !confirmTitle.isNullOrBlank()) {
            val confirmHandler = onConfirm
            card.addView(
                AppleOverlayChrome.formNav(
                    context = context,
                    colors = c,
                    title = title,
                    cancelTitle = cancelTitle,
                    confirmTitle = confirmTitle,
                    onCancel = { dismissWithAnimation { super.dismiss() } },
                    onConfirm = {
                        if (confirmHandler?.invoke() != false) {
                            dismissWithAnimation { super.dismiss() }
                        }
                    }
                )
            )
            card.addView(AppleOverlayChrome.hairline(context, c.hairline))
        }
        val body = contentView
        (body.parent as? ViewGroup)?.removeView(body)
        card.addView(body, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        host.addView(root, FrameLayout.LayoutParams(sheetWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        })
        AppleOverlayChrome.bindScrimDismiss(host, root) {
            dismissWithAnimation { super.dismiss() }
        }
        return host
    }

    private fun constrainToSafeHeight(host: View) {
        host.doOnLayout {
            val scroll = findScroll(contentView) ?: findScroll(host) ?: return@doOnLayout
            val sheet = (host as? ViewGroup)?.getChildAt(0) ?: return@doOnLayout
            val insets = ViewCompat.getRootWindowInsets(host)?.let { OverlayWindowInsets.edgesFrom(it, includeIme) }
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
            content: View,
            includeIme: Boolean = false,
            title: CharSequence? = null,
            cancelTitle: CharSequence? = null,
            confirmTitle: CharSequence? = null,
            onConfirm: (() -> Boolean)? = null,
            defaultFocus: View? = null,
            onDismiss: () -> Unit = {}
        ): Dialog = AppleFormSheet(
            context = context,
            contentView = content,
            includeIme = includeIme,
            title = title,
            cancelTitle = cancelTitle,
            confirmTitle = confirmTitle,
            onConfirm = onConfirm,
            defaultFocus = defaultFocus
        ).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(true)
            setOnDismissListener { onDismiss() }
            show()
        }
    }
}
