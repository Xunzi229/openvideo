package com.example.openvideo.ui.player

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.openvideo.R

data class PlayerSettingsPrimaryItem(
    val page: SettingsPage,
    val titleRes: Int,
    val iconRes: Int
)

enum class SettingsPage {
    AUDIO,
    SUBTITLE,
    ASPECT,
    DISPLAY,
    PLAYLIST,
    STREAM,
    INFO,
    SHARE,
    CUT,
    BOOKMARK,
    TUTORIAL,
    MORE
}

class PlayerSettingsPrimaryGridBuilder(
    private val context: Context,
    private val onClick: (PlayerSettingsPrimaryItem) -> Unit
) {

    fun createPrimaryItemView(item: PlayerSettingsPrimaryItem): View {
        val cell = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = dp(74)
            isClickable = true
            isFocusable = true
            setPadding(1, 4, 1, 2)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(1), dp(2), dp(1), dp(2))
            }
            setOnClickListener { onClick(item) }
        }

        val selected = item.page == SettingsPage.AUDIO
        val iconWrap = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = context.getDrawable(
                if (selected) R.drawable.bg_player_settings_icon_selected else R.drawable.bg_player_settings_icon
            )
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        iconWrap.addView(ImageView(context).apply {
            setImageResource(item.iconRes)
            setColorFilter(context.getColor(if (selected) R.color.player_accent else android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
        })

        cell.addView(iconWrap)
        cell.addView(TextView(context).apply {
            text = context.getString(item.titleRes)
            gravity = Gravity.CENTER
            setTextColor(context.getColor(if (selected) R.color.player_accent else android.R.color.white))
            textSize = 12f
            maxLines = 1
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        })

        return cell
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
