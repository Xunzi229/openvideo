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
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.openvideo.R
import kotlin.math.max

class PlaylistRenameActionSheet private constructor(
    context: Context,
    private val initialName: String,
    private val onConfirm: (String) -> Unit
) : Dialog(context) {

    private var closing = false
    private var content: View? = null
    private lateinit var input: EditText
    private val colors: SheetColors get() = SheetColors.from(context)

    override fun onStart() {
        super.onStart()
        val content = buildContent()
        this.content = content
        setContentView(content)
        configureWindow()
        enter(content)
        input.requestFocus()
        input.post {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            context.getSystemService(InputMethodManager::class.java)?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
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
            val imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(dp(14), 0, dp(14), max(dp(8), max(bottomInset, imeInset) + dp(8)))
            insets
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(c)
            clipToOutline = true
        }
        card.addView(TextView(context).apply {
            setText(R.string.playlist_rename_title)
            gravity = Gravity.CENTER
            setTextColor(c.title)
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setPadding(dp(18), dp(18), dp(18), dp(12))
        })
        input = EditText(context).apply {
            setText(initialName)
            selectAll()
            setSingleLine(true)
            setTextColor(c.title)
            setHintTextColor(c.message)
            textSize = 17f
            background = inputBackground(c)
            setPadding(dp(14), 0, dp(14), 0)
            minHeight = dp(48)
        }
        card.addView(input, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dp(16)
            rightMargin = dp(16)
            bottomMargin = dp(14)
        })
        card.addView(divider(c))
        card.addView(actionRow(context.getString(R.string.action_ok), c.cancel, true) {
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) onConfirm(name)
            dismissWithAnimation { super.dismiss() }
        })

        val cancelCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(c)
            clipToOutline = true
        }
        cancelCard.addView(actionRow(context.getString(R.string.action_cancel), c.cancel, true) {
            dismissWithAnimation { super.dismiss() }
        })

        root.addView(card)
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

    private fun inputBackground(colors: SheetColors): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(colors.input)
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

    private data class SheetColors(
        val card: Int,
        val input: Int,
        val title: Int,
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
                        input = Color.parseColor("#2C2C2E"),
                        title = Color.parseColor("#F2F2F7"),
                        message = Color.parseColor("#AEAEB2"),
                        divider = Color.parseColor("#3DFFFFFF"),
                        danger = Color.parseColor("#FF453A"),
                        cancel = Color.parseColor("#0A84FF"),
                        dimAmount = 0.58f
                    )
                } else {
                    SheetColors(
                        card = Color.parseColor("#EBFFFFFF"),
                        input = Color.parseColor("#1AF2F2F7"),
                        title = Color.parseColor("#1C1C1E"),
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
