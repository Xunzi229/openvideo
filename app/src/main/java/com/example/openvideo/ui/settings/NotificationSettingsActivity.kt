package com.example.openvideo.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSettingsActivity : AppCompatActivity() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)
        NotificationSettingsBinder.bind(
            activity = this,
            root = findViewById(android.R.id.content),
            playerPrefs = playerPrefs,
            onBack = { finish() }
        )
    }

    override fun onResume() {
        super.onResume()
        NotificationSettingsBinder.refreshSystemNotificationUi(
            this,
            findViewById(R.id.sw_allow_system_notifications),
            findViewById(R.id.tv_allow_system_summary)
        )
    }
}
