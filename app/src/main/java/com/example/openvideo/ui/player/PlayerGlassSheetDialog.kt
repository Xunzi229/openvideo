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
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.example.openvideo.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.max
import kotlin.math.min

data class PlayerGlassSheetChoice<T>(
    val value: T,
    val label: String,
    val selected: Boolean,
    val enabled: Boolean = true
)

object PlayerGlassSheetDialog {

    fun <T> showSingleChoice(
        context: Context,
        layoutInflater: LayoutInflater,
        titleRes: Int,
        choices: List<PlayerGlassSheetChoice<T>>,
        onDismiss: (() -> Unit)? = null,
        onSelected: (T) -> Unit
    ): AlertDialog {
        val (content, list, scroll) = inflate(context, layoutInflater, titleRes)
        val spacingPx = context.resources.getDimensionPixelSize(R.dimen.player_aspect_option_spacing)
        var dialog: AlertDialog? = null

        choices.forEachIndexed { index, choice ->
            val row = layoutInflater.inflate(R.layout.item_player_glass_sheet_row, list, false)
            row.findViewById<TextView>(R.id.player_glass_sheet_label).text = choice.label
            row.isEnabled = choice.enabled
            row.isClickable = choice.enabled
            row.isFocusable = choice.enabled
            row.alpha = if (choice.enabled) 1f else 0.42f
            if (!choice.enabled) {
                row.foreground = null
            }
            applyRowVisual(context, row, choice.selected && choice.enabled)
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

        dialog = MaterialAlertDialogBuilder(context)
            .setView(content)
            .setOnDismissListener { onDismiss?.invoke() }
            .create()
        dialog.show()
        dialog.applyChrome()
        scroll.post { capScroll(scroll, 0) }
        return dialog
    }

    fun Dialog.applyChrome() {
        val window = window ?: return
        val context = context
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.alpha = 1f
        val dm = context.resources.displayMetrics
        val maxWidth = context.resources.getDimensionPixelSize(R.dimen.player_aspect_dialog_max_width)
        val gutter = PlayerQuickEntryDialogPolicy.sheetPaddingPx(dm.density)
        val dialogWidth = min(
            maxWidth,
            max(0, (dm.widthPixels * 0.9f).toInt() - 2 * gutter)
        )
        window.setLayout(dialogWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.apply {
            dimAmount = 0.48f
        }
        if (PlayerSettingsSheetStylePolicy.supportsBackdropBlur(Build.VERSION.SDK_INT)) {
            window.setBackgroundBlurRadius(max(1, (18f * dm.density).toInt()))
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

    private fun inflate(
        context: Context,
        layoutInflater: LayoutInflater,
        titleRes: Int
    ): Triple<View, LinearLayout, NestedScrollView> {
        val content = layoutInflater.inflate(R.layout.dialog_player_glass_sheet, null, false)
        content.findViewById<TextView>(R.id.player_glass_sheet_title).setText(titleRes)
        val list = content.findViewById<LinearLayout>(R.id.player_glass_sheet_option_list)
        val scroll = content.findViewById<NestedScrollView>(R.id.player_glass_sheet_scroll)
        return Triple(content, list, scroll)
    }

    private fun capScroll(scrollView: NestedScrollView, attempt: Int) {
        val inner = scrollView.getChildAt(0) ?: return
        val widthPx = scrollView.width.takeIf { it > 0 } ?: scrollView.measuredWidth.takeIf { it > 0 }
        if (widthPx == null || widthPx <= 0) {
            if (attempt < 6) {
                scrollView.post { capScroll(scrollView, attempt + 1) }
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
