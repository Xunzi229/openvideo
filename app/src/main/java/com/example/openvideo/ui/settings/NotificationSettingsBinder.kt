package com.example.openvideo.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import com.example.openvideo.R
import com.example.openvideo.core.player.PlaybackServiceIntents
import com.example.openvideo.core.prefs.PlayerPrefs
import com.google.android.material.materialswitch.MaterialSwitch

object NotificationSettingsBinder {
    fun bind(activity: Activity, root: View, playerPrefs: PlayerPrefs, onBack: () -> Unit) {
        root.findViewById<TextView>(R.id.tv_back_title).setText(R.string.settings_page_title)
        root.findViewById<View>(R.id.btn_back).setOnClickListener { onBack() }

        val swAllowSystem = root.findViewById<MaterialSwitch>(R.id.sw_allow_system_notifications)
        val tvAllowSystemSummary = root.findViewById<TextView>(R.id.tv_allow_system_summary)

        root.findViewById<View>(R.id.row_allow_system_notifications).setOnClickListener {
            openSystemNotificationSettings(activity)
        }

        val swBgNotification = root.findViewById<MaterialSwitch>(R.id.sw_bg_notification)
        swBgNotification.isChecked = playerPrefs.bgPlaybackNotificationEnabled
        swBgNotification.setOnCheckedChangeListener { _, checked ->
            playerPrefs.bgPlaybackNotificationEnabled = checked
            if (!checked) dismissExistingPlaybackNotification(activity)
        }
        root.findViewById<View>(R.id.row_bg_notification).setOnClickListener {
            swBgNotification.isChecked = !swBgNotification.isChecked
        }

        refreshSystemNotificationUi(activity, swAllowSystem, tvAllowSystemSummary)
    }

    fun refreshSystemNotificationUi(
        activity: Activity,
        swAllowSystem: MaterialSwitch,
        tvAllowSystemSummary: TextView
    ) {
        val enabled = NotificationManagerCompat.from(activity).areNotificationsEnabled()
        swAllowSystem.isChecked = enabled
        tvAllowSystemSummary.text = activity.getString(
            if (enabled) R.string.settings_notification_allow_system_on
            else R.string.settings_notification_allow_system_off
        )
    }

    private fun dismissExistingPlaybackNotification(activity: Activity) {
        runCatching { activity.startService(PlaybackServiceIntents.stop(activity)) }
        runCatching { activity.stopService(PlaybackServiceIntents.stop(activity)) }
    }

    private fun openSystemNotificationSettings(activity: Activity) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", activity.packageName, null)
            }
        }
        runCatching { activity.startActivity(intent) }
    }
}
