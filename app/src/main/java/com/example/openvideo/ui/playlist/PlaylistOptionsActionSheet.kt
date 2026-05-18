package com.example.openvideo.ui.playlist

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.openvideo.R
import kotlin.math.max

class PlaylistOptionsActionSheet private constructor(
    context: Context,
    private val playlistName: String,
    private val onRename: () -> Unit,
    private val onDelete: () -> Unit
) : Dialog(context) {

    private var closing = false
    private var content: View? = null
    private val colors: SheetColors get() = SheetColors.from(context)

    override fun onStart() {
        super.onStart()
        val content = buildContent()
        this.content = content
        setContentView(content)
        configureWindow()
        enter(content)
    }

    override fun dismiss() {
        if (closing) return
        dismissWithAnimation { super.dismiss() }
    }

    private fun buildContent(): View {
        val c = colors
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(14), dp(8))
            clipToPadding = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.setPadding(dp(14), 0, dp(14), max(dp(8), bottomInset + dp(8)))
            insets
        }

        val actionCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(c)
            clipToOutline = true
        }
        actionCard.addView(TextView(context).apply {
            text = playlistName
            gravity = Gravity.CENTER
            setTextColor(c.message)
            textSize = 14f
            includeFontPadding = false
            setPadding(dp(18), dp(17), dp(18), dp(15))
        }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        actionCard.addView(divider(c))
        actionCard.addView(actionRow(
            text = context.getString(R.string.playlist_option_rename),
            color = c.cancel,
            bold = false
        ) {
            dismissWithAnimation {
                super.dismiss()
                onRename()
            }
        })
        actionCard.addView(divider(c))
        actionCard.addView(actionRow(
            text = context.getString(R.string.playlist_option_delete),
            color = c.danger,
            bold = false
        ) {
            dismissWithAnimation {
                super.dismiss()
                onDelete()
            }
        })

        val cancelCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(c)
            clipToOutline = true
        }
        cancelCard.addView(actionRow(
            text = context.getString(R.string.action_cancel),
            color = c.cancel,
            bold = true
        ) {
            dismissWithAnimation { super.dismiss() }
        })

        root.addView(actionCard)
        root.addView(cancelCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) })
        return root
    }

    private fun actionRow(text: String, color: Int, bold: Boolean, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(color)
            textSize = 19f
            typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            includeFontPadding = false
            minHeight = dp(56)
            isClickable = true
            isFocusable = true
            foreground = selectableForeground()
            setOnClickListener { onClick() }
        }

    private fun divider(colors: SheetColors): View =
        View(context).apply { setBackgroundColor(colors.divider) }.also {
            it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        }

    private fun roundedBackground(colors: SheetColors): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(colors.card)
        }

    private fun selectableForeground(): android.graphics.drawable.Drawable? {
        val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        return typedArray.getDrawable(0).also { typedArray.recycle() }
    }

    private fun configureWindow() {
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = colors.dimAmount }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setBackgroundBlurRadius(dp(20))
        }
    }

    private fun enter(content: View) {
        content.alpha = 0f
        content.translationY = dp(18).toFloat()
        content.animate().alpha(1f).translationY(0f).setDuration(220L).start()
    }

    private fun dismissWithAnimation(onComplete: () -> Unit) {
        val content = content ?: return onComplete()
        closing = true
        content.animate()
            .alpha(0f)
            .translationY(dp(18).toFloat())
            .setDuration(160L)
            .withEndAction {
                onComplete()
                closing = false
            }
            .start()
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        fun show(
            context: Context,
            playlistName: String,
            onDismiss: () -> Unit,
            onRename: () -> Unit,
            onDelete: () -> Unit
        ): Dialog =
            PlaylistOptionsActionSheet(context, playlistName, onRename, onDelete).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCanceledOnTouchOutside(true)
                setOnDismissListener { onDismiss() }
                show()
            }
    }

    private data class SheetColors(
        val card: Int,
        val message: Int,
        val divider: Int,
        val danger: Int,
        val cancel: Int,
        val dimAmount: Float
    ) {
        companion object {
            fun from(context: Context): SheetColors {
                val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                return if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                    SheetColors(
                        card = Color.parseColor("#D91C1C1E"),
                        message = Color.parseColor("#AEAEB2"),
                        divider = Color.parseColor("#3DFFFFFF"),
                        danger = Color.parseColor("#FF453A"),
                        cancel = Color.parseColor("#0A84FF"),
                        dimAmount = 0.58f
                    )
                } else {
                    SheetColors(
                        card = Color.parseColor("#EBFFFFFF"),
                        message = Color.parseColor("#6E6E73"),
                        divider = Color.parseColor("#2E3C3C43"),
                        danger = Color.parseColor("#FF3B30"),
                        cancel = Color.parseColor("#007AFF"),
                        dimAmount = 0.52f
                    )
                }
            }
        }
    }
}
