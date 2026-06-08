package com.example.openvideo.ui.settings

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
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.openvideo.R
import kotlin.math.max

class SettingsConfirmationActionSheet private constructor(
    context: Context,
    private val title: CharSequence,
    private val message: CharSequence,
    @param:StringRes private val confirmRes: Int,
    @param:StringRes private val cancelRes: Int,
    private val onConfirm: () -> Unit
) : Dialog(context) {

    private var closing = false
    private var content: View? = null
    private var defaultFocusView: View? = null
    private val colors: ActionSheetColors
        get() = ActionSheetColors.from(context)

    override fun onStart() {
        super.onStart()
        val content = buildContent()
        this.content = content
        setContentView(content)
        configureWindow()
        SettingsConfirmationActionSheet.enter(content)
        requestDefaultFocus()
    }

    override fun dismiss() {
        if (closing) return
        val content = content
        if (content == null || !isShowing) {
            super.dismiss()
            return
        }
        dismissWithAnimation {
            super.dismiss()
        }
    }

    fun dismissWithAnimation(onComplete: () -> Unit = {}) {
        val content = content
        if (content == null) {
            onComplete()
            return
        }
        closing = true
        content.animate()
            .translationY(dp(18).toFloat())
            .alpha(0f)
            .setDuration(EXIT_ANIMATION_MS)
            .withEndAction {
                onComplete()
                closing = false
            }
            .start()
    }

    private fun buildContent(): View {
        val density = context.resources.displayMetrics.density
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

        val colors = colors
        val actionCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedSheetBackground(colors)
            clipToOutline = true
        }
        val textArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(19), dp(18), dp(16))
        }
        textArea.addView(TextView(context).apply {
            text = title
            gravity = Gravity.CENTER
            setTextColor(colors.title)
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        textArea.addView(TextView(context).apply {
            text = message
            gravity = Gravity.CENTER
            setTextColor(colors.message)
            textSize = 14f
            includeFontPadding = false
            setPadding(0, dp(7), 0, 0)
        }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        actionCard.addView(textArea)
        actionCard.addView(View(context).apply {
            setBackgroundColor(colors.divider)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
        actionCard.addView(actionText(
            textRes = confirmRes,
            color = colors.danger,
            bold = false
        ).apply {
            setOnClickListener {
                onConfirm()
                dismissWithAnimation { super@SettingsConfirmationActionSheet.dismiss() }
            }
        })

        val cancelCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedSheetBackground(colors)
            clipToOutline = true
        }
        val cancelAction = actionText(
            textRes = cancelRes,
            color = colors.cancel,
            bold = true
        ).apply {
            setOnClickListener { dismissWithAnimation { super@SettingsConfirmationActionSheet.dismiss() } }
        }
        defaultFocusView = cancelAction
        cancelCard.addView(cancelAction)

        root.addView(actionCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(cancelCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = (10f * density).toInt()
        })
        return root
    }

    private fun actionText(
        @StringRes textRes: Int,
        color: Int,
        bold: Boolean
    ): TextView =
        TextView(context).apply {
            setText(textRes)
            gravity = Gravity.CENTER
            setTextColor(color)
            textSize = 19f
            typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            includeFontPadding = false
            minHeight = dp(56)
            isClickable = true
            isFocusable = true
            foreground = selectableBorderlessForeground()
        }

    private fun roundedSheetBackground(colors: ActionSheetColors): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(colors.card)
        }

    private fun selectableBorderlessForeground(): android.graphics.drawable.Drawable? {
        val attrs = intArrayOf(android.R.attr.selectableItemBackground)
        val typedArray = context.obtainStyledAttributes(attrs)
        return typedArray.getDrawable(0).also {
            typedArray.recycle()
        }
    }

    private fun requestDefaultFocus() {
        defaultFocusView?.post {
            defaultFocusView?.requestFocus()
        }
    }

    private fun configureWindow() {
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                dimAmount = colors.dimAmount
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setBackgroundBlurRadius(dp(20))
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        private const val ENTER_ANIMATION_MS = 220L
        private const val EXIT_ANIMATION_MS = 160L

        fun show(
            context: Context,
            @StringRes titleRes: Int,
            @StringRes messageRes: Int,
            @StringRes confirmRes: Int,
            @StringRes cancelRes: Int,
            onDismiss: () -> Unit,
            onConfirm: () -> Unit
        ): Dialog =
            show(
                context = context,
                title = context.getString(titleRes),
                message = context.getString(messageRes),
                confirmRes = confirmRes,
                cancelRes = cancelRes,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )

        fun show(
            context: Context,
            @StringRes titleRes: Int,
            message: CharSequence,
            @StringRes confirmRes: Int,
            @StringRes cancelRes: Int,
            onDismiss: () -> Unit,
            onConfirm: () -> Unit
        ): Dialog =
            show(
                context = context,
                title = context.getString(titleRes),
                message = message,
                confirmRes = confirmRes,
                cancelRes = cancelRes,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )

        private fun show(
            context: Context,
            title: CharSequence,
            message: CharSequence,
            @StringRes confirmRes: Int,
            @StringRes cancelRes: Int,
            onDismiss: () -> Unit,
            onConfirm: () -> Unit
        ): Dialog =
            SettingsConfirmationActionSheet(
                context = context,
                title = title,
                message = message,
                confirmRes = confirmRes,
                cancelRes = cancelRes,
                onConfirm = onConfirm
            ).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCanceledOnTouchOutside(true)
                setOnDismissListener { onDismiss() }
                show()
            }

        fun enter(content: View) {
            content.alpha = 0f
            content.translationY = content.resources.displayMetrics.density * 18f
            content.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ENTER_ANIMATION_MS)
                .start()
        }
    }

    private data class ActionSheetColors(
        val card: Int,
        val title: Int,
        val message: Int,
        val divider: Int,
        val danger: Int,
        val cancel: Int,
        val dimAmount: Float
    ) {
        companion object {
            fun from(context: Context): ActionSheetColors {
                val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                return if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                    ActionSheetColors(
                        card = Color.parseColor("#D91C1C1E"),
                        title = Color.parseColor("#F2F2F7"),
                        message = Color.parseColor("#AEAEB2"),
                        divider = Color.parseColor("#3DFFFFFF"),
                        danger = Color.parseColor("#FF453A"),
                        cancel = Color.parseColor("#0A84FF"),
                        dimAmount = 0.58f
                    )
                } else {
                    ActionSheetColors(
                        card = Color.parseColor("#EBFFFFFF"),
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
