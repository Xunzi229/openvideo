package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.max
import kotlin.math.min

data class PlayerGlassSheetChoice<T>(
    val value: T,
    val label: String,
    val selected: Boolean,
    val enabled: Boolean = true
)

enum class PlayerGlassSheetChrome {
    CENTER,
    PLAYER_BOTTOM,
    PLAYER_SETTINGS_PANEL
}

object PlayerGlassSheetDialog {

    fun <T> showSingleChoice(
        context: Context,
        layoutInflater: LayoutInflater,
        titleRes: Int,
        choices: List<PlayerGlassSheetChoice<T>>,
        chrome: PlayerGlassSheetChrome = PlayerGlassSheetChrome.CENTER,
        playerPrefs: PlayerPrefs? = null,
        onDismiss: (() -> Unit)? = null,
        onSelected: (T) -> Unit
    ): Dialog {
        val (content, list, scroll) = inflate(context, layoutInflater, titleRes, chrome)
        val spacingPx = context.resources.getDimensionPixelSize(R.dimen.player_aspect_option_spacing)
        var dialog: Dialog? = null

        choices.forEachIndexed { index, choice ->
            val row = layoutInflater.inflate(rowLayout(chrome), list, false)
            row.findViewById<TextView>(R.id.player_glass_sheet_label).text = choice.label
            row.isEnabled = choice.enabled
            row.isClickable = choice.enabled
            row.isFocusable = choice.enabled
            row.alpha = if (choice.enabled) 1f else 0.42f
            if (!choice.enabled) {
                row.foreground = null
            }
            applyChoiceState(context, row, choice.selected && choice.enabled, chrome)
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (index < choices.lastIndex) bottomMargin = spacingPx
            }
            row.setOnClickListener {
                if (!choice.enabled) return@setOnClickListener
                dialog?.dismiss()
                onSelected(choice.value)
            }
            list.addView(row)
        }

        if (chrome == PlayerGlassSheetChrome.CENTER) {
            capScroll(scroll, centerDialogContentWidthPx(context), 0)
        }

        dialog = when (chrome) {
            PlayerGlassSheetChrome.CENTER -> MaterialAlertDialogBuilder(context)
                .setView(content)
                .setOnDismissListener { onDismiss?.invoke() }
                .create()
            PlayerGlassSheetChrome.PLAYER_BOTTOM,
            PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL -> Dialog(context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(content)
                setCanceledOnTouchOutside(true)
                setOnDismissListener { onDismiss?.invoke() }
            }
        }
        dialog.prepareCenterAnimation(chrome)
        dialog.show()
        dialog.applyChrome(chrome, playerPrefs, content)
        if (chrome != PlayerGlassSheetChrome.CENTER) {
            scroll.post { capScroll(scroll, null, 0) }
        }
        return dialog
    }

    private fun Dialog.prepareCenterAnimation(chrome: PlayerGlassSheetChrome) {
        if (chrome != PlayerGlassSheetChrome.CENTER) return
        val window = window ?: return
        window.attributes = window.attributes.apply {
            windowAnimations = R.style.Animation_OpenVideo_CenterDialog
        }
    }

    fun Dialog.applyChrome(
        chrome: PlayerGlassSheetChrome = PlayerGlassSheetChrome.CENTER,
        playerPrefs: PlayerPrefs? = null,
        panelRoot: View? = null
    ) {
        val window = window ?: return
        if (chrome == PlayerGlassSheetChrome.PLAYER_BOTTOM) {
            window.applyBottomChrome(context)
            return
        }
        if (chrome == PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL) {
            val prefs = playerPrefs ?: return
            val dm = context.resources.displayMetrics
            PlayerSettingsSheetChrome.applyWindowLayout(window, dm.widthPixels, dm.heightPixels, dm.density)
            PlayerSettingsSheetChrome.applyBackdrop(window, prefs, dm.density)
            panelRoot?.let { PlayerSettingsSheetChrome.applyPanelOpacity(it, prefs) }
            return
        }
        val context = context
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.alpha = 1f
        val dm = context.resources.displayMetrics
        window.setLayout(centerDialogWidthPx(context), LinearLayout.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.apply {
            dimAmount = 0.48f
            windowAnimations = R.style.Animation_OpenVideo_CenterDialog
        }
        if (PlayerSettingsSheetStylePolicy.supportsBackdropBlur(Build.VERSION.SDK_INT)) {
            window.setBackgroundBlurRadius(max(1, (18f * dm.density).toInt()))
        }
    }

    private fun centerDialogWidthPx(context: Context): Int {
        val dm = context.resources.displayMetrics
        val maxWidth = context.resources.getDimensionPixelSize(R.dimen.player_aspect_dialog_max_width)
        val gutter = PlayerQuickEntryDialogPolicy.sheetPaddingPx(dm.density)
        return min(
            maxWidth,
            max(0, (dm.widthPixels * 0.9f).toInt() - 2 * gutter)
        )
    }

    private fun centerDialogContentWidthPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        val horizontalInset = context.resources.getDimensionPixelSize(R.dimen.player_aspect_dialog_horizontal_inset)
        return (centerDialogWidthPx(context) - 2 * horizontalInset - (48f * density).toInt()).coerceAtLeast(1)
    }

    private fun android.view.Window.applyBottomChrome(context: Context) {
        val dm = context.resources.displayMetrics
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout(LayoutParams.MATCH_PARENT, (dm.heightPixels * 0.56f).toInt())
        setGravity(Gravity.BOTTOM)
        decorView.setPadding(0, 0, 0, 0)
        decorView.elevation = context.resources.displayMetrics.density * 20f
        addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        attributes = attributes.apply {
            x = 0
            y = 0
            dimAmount = 0.12f
            windowAnimations = R.style.Animation_OpenVideo_BottomSheet
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            PlayerSettingsSheetStylePolicy.supportsBackdropBlur(Build.VERSION.SDK_INT)
        ) {
            setBackgroundBlurRadius(max(1, (18f * dm.density).toInt()))
        }
    }

    fun applyRowVisual(context: Context, row: View, selected: Boolean) {
        val density = context.resources.displayMetrics.density
        row.setBackgroundResource(
            if (selected) R.drawable.player_aspect_ratio_row_selected
            else R.drawable.player_aspect_ratio_row_unselected
        )
        row.findViewById<ImageView>(R.id.player_glass_sheet_radio).setImageResource(
            if (selected) R.drawable.ic_player_aspect_radio_on
            else R.drawable.ic_player_aspect_radio_off
        )
        row.findViewById<TextView>(R.id.player_glass_sheet_label).setTextColor(
            ContextCompat.getColor(
                context,
                if (selected) R.color.player_aspect_row_label_selected
                else R.color.player_aspect_row_label_normal
            )
        )
        row.translationZ = if (selected) 4f * density else 0f
    }

    private fun rowLayout(chrome: PlayerGlassSheetChrome): Int =
        when (chrome) {
            PlayerGlassSheetChrome.CENTER -> R.layout.item_player_glass_sheet_row
            PlayerGlassSheetChrome.PLAYER_BOTTOM,
            PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL -> R.layout.item_player_quick_bottom_sheet_row
        }

    private fun applyChoiceState(
        context: Context,
        row: View,
        selected: Boolean,
        chrome: PlayerGlassSheetChrome
    ) {
        when (chrome) {
            PlayerGlassSheetChrome.CENTER -> applyRowVisual(context, row, selected)
            PlayerGlassSheetChrome.PLAYER_BOTTOM,
            PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL -> applyBottomRowVisual(context, row, selected)
        }
    }

    private fun applyBottomRowVisual(context: Context, row: View, selected: Boolean) {
        row.setBackgroundResource(R.drawable.bg_player_touch)
        val radio = row.findViewById<ImageView>(R.id.player_glass_sheet_radio)
        radio.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        radio.setImageResource(R.drawable.ic_player_aspect_radio_on)
        row.findViewById<TextView>(R.id.player_glass_sheet_label).setTextColor(
            ContextCompat.getColor(
                context,
                if (selected) R.color.player_accent else R.color.player_title_normal
            )
        )
        row.translationZ = 0f
    }

    private fun inflate(
        context: Context,
        layoutInflater: LayoutInflater,
        titleRes: Int,
        chrome: PlayerGlassSheetChrome
    ): Triple<View, LinearLayout, NestedScrollView> {
        val layout = when (chrome) {
            PlayerGlassSheetChrome.CENTER -> R.layout.dialog_player_glass_sheet
            PlayerGlassSheetChrome.PLAYER_BOTTOM,
            PlayerGlassSheetChrome.PLAYER_SETTINGS_PANEL -> R.layout.dialog_player_quick_bottom_sheet
        }
        val content = layoutInflater.inflate(layout, null, false)
        content.findViewById<TextView>(R.id.player_glass_sheet_title).setText(titleRes)
        val list = content.findViewById<LinearLayout>(R.id.player_glass_sheet_option_list)
        val scroll = content.findViewById<NestedScrollView>(R.id.player_glass_sheet_scroll)
        return Triple(content, list, scroll)
    }

    private fun capScroll(scrollView: NestedScrollView, widthPxOverride: Int?, attempt: Int) {
        val inner = scrollView.getChildAt(0) ?: return
        val widthPx = widthPxOverride
            ?: scrollView.width.takeIf { it > 0 }
            ?: scrollView.measuredWidth.takeIf { it > 0 }
        if (widthPx == null || widthPx <= 0) {
            if (attempt < 6) {
                scrollView.post { capScroll(scrollView, null, attempt + 1) }
            }
            return
        }
        val dm = scrollView.resources.displayMetrics
        val capFromScreen = (dm.heightPixels * 0.48f).toInt()
        val capFromDimen = scrollView.resources.getDimensionPixelSize(
            R.dimen.player_aspect_dialog_option_scroll_cap
        )
        val capPx = min(capFromScreen, capFromDimen)

        inner.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val layoutParams = scrollView.layoutParams ?: return
        layoutParams.height = if (inner.measuredHeight > capPx) capPx else ViewGroup.LayoutParams.WRAP_CONTENT
        scrollView.layoutParams = layoutParams
        scrollView.requestLayout()
    }
}
