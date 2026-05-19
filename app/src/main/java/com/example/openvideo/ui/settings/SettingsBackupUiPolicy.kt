package com.example.openvideo.ui.settings

/**
 * 设置备份 UI 开关。底层 Schema/Exporter 已实现；用户可见入口暂隐藏，
 * 待 Web 侧备份方案就绪后再开放（见 design/rules/settings-backup-deferred.md）。
 */
object SettingsBackupUiPolicy {
    const val SETTINGS_EXPORT_ENTRY_VISIBLE = false
}
