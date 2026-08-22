package com.example.openvideo.ui.playlist

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.openvideo.R
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionStyle
import com.example.openvideo.core.ui.AppleOverlayChrome
import com.example.openvideo.core.ui.AppleOverlayColors
import com.example.openvideo.core.ui.OverlayWindowInsets

class PlaylistRenameActionSheet private constructor(
    context: Context,
    private val initialName: String,
    private val onConfirm: (String) -> Unit
) : Dialog(context) {

    private var closing = false
    private var content: View? = null
    private lateinit var input: EditText
    private val colors: AppleOverlayColors get() = AppleOverlayColors.from(context)

    override fun onStart() {
        super.onStart()
        val content = buildContent()
        this.content = content
        setContentView(content)
        window?.let { AppleOverlayChrome.configureBottomWindow(it, context, colors, blur = true) }
        AppleOverlayChrome.enterFromBottom(content)
        input.requestFocus()
        input.post {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            context.getSystemService(InputMethodManager::class.java)
                ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun dismiss() {
        if (closing) return
        dismissWithAnimation { super.dismiss() }
    }

    private fun buildContent(): View {
        val c = colors
        val sheetWidth = AppleOverlayChrome.sheetWidth(context)
        val host = FrameLayout(context)
        OverlayWindowInsets.bind(host, extraBottomPx = AppleOverlayChrome.dp(context, 8), includeIme = true)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipToPadding = false
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AppleOverlayChrome.cardBackground(context, c.card)
            clipToOutline = true
        }
        card.addView(TextView(context).apply {
            setText(R.string.playlist_rename_title)
            gravity = Gravity.CENTER
            setTextColor(c.title)
            textSize = AppleOverlayChrome.TITLE_SP
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setPadding(
                AppleOverlayChrome.dp(context, 18),
                AppleOverlayChrome.dp(context, 18),
                AppleOverlayChrome.dp(context, 18),
                AppleOverlayChrome.dp(context, 12)
            )
        })
        input = EditText(context).apply {
            setText(initialName)
            selectAll()
            setSingleLine(true)
            setTextColor(c.title)
            setHintTextColor(c.message)
            textSize = 17f
            background = AppleOverlayChrome.cardBackground(context, c.input, radiusDp = 12)
            setPadding(AppleOverlayChrome.dp(context, 14), 0, AppleOverlayChrome.dp(context, 14), 0)
            minHeight = AppleOverlayChrome.dp(context, 48)
        }
        card.addView(input, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = AppleOverlayChrome.dp(context, 16)
            rightMargin = AppleOverlayChrome.dp(context, 16)
            bottomMargin = AppleOverlayChrome.dp(context, 14)
        })
        card.addView(AppleOverlayChrome.hairline(context, c.hairline))
        card.addView(
            AppleOverlayChrome.actionRow(
                context = context,
                action = AppleAction(context.getString(R.string.action_ok), AppleActionStyle.CANCEL, bold = true),
                colors = c
            ) {
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) onConfirm(name)
                dismissWithAnimation { super.dismiss() }
            }
        )

        val cancelCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AppleOverlayChrome.cardBackground(context, c.card)
            clipToOutline = true
        }
        cancelCard.addView(
            AppleOverlayChrome.actionRow(
                context = context,
                action = AppleAction(context.getString(R.string.action_cancel), AppleActionStyle.CANCEL, bold = true),
                colors = c
            ) {
                dismissWithAnimation { super.dismiss() }
            }
        )

        root.addView(card)
        root.addView(cancelCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = AppleOverlayChrome.dp(context, 10) })
        host.addView(root, FrameLayout.LayoutParams(sheetWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        })
        AppleOverlayChrome.bindScrimDismiss(host, root) {
            dismissWithAnimation { super.dismiss() }
        }
        return host
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
            initialName: String,
            onDismiss: () -> Unit,
            onConfirm: (String) -> Unit
        ): Dialog =
            PlaylistRenameActionSheet(context, initialName, onConfirm).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCanceledOnTouchOutside(true)
                setOnDismissListener { onDismiss() }
                show()
            }
    }
}
