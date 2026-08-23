package com.example.openvideo.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.ui.LibraryNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSettingsFragment : Fragment() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.activity_notification_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NotificationSettingsBinder.bind(
            activity = requireActivity(),
            root = view,
            playerPrefs = playerPrefs,
            onBack = { LibraryNavigator.pop(this) }
        )
    }

    override fun onResume() {
        super.onResume()
        val root = view ?: return
        NotificationSettingsBinder.refreshSystemNotificationUi(
            requireActivity(),
            root.findViewById(R.id.sw_allow_system_notifications),
            root.findViewById(R.id.tv_allow_system_summary)
        )
    }
}
