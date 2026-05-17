package com.example.openvideo.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import com.example.openvideo.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Top-end anchored filter popover with glass styling, chip groups, and light animations.
 * Apply/reset callbacks connect to [HomeViewModel] advanced filters.
 */
class VideoLibraryFilterPopover(
    private val anchor: View,
    initial: MediaLibraryAdvancedFilters,
    private val onApply: (MediaLibraryAdvancedFilters) -> Unit,
    private val onDismiss: () -> Unit = {}
) {
    private var popup: PopupWindow? = null
    private var draft = VideoLibraryFilterUiState.from(initial)
    private var popupVisible = false

    fun toggle(current: MediaLibraryAdvancedFilters = draft.toAdvancedFilters()) {
        draft = VideoLibraryFilterUiState.from(current)
        if (popupVisible) dismiss() else show()
    }

    fun isShowing(): Boolean = popupVisible

    fun dismiss() {
        val window = popup ?: return
        val panel = window.contentView?.findViewById<View>(R.id.filter_popover_root) ?: run {
            window.dismiss()
            return
        }
        panel.animate()
            .alpha(0f)
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(ANIM_OUT_MS)
            .withEndAction { window.dismiss() }
            .start()
    }

    fun show() {
        if (popupVisible) return
        val context = anchor.context
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(R.layout.overlay_video_library_filter, null, false)
        val scrim = root.findViewById<View>(R.id.filter_scrim)
        val popoverRoot = checkNotNull(root.findViewById(R.id.filter_popover_root))
        val arrow = root.findViewById<ImageView>(R.id.filter_popover_arrow)

        bindChipGroups(root, context)
        bindActions(root)

        scrim.setOnClickListener { dismiss() }
        popoverRoot.isClickable = true
        popoverRoot.setOnClickListener { /* keep taps on panel from passing through */ }

        val window = PopupWindow(
            root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            isClippingEnabled = false
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = context.resources.displayMetrics.density * 12f
            setOnDismissListener {
                popup = null
                popupVisible = false
                onDismiss()
            }
        }
        popup = window
        window.showAtLocation(anchor, Gravity.TOP or Gravity.START, 0, 0)

        popoverRoot.doOnLayout {
            positionPopover(popoverRoot, arrow, anchor, context)
            popoverRoot.alpha = 0f
            popoverRoot.scaleX = 0.94f
            popoverRoot.scaleY = 0.94f
            popoverRoot.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ANIM_IN_MS)
                .start()
        }
        popupVisible = true
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

    private fun positionPopover(host: View, arrow: ImageView, anchor: View, context: Context) {
        val dm = context.resources.displayMetrics
        val widthPercent = context.resources.getInteger(R.integer.ov_filter_popover_width_percent)
        val marginHorizontal = (12 * dm.density).toInt()
        val byPercent = (dm.widthPixels * widthPercent / 100f).toInt()
        val maxFit = dm.widthPixels - marginHorizontal * 2
        val panelWidth = minOf(byPercent, maxFit).coerceAtLeast(dm.widthPixels / 4)

        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val anchorCenterX = anchorLoc[0] + anchor.width / 2

        val overlayRoot = host.rootView.findViewById<View>(R.id.filter_overlay_root)
        val rootLoc = IntArray(2)
        overlayRoot.getLocationOnScreen(rootLoc)

        val marginEnd = marginHorizontal
        val gapBelowAnchor = (6 * dm.density).toInt()
        val x = dm.widthPixels - panelWidth - marginEnd - rootLoc[0]
        val leftMargin = x.coerceAtLeast(marginEnd)
        val y = anchorLoc[1] + anchor.height + gapBelowAnchor - rootLoc[1]

        host.layoutParams = (host.layoutParams as? FrameLayout.LayoutParams
            ?: FrameLayout.LayoutParams(panelWidth, ViewGroup.LayoutParams.WRAP_CONTENT)).apply {
            width = panelWidth
            gravity = Gravity.TOP or Gravity.START
            this.leftMargin = leftMargin
            topMargin = y.coerceAtLeast(marginEnd)
        }

        layoutArrowHorizontal(arrow, anchorCenterX, rootLoc[0] + leftMargin, panelWidth, dm.density)
    }

    private fun layoutArrowHorizontal(
        arrow: ImageView,
        anchorCenterScreenX: Int,
        popoverLeftScreenX: Int,
        popoverWidthPx: Int,
        density: Float
    ) {
        val arrowW = arrow.resources.getDimensionPixelSize(R.dimen.ov_filter_arrow_width)
        val minInset = (4 * density).toInt()
        val maxStart = (popoverWidthPx - arrowW - minInset).coerceAtLeast(minInset)
        val desired =
            anchorCenterScreenX - popoverLeftScreenX - arrowW / 2
        val clamped = desired.coerceIn(minInset, maxStart)
        arrow.updateLayoutParams<LinearLayout.LayoutParams> {
            width = arrowW
            height = arrow.resources.getDimensionPixelSize(R.dimen.ov_filter_arrow_height)
            marginStart = clamped
        }
    }

    companion object {
        private const val ANIM_IN_MS = 180L
        private const val ANIM_OUT_MS = 140L
    }
}
