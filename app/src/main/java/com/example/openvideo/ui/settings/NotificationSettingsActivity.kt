package com.example.openvideo.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.example.openvideo.R
import com.example.openvideo.core.player.PlaybackServiceIntents
import com.example.openvideo.core.prefs.PlayerPrefs
import com.google.android.material.materialswitch.MaterialSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSettingsActivity : AppCompatActivity() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    private lateinit var swAllowSystem: MaterialSwitch
    private lateinit var tvAllowSystemSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        findViewById<TextView>(R.id.tv_back_title).setText(R.string.settings_page_title)
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        swAllowSystem = findViewById(R.id.sw_allow_system_notifications)
        tvAllowSystemSummary = findViewById(R.id.tv_allow_system_summary)

        findViewById<View>(R.id.row_allow_system_notifications).setOnClickListener {
            openSystemNotificationSettings()
        }

        val swBgNotification = findViewById<MaterialSwitch>(R.id.sw_bg_notification)
        swBgNotification.isChecked = playerPrefs.bgPlaybackNotificationEnabled
        swBgNotification.setOnCheckedChangeListener { _, checked ->
            playerPrefs.bgPlaybackNotificationEnabled = checked
            if (!checked) dismissExistingPlaybackNotification()
        }
        findViewById<View>(R.id.row_bg_notification).setOnClickListener {
            swBgNotification.isChecked = !swBgNotification.isChecked
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSystemNotificationUi()
    }

    private fun refreshSystemNotificationUi() {
        val enabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        swAllowSystem.isChecked = enabled
        tvAllowSystemSummary.text = getString(
            if (enabled) R.string.settings_notification_allow_system_on
            else R.string.settings_notification_allow_system_off
        )
    }

    private fun dismissExistingPlaybackNotification() {
        runCatching { startService(PlaybackServiceIntents.stop(this)) }
        runCatching { stopService(PlaybackServiceIntents.stop(this)) }
    }

    private fun openSystemNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", packageName, null)
            }
        }
        runCatching { startActivity(intent) }
    }
}
