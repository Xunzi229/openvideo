# 设置备份：设置页 SAF 入口已开放

> **状态：导出 / 导入基础流程已开放**
> **开关：** `SettingsBackupUiPolicy.SETTINGS_EXPORT_ENTRY_VISIBLE = true`；`SettingsBackupUiPolicy.SETTINGS_IMPORT_ENTRY_VISIBLE = true`

---

## 1. 当前决策

- **0.3.1 / 0.3.2**（Schema + 白名单导出）保留在代码库，供调试与后续 Web 备份复用。
- **0.3.3**（SAF 导出 JSON 到 `openvideo-settings.json`）已在设置页开放。
- **0.3.4 MVP**（SAF 导入 JSON）已在设置页开放；坏 JSON / 不支持版本会 Toast 降级，不写入设置。
- Web 备份仍可作为后续体验升级方向，用于预览、冲突摘要和更友好的恢复流程。

---

## 2. 已实现、可复用（勿删）

| 模块 | 用途 |
|------|------|
| `SettingsBackupSchema` / `SettingsBackupJson` | 版本化 JSON 契约 |
| `SettingsBackupAllowlistPolicy` | 敏感字段拦截 |
| `SettingsBackupExporter` | `PlayerPrefs` + `AppPrefs` → JSON |
| `SettingsBackupImporter` | JSON Document → `PlayerPrefs` + `AppPrefs` |
| `SettingsBackupFileWriter` | SAF URI 写入 |
| `SettingsViewModel.writeSettingsExportTo` / `readAndImportSettings` | 串联导出 / 导入 |
| `SettingsFragment` + `exportSettingsLauncher` / `importSettingsLauncher` | 设置页 SAF 入口，由 `bindBackupSection` + UI Policy 控制可见性 |

如需暂时关闭入口：将对应 `SettingsBackupUiPolicy` 开关改回 `false`，并同步本文与 roadmap。

---

## 3. 后续开发（Web 备份）

| 切片 | 说明 |
|------|------|
| Web 备份 UX | 用 webdev 做配置备份/恢复界面，补充格式预览、变更摘要、冲突处理 |
| 0.3.4 导入预检增强 | 当前已有版本/坏 JSON 保护；后续补变更摘要与确认页 |
| 0.3.5 设置页入口抛光 | 当前为 SAF 文件选择器 MVP；后续可替换为 Web / Hybrid 流程 |

---

## 4. AI / 开发注意

- **不要**删除 Schema/Exporter/Importer 单测；调整入口开关时必须同步本文与 roadmap。
- 改备份格式时同步更新 `SettingsBackupSchema.SCHEMA_VERSION` 与 allowlist 测试。

### Changelog

| 日期 | 说明 |
|------|------|
| 2026-05-19 | 隐藏设置页备份区；备注后续 webdev Web 备份 |
| 2026-05-24 | 开放设置页 SAF 导出 / 导入 MVP；保留 Web 备份作为后续体验升级 |
