package com.example.openvideo.ui.home

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.openvideo.R
import com.example.openvideo.core.ui.AppleFormSheet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class VideoLibraryFilterPopover(
    private val anchor: View,
    initial: MediaLibraryAdvancedFilters,
    private val onApply: (MediaLibraryAdvancedFilters) -> Unit,
    private val onDismiss: () -> Unit = {}
) {
    private var dialog: Dialog? = null
    private var draft = VideoLibraryFilterUiState.from(initial)

    fun toggle(current: MediaLibraryAdvancedFilters = draft.toAdvancedFilters()) {
        draft = VideoLibraryFilterUiState.from(current)
        if (isShowing()) dismiss() else show()
    }

    fun isShowing(): Boolean = dialog?.isShowing == true

    fun dismiss() {
        dialog?.dismiss()
    }

    fun show() {
        if (isShowing()) return
        val context = anchor.context
        val content = LayoutInflater.from(context).inflate(R.layout.view_video_library_filter_popover, null, false)
        bindChipGroups(content, context)
        bindActions(content)
        dialog = AppleFormSheet.show(
            context = context,
            content = content,
            onDismiss = {
                dialog = null
                onDismiss()
            }
        )
    }

    private fun bindChipGroups(root: View, context: Context) {
        val durationGroup = root.findViewById<ChipGroup>(R.id.chip_group_duration)
        val formatGroup = root.findViewById<ChipGroup>(R.id.chip_group_format)
        val dateGroup = root.findViewById<ChipGroup>(R.id.chip_group_date)

        val durationOptions = listOf(
            DurationFilter.ANY to context.getString(R.string.home_filter_chip_duration_all),
            DurationFilter.SHORT to context.getString(R.string.home_filter_chip_duration_0_5),
            DurationFilter.MEDIUM to context.getString(R.string.home_filter_chip_duration_5_20),
            DurationFilter.LONG to context.getString(R.string.home_filter_chip_duration_20_plus)
        )
        val formatOptions = listOf(
            null to context.getString(R.string.home_filter_chip_format_all),
            "mp4" to context.getString(R.string.home_filter_format_mp4),
            "mkv" to context.getString(R.string.home_filter_format_mkv),
            "avi" to context.getString(R.string.home_filter_format_avi),
            "mov" to context.getString(R.string.home_filter_format_mov)
        )
        val dateOptions = listOf(
            DateFilter.ANY to context.getString(R.string.home_filter_chip_date_all),
            DateFilter.TODAY to context.getString(R.string.home_filter_date_today),
            DateFilter.LAST_7_DAYS to context.getString(R.string.home_filter_date_7d),
            DateFilter.LAST_30_DAYS to context.getString(R.string.home_filter_date_30d),
            DateFilter.OLDER_THAN_30_DAYS to context.getString(R.string.home_filter_date_older)
        )

        populateChipGroup(durationGroup, durationOptions, draft.duration) { selected ->
            draft = draft.copy(duration = selected)
        }
        populateChipGroup(formatGroup, formatOptions, draft.formatExtension) { selected ->
            draft = draft.copy(formatExtension = selected)
        }
        populateChipGroup(dateGroup, dateOptions, draft.date) { selected ->
            draft = draft.copy(date = selected)
        }
    }

    private fun <T> populateChipGroup(
        group: ChipGroup,
        options: List<Pair<T, String>>,
        current: T,
        onSelected: (T) -> Unit
    ) {
        group.removeAllViews()
        val context = group.context
        options.forEach { (value, label) ->
            val chip = Chip(context).apply {
                text = label
                isCheckable = true
                isChecked = value == current
                checkedIcon = null
                isCheckedIconVisible = false
                chipStrokeWidth = 0f
                chipMinHeight = context.resources.displayMetrics.density * 36f
                textSize = 13f
                setEnsureMinTouchTargetSize(false)
                bindFilterChipStyle(this, isChecked)
                setOnClickListener {
                    onSelected(value)
                    for (i in 0 until group.childCount) {
                        val child = group.getChildAt(i) as? Chip ?: continue
                        bindFilterChipStyle(child, child == this)
                    }
                }
            }
            group.addView(chip)
        }
    }

    private fun bindFilterChipStyle(chip: Chip, selected: Boolean) {
        val context = chip.context
        chip.setTextColor(
            ContextCompat.getColor(
                context,
                if (selected) R.color.ov_filter_chip_selected_text else R.color.ov_filter_chip_text
            )
        )
        chip.chipBackgroundColor = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                if (selected) R.color.ov_filter_chip_selected_bg else R.color.ov_filter_chip_bg
            )
        )
        chip.chipStrokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                if (selected) R.color.ov_filter_chip_selected_stroke else R.color.ov_filter_chip_stroke
            )
        )
        chip.chipStrokeWidth = context.resources.displayMetrics.density
        chip.elevation = if (selected) context.resources.displayMetrics.density * 4f else 0f
    }

    private fun bindActions(root: View) {
        root.findViewById<TextView>(R.id.btn_filter_reset).setOnClickListener {
            draft = VideoLibraryFilterUiState.default()
            bindChipGroups(root, root.context)
        }
        root.findViewById<TextView>(R.id.btn_filter_cancel).setOnClickListener { dismiss() }
        root.findViewById<TextView>(R.id.btn_filter_apply).setOnClickListener {
            onApply(draft.toAdvancedFilters())
            dismiss()
        }
    }
}
