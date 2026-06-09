package com.example.openvideo.ui.player

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import com.google.android.material.switchmaterial.SwitchMaterial

class PlayerSettingsRowBuilder(
    private val context: Context,
    private val playerPrefs: PlayerPrefs,
    private val detailContainerProvider: () -> LinearLayout,
    private val openNestedDetailScreen: (String, () -> Unit) -> Unit,
    private val onNestedChoiceSelected: () -> Unit
) {

    fun addInfoRow(title: String, value: String) {
        detailContainerProvider().addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(56)
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = value
                setTextColor(Color.rgb(176, 176, 176))
                textSize = 14f
                gravity = Gravity.END
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        })
        addDivider(detailContainerProvider())
    }

    fun addDisabledRow(title: String) {
        detailContainerProvider().addView(TextView(context).apply {
            text = title
            setTextColor(Color.rgb(85, 85, 85))
            textSize = 15f
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(56)
        })
        addDivider(detailContainerProvider())
    }

    fun addSwitchRow(
        parent: LinearLayout,
        title: String,
        checked: Boolean,
        onChanged: (Boolean) -> Unit
    ) {
        parent.addView(LinearLayout(context).apply {
            isBaselineAligned = false
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(56)
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(8)
                }
            })
            addView(SwitchMaterial(context).apply {
                isChecked = checked
                setOnCheckedChangeListener { _: CompoundButton, value: Boolean -> onChanged(value) }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dp(8)
                    gravity = Gravity.CENTER_VERTICAL
                }
            })
        })
        addDivider(parent)
    }

    fun addSwitchRows(
        rows: List<PlayerSettingsSwitchSpec>,
        parent: LinearLayout = detailContainerProvider()
    ) {
        rows.forEach { row ->
            addSwitchRow(
                parent = parent,
                title = row.title,
                checked = row.checked,
                onChanged = row.onChanged
            )
        }
    }

    fun addRadioRow(
        title: String,
        checked: Boolean,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        detailContainerProvider().addView(RadioButton(context).apply {
            text = title
            isChecked = checked
            isEnabled = enabled
            buttonTintList = context.getColorStateList(R.color.player_accent)
            setTextColor(if (enabled) Color.WHITE else Color.rgb(85, 85, 85))
            textSize = 15f
            minHeight = dp(56)
            setOnClickListener { if (enabled) onClick() }
        })
        addDivider(detailContainerProvider())
    }

    fun addCheckboxRow(
        title: String,
        checked: Boolean,
        enabled: Boolean = true,
        onChanged: (Boolean) -> Unit
    ) {
        detailContainerProvider().addView(CheckBox(context).apply {
            text = title
            isChecked = checked
            isEnabled = enabled
            buttonTintList = context.getColorStateList(R.color.player_accent)
            setTextColor(if (enabled) Color.WHITE else Color.rgb(85, 85, 85))
            textSize = 15f
            minHeight = dp(56)
            setOnCheckedChangeListener { _, value -> onChanged(value) }
        })
        addDivider(detailContainerProvider())
    }

    fun addActionRow(title: String, onClick: () -> Unit) {
        detailContainerProvider().addView(
            TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                minHeight = dp(56)
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        )
        addDivider(detailContainerProvider())
    }

    fun addActionRows(rows: List<PlayerSettingsActionSpec>) {
        rows.forEach { row ->
            if (row.value == null) {
                addActionRow(row.title, row.onClick)
            } else {
                addActionRow(row.title, row.value, row.onClick)
            }
        }
    }

    fun addAccentActionRow(parent: LinearLayout, title: String, onClick: () -> Unit) {
        parent.addView(
            TextView(context).apply {
                text = title
                setTextColor(context.getColor(R.color.player_accent))
                textSize = 15f
                gravity = Gravity.CENTER_VERTICAL
                minHeight = dp(56)
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        )
        addDivider(parent)
    }

    fun addActionRow(title: String, value: String, onClick: () -> Unit) {
        detailContainerProvider().addView(LinearLayout(context).apply {
            isBaselineAligned = false
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(56)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(8)
                }
            })
            addView(TextView(context).apply {
                text = value
                setTextColor(context.getColor(R.color.player_accent))
                textSize = 14f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(5), dp(12), dp(5))
                background = context.getDrawable(R.drawable.bg_player_settings_value)
                maxWidth = (context.resources.displayMetrics.widthPixels * 0.5f).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            })
        })
        addDivider(detailContainerProvider())
    }

    fun addSubtitleColorSwatchRow() {
        val density = context.resources.displayMetrics.density
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        row.addView(TextView(context).apply {
            text = context.getString(R.string.settings_subtitle_color)
            setTextColor(Color.WHITE)
            textSize = 15f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            includeFontPadding = false
        })
        val swatchRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }
        val swatches = PlayerSubtitleColorPolicy.options.mapIndexed { index, _ ->
            PlayerSubtitleColorSwatchBinder.createSwatchView(context, dp(36)).also { swatch ->
                val lp = swatch.layoutParams as LinearLayout.LayoutParams
                if (index > 0) lp.marginStart = dp(16)
                swatchRow.addView(swatch)
            }
        }
        PlayerSubtitleColorSwatchBinder.bindSwatches(
            swatches = swatches,
            context = context,
            playerPrefs = playerPrefs,
            density = density,
            onColorChanged = {}
        )
        row.addView(swatchRow)
        detailContainerProvider().addView(row)
        addDivider(detailContainerProvider())
    }

    fun addChoiceRow(
        title: String,
        value: String,
        options: List<String>,
        onSelected: (String) -> Unit
    ) {
        addActionRow(title, value) {
            openNestedDetailScreen(title) {
                options.forEach { opt ->
                    addRadioRow(
                        title = opt,
                        checked = opt == value
                    ) {
                        onSelected(opt)
                        onNestedChoiceSelected()
                    }
                }
            }
        }
    }

    fun addSeekRow(
        title: String,
        min: Int,
        maxValue: Int,
        value: Int,
        label: (Int) -> String,
        commitOnStop: Boolean = false,
        parent: LinearLayout = detailContainerProvider(),
        onChanged: (Int) -> Unit
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val valueView = TextView(context).apply {
            text = label(value)
            setTextColor(context.getColor(R.color.player_accent))
            textSize = 14f
        }
        row.addView(LinearLayout(context).apply {
            isBaselineAligned = false
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(8)
                }
            })
            addView(valueView.apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    marginStart = dp(8)
                }
            })
        })
        row.addView(SeekBar(context).apply {
            max = maxValue - min
            progress = (value - min).coerceIn(0, maxValue - min)
            progressTintList = context.getColorStateList(R.color.player_accent)
            thumbTintList = context.getColorStateList(R.color.player_accent)
            var pendingValue = value.coerceIn(min, maxValue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val next = min + progress
                    pendingValue = next
                    valueView.text = label(next)
                    if (!commitOnStop) onChanged(next)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (commitOnStop) onChanged(pendingValue)
                }
            })
        })
        parent.addView(row)
        addDivider(parent)
    }

    fun addDivider(parent: LinearLayout) {
        parent.addView(View(context).apply {
            background = context.getDrawable(R.drawable.bg_player_settings_divider)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
        })
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
