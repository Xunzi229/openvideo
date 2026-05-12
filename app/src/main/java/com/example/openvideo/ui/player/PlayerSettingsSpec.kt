package com.example.openvideo.ui.player

data class PlayerSettingsActionSpec(
    val title: String,
    val value: String? = null,
    val onClick: () -> Unit
)

data class PlayerSettingsSwitchSpec(
    val title: String,
    val checked: Boolean,
    val onChanged: (Boolean) -> Unit
)
