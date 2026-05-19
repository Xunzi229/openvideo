# 设置备份：暂缓用户入口，后续 Web 方案

> **状态：底层已实现，UI 暂隐藏**  
> **开关：** `SettingsBackupUiPolicy.SETTINGS_EXPORT_ENTRY_VISIBLE = false`

---

## 1. 当前决策

- **0.3.1 / 0.3.2**（Schema + 白名单导出）保留在代码库，供调试与后续 Web 备份复用。
- **0.3.3**（SAF 导出 JSON 到 `openvideo-settings.json`）已实现但**不在设置页展示**。
- 直接让用户导出裸 JSON 体验不佳；**后续改用 Web 侧备份方案**（webdev）再开放入口与完整导入导出流程。

---

## 2. 已实现、可复用（勿删）

| 模块 | 用途 |
|------|------|
| `SettingsBackupSchema` / `SettingsBackupJson` | 版本化 JSON 契约 |
| `SettingsBackupAllowlistPolicy` | 敏感字段拦截 |
| `SettingsBackupExporter` | `PlayerPrefs` + `AppPrefs` → JSON |
| `SettingsBackupFileWriter` | SAF URI 写入 |
| `SettingsViewModel.writeSettingsExportTo` | 串联导出 |
| `SettingsFragment` + `exportSettingsLauncher` | 入口代码保留，由 `bindBackupSection` + UI Policy 控制可见性 |

重新开放入口：将 `SettingsBackupUiPolicy.SETTINGS_EXPORT_ENTRY_VISIBLE` 改为 `true`，或 Web 方案就绪后替换为新的 Activity/Deep Link。

---

## 3. 后续开发（Web 备份）

| 切片 | 说明 |
|------|------|
| Web 备份 UX | 用 webdev 做配置备份/恢复界面（格式、预览、冲突处理优于裸 JSON 文件） |
| 0.3.4 导入预检 | 版本/坏 JSON/变更摘要（可与 Web 或 App 共用 `SettingsBackupSchema.decode`） |
| 0.3.5 设置页入口 | 挂 Web 流程或 Hybrid 入口，**不要**在未评审 UX 前重新暴露纯 JSON 导出 |

---

## 4. AI / 开发注意

- **不要**删除 Schema/Exporter 单测；**不要**在未更新本文前把 `SETTINGS_EXPORT_ENTRY_VISIBLE` 改回 `true`。
- 改备份格式时同步更新 `SettingsBackupSchema.SCHEMA_VERSION` 与 allowlist 测试。

### Changelog

| 日期 | 说明 |
|------|------|
| 2026-05-19 | 隐藏设置页备份区；备注后续 webdev Web 备份 |
