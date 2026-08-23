# 未实现功能：先隐藏，预留后续开发

**日期：** 2026-08-23  
**原则：** 用户能点到的入口必须可用。只有占位、Toast「敬请期待 / 暂未接入」、或没有真实实现的功能，默认不出现在 UI 里。代码和接口留下，等条件成熟再打开。

开关集中在 `DeferredFeaturePolicy`。设置备份另有 `SettingsBackupUiPolicy`，行为相同，不重复造开关。

---

## 怎么重新打开

1. 把对应常量改成 `true`（或备份开关改成 `true`）。
2. 补齐真实实现，不要只把入口露出来继续弹「暂未接入」。
3. 同步改本文状态，并补源码测试。

不要删保留的接口、模型和单测。

---

## 当前隐藏项

| 开关 | 入口 | 现状 | 后续怎么做 |
|------|------|------|------------|
| `ONLINE_SUBTITLE_SEARCH_VISIBLE` | 播放器字幕设置「在线字幕搜索」 | 只有隐私提示，点确定后 HUD「暂未接入」。`OnlineSubtitleClient` 无实现，未接 OpenSubtitles | 接 OpenSubtitles.com 手动搜索。必须先隐私确认；禁止打开视频时自动搜；默认不上传文件 hash |
| `SOURCE_FUTURE_ADAPTERS_VISIBLE` | 来源页「SMB、Jellyfin、Plex」 | 只有 Toast「后续」，没有适配器 | 按来源单独做，不要一次摊开三个 |
| `PLAYER_CAST_VISIBLE` | 横屏播放器投屏按钮 | 点击只 Toast「投屏（敬请期待）」 | 投屏协议未选型。打开前不要改 `player_controls.xml` 控件结构，只改 `PlayerControlsBinder` 可见性 |
| `SettingsBackupUiPolicy` 导出/导入 | 设置页备份区 | 底层 JSON 导出导入已实现，入口关闭 | Web 备份 UX 或确认页做好后再打开。见 [settings-backup-deferred.md](./settings-backup-deferred.md) |

---

## 已实现、不要当占位藏掉

- **WebDAV**：可添加、探测、浏览、播放。已从来源页「规划中」挪到可用区，去掉「规划中」角标。
- **打开 URL / 本地库 / 字幕加载与样式 / 双字幕**：已接通，保持可见。

---

## 还没做、也还没有用户入口

这些目前没有可点的假按钮。需要时再开入口：

- OpenSubtitles 真请求、账号、配额
- SMB / NAS 浏览
- DLNA / UPnP 投屏或发现
- Jellyfin / Plex 媒体库
- ASS/SSA 高级特效
- 设置 Web 备份界面

---

## 代码落点

| 功能 | 保留位置 | 隐藏位置 |
|------|----------|----------|
| 在线字幕搜索 | `OnlineSubtitleClient`、`OnlineSubtitleModels`、隐私文案 | `activity_player_subtitle_settings.xml`、`PlayerSubtitleSettingsSheet`、`PlayerSubtitleSettingsActivity` |
| SMB/Jellyfin/Plex | `fragment_sources.xml` 的 `row_source_future`、`sources_planned_section` | `SourcesFragment` 按开关控制可见性 |
| 投屏 | `layout-land/player_controls.xml` 的 `btn_land_cast`（布局不删） | `PlayerControlsBinder` |
| 设置备份 | Schema / Exporter / Importer | `SettingsFragment.bindBackupSection` |

---

## Changelog

| 日期 | 说明 |
|------|------|
| 2026-08-23 | 未实现入口默认隐藏；WebDAV 保留在可用来源 |
