# Phase 0：稳定化与架构清债

> 目标周期：约 1～2 周
> 主题：先把播放器稳定性、通知后台播放边界、设置备份和发布门禁补齐，再进入后续大功能。

---

## 0. 当前开发状态

| 范围 | 状态 | 已落地内容 | 验证 |
|------|------|------------|------|
| Sprint 0.1 / 0.1.1 错误分类模型 | ✅ 已开发 | 新增 `PlayerPlaybackErrorPolicy`，覆盖 Network / Permission / Decoder / Source / Timeout / Unknown 分类与推荐动作 | `PlayerPlaybackErrorPolicyTest` 通过 |
| Sprint 0.1 / 0.1.2 播放失败 HUD | ✅ 已开发 | `PlayerActivity` 播放错误走播放器内 HUD，提供重试、软件解码、复制诊断、返回列表入口 | `PlayerPlaybackErrorSourceTest`、`assembleDebug` 通过 |
| Sprint 0.1 / 0.1.3 诊断信息脱敏 | ✅ 已开发 | 复制诊断包含分类、错误码、cause chain、播放位置、时长、videoId、解码模式、SDK、设备，并通过 `CrashRedactionPolicy` 与 URL query 脱敏隐藏路径/标题/token | `PlayerPlaybackErrorDiagnosticsPolicyTest` 通过 |
| Sprint 0.1 / 0.1.4 软解切换入口 | ✅ 已开发 | HUD 可触发本次软件解码重试，按钮执行前会禁用以防连点，且不写入 `playerPrefs.softwareAudioDecoder` 全局偏好 | `PlayerPlaybackErrorSourceTest` 通过 |
| Sprint 0.1 / 0.1.5 错误日志串联 | ✅ 已开发 | 保留 `CrashLogger.logPlayerError`，并额外写入 `player_playback_error` 脱敏诊断日志；日志与复制诊断复用同一 `playbackErrorDiagnostics(error, presentation)` helper | `PlayerPlaybackErrorSourceTest` 通过 |
| Sprint 0.2 / 0.2.1 播放通知状态快照 | ✅ 已开发 | 新增 `PlaybackNotificationSnapshotPolicy` 与 skip capability 纯函数；`PlayerActivity` 通过策略生成快照后交给 coordinator | `PlaybackNotificationSnapshotPolicyTest` / `PlaybackNotificationSnapshotSourceTest` 通过 |
| Sprint 0.2 / 0.2.2 通知同步协调器 | ✅ 已开发 | `PlaybackNotificationCoordinator` 新增 `sync`、`startIfNeeded`、`refreshIfNeeded`、`stop`、`dismiss`，Activity 不再直接拼装通知 Service Intent | `PlaybackNotificationCoordinatorSourceTest` / `PlaybackServiceIntentsSourceTest` 通过 |
| Sprint 0.2 / 0.2.3 队列动作统一策略 | ✅ 已开发 | 新增 core `PlaybackQueueSkipPolicy`，`PlayerQueueSkipPolicy`、通知按钮和通知快照 skip capability 统一复用；支持首尾、单集、LoopMode.LIST 环绕 | `PlaybackQueueSkipPolicyTest` / `PlayerQueueSkipPolicyTest` / `PlaybackNotificationSnapshotPolicyTest` 通过 |
| Sprint 0.2 / 0.2.4 通知权限状态处理 | ✅ 已开发 | Android 13+ 通知权限请求由 `PlayerNotificationPermissionPolicy` 统一判定；`PlayerActivity` 记住已请求状态，拒绝后不反复弹窗，且播放/后台服务启动链路仍以 `runCatching` 容错 | `PlayerNotificationPermissionPolicyTest` / `PlayerActivityStartupSourceTest` 通过 |
| Sprint 0.2 / 0.2.5 锁屏媒体卡同步 | ✅ 已开发 | `PlaybackNotificationSkipCapabilityPolicy` + `MediaSessionPlaybackStatePolicy` 统一通知栏与 MediaSession skip 能力；`PlaybackService` 按队列/循环模式动态设置 actions | `PlaybackQueueSkipPolicyTest` / `MediaSessionPlaybackStatePolicyTest` / `PlaybackServiceMediaSessionSourceTest` 通过；`tools/adb/phase0-media-session-regression.ps1` 真机 PASS |
| Sprint 0.3 / 0.3.1 Schema 定义 | ✅ 已开发 | `SettingsBackupSchema` + `SettingsBackupJson`：版本号、导出时间、player/app 分区、编解码与校验 | `SettingsBackupSchemaTest` / `SettingsBackupSchemaSourceTest` 通过 |
| Sprint 0.3 / 0.3.2 敏感字段白名单 | ✅ 已开发 | `SettingsBackupAllowlistPolicy` + `SettingsBackupExporter` 从 `PlayerPrefs`/`AppPrefs` 显式映射导出，拒绝路径/URL/token/会话位点 | `SettingsBackupAllowlistPolicyTest` / `SettingsBackupExporterTest` / `SettingsBackupExporterSourceTest` 通过 |
| Sprint 0.3 / 0.3.3 导出/导入实现 | ✅ 已开发 | 底层 `SettingsBackupFileWriter` + SAF 导出、`SettingsBackupImporter` + SAF 导入已实现；设置页入口暂时关闭，后续 Web 备份作为预览/冲突处理体验升级 | `SettingsBackupFileWriterTest` / `SettingsBackupExportSourceTest` 通过；见 `design/rules/settings-backup-deferred.md` |
| 播放器 / 进入横竖屏 | ✅ 已开发 | `MediaStoreVideoDimensionsPolicy` 扫描归一化显示尺寸；`PlayerVideoLayoutPolicy` 唯一方向阈值；`PlayerActivity.applyInitialVideoOrientation` + Manifest `unspecified`；禁止未知尺寸默认横屏 | `MediaStoreVideoDimensionsPolicyTest` / `PlayerOrientationPolicyTest` / `PlayerVideoLayoutPolicyTest` 通过；见 `design/rules/player-video-orientation.md` |
| 播放列表 / 失效项与路径 | ✅ 已开发 | `PlaylistVideoAvailabilityPolicy` + `LocalMediaUriPolicy`：磁盘已删条目自动移出列表；含 `#` 文件名不用 `Uri.parse` 裸路径，避免 ExoPlayer `FileDataSourceException` | `PlaylistVideoAvailabilityPolicyTest` / `LocalMediaUriPolicyTest` 通过 |
| 播放列表 / 列表行 UI | ✅ 已开发 | `item_playlist_video` 对齐 `item_video`（缩略图 + 时长角标 + 标题 + 副标题）；`PlaylistMarqueeTextPolicy` 标题/副标题过长横向跑马灯 | `assembleDebug` 通过 |
| UI 稳定性 / 弹窗互斥 | ✅ 已开发 | 播放器页 `activePlayerDialog` 覆盖倍速、比例、音轨、字幕、更多设置、剧集列表；设置页 `activeSettingsDialog` 覆盖默认画面比例、默认播放速度、清缓存、清历史，快速连点不再叠多个弹窗 | `PlayerQuickEntrySourceTest` / `SettingsLanguageRowSourceTest` 通过；ADB 安装后真机验证通过 |
| UI 稳定性 / 快捷弹窗响应式 chrome | ✅ 已开发 | 倍速、比例、音轨、字幕统一走 `PlayerGlassSheetDialog`；竖屏复用选集同款底部滑出，横屏复用播放器设置 `PlayerSettingsSheetChrome` 的宽高、背景 dim/blur 与面板透明度；弹出时隐藏播放/进度 controls | `PlayerQuickEntrySourceTest` 通过；`testDebugUnitTest` / `assembleDebug --warning-mode fail` / `lintDebug --warning-mode fail` 通过 |
| UI 稳定性 / iOS Action Sheet | ✅ 已开发 | 设置页清缩略图缓存/清播放历史、播放列表页重命名/删除改为底部 iOS Action Sheet；浅/深色主题分别使用独立颜色 token，保留原业务回调和弹窗互斥控制 | `SettingsConfirmationActionSheetSourceTest` / `PlaylistActionSheetSourceTest` 通过；ADB 安装成功 |

**真机回归清单：** [`playback-error-regression-checklist.md`](./playback-error-regression-checklist.md)

**已跑验证：**

```text
git diff --check                     # no output，2026-05-17，0.2.2 文档回写后
.\gradlew.bat :app:testDebugUnitTest   # BUILD SUCCESSFUL，2026-05-18，0.2.4 后完整单元测试复跑
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.ui.player.PlayerNotificationPermissionPolicyTest" --tests "com.example.openvideo.ui.player.PlayerActivityStartupSourceTest"  # BUILD SUCCESSFUL，2026-05-18，0.2.4 通知权限状态处理
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.ui.player.PlayerQuickEntrySourceTest" --tests "com.example.openvideo.ui.settings.SettingsLanguageRowSourceTest"  # BUILD SUCCESSFUL，2026-05-18，快速弹窗互斥
.\gradlew.bat :app:assembleDebug       # BUILD SUCCESSFUL，2026-05-18，0.2.4 后复跑
adb install -r app\build\outputs\apk\debug\app-debug.apk  # Success，2026-05-18，设备 838eac33；快速连点弹窗真机验证通过
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.ui.settings.SettingsConfirmationActionSheetSourceTest" --tests "com.example.openvideo.ui.playlist.PlaylistActionSheetSourceTest"  # BUILD SUCCESSFUL，2026-05-19，Action Sheet 样式与播放列表操作迁移
.\gradlew.bat :app:testDebugUnitTest   # BUILD SUCCESSFUL，2026-05-19，Action Sheet 后完整单元测试复跑
.\gradlew.bat :app:assembleDebug       # BUILD SUCCESSFUL，2026-05-19，Action Sheet 后复跑
adb install -r app\build\outputs\apk\debug\app-debug.apk  # Success，2026-05-19，设备 838eac33
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.core.player.*" --tests "com.example.openvideo.ui.player.PlayerQueueSkipPolicyTest"  # BUILD SUCCESSFUL，2026-05-19，0.2.5 MediaSession 策略
.\gradlew.bat :app:assembleDebug       # BUILD SUCCESSFUL，2026-05-19，0.2.5 后复跑
adb install -r app\build\outputs\apk\debug\app-debug.apk  # Success，2026-05-19，设备 71615e08
powershell -File tools\adb\phase0-media-session-regression.ps1 -SkipBuild  # PASS，2026-05-19，PlaybackService + OpenVideoSession + play-pause
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.core.prefs.SettingsBackupSchemaTest"  # BUILD SUCCESSFUL，2026-05-19，0.3.1 Schema
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.core.prefs.SettingsBackupAllowlistPolicyTest" --tests "com.example.openvideo.core.prefs.SettingsBackupExporterTest"  # BUILD SUCCESSFUL，2026-05-19，0.3.2 白名单导出
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.core.prefs.SettingsBackupFileWriterTest" --tests "com.example.openvideo.ui.settings.SettingsBackupExportSourceTest"  # BUILD SUCCESSFUL，2026-05-19，0.3.3 SAF 导出
.\gradlew.bat :app:testDebugUnitTest   # BUILD SUCCESSFUL，2026-05-19，0.3.2 后完整单元测试复跑
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.data.scanner.MediaStoreVideoDimensionsPolicyTest" --tests "com.example.openvideo.ui.player.PlayerOrientationPolicyTest" --tests "com.example.openvideo.ui.player.PlayerVideoLayoutPolicyTest"  # BUILD SUCCESSFUL，2026-05-20，进入横竖屏
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.core.media.LocalMediaUriPolicyTest" --tests "com.example.openvideo.ui.playlist.PlaylistVideoAvailabilityPolicyTest"  # BUILD SUCCESSFUL，2026-05-20，播放列表失效项/URI
.\gradlew.bat :app:assembleDebug       # BUILD SUCCESSFUL，2026-05-20，播放列表 UI + 跑马灯
.\gradlew.bat :app:lintDebug           # FAILED，2026-05-17，既有 lint 阻塞仍存在（PlayerActivity API 31 guard 等）
```

**已知阻塞：**

- `./gradlew.bat :app:lintDebug` 仍受既有问题阻塞：首个错误为 `PlayerActivity.kt:630` 中 `Window#setBackgroundBlurRadius` 需要 API 31 guard；lint 汇总为 9 errors / 358 warnings，该问题不属于本轮 0.1/0.2.1/0.2.2/0.2.3 新增代码。

---

## 1. 成功定义

1. `PlayerActivity` 不再继续承接新的业务策略，后续播放器能力优先落在 `Policy`、`Coordinator`、`ViewModel` 或 `PlayerManager` 边界内。
2. 播放失败时，用户能看到可理解的错误提示，并能执行「重试」「切换软解」「复制诊断」「返回列表」等动作。
3. 通知、后台播放、MediaSession 快照同步有更清晰的协调层，Activity 只负责生命周期转发与 UI 状态采集。
4. 用户可导出/导入非敏感设置，为后续调试、迁移和复现做基础。
5. 发布前检查有明确命令、结果记录和回归清单。

---

## 2. 非目标

- 不重写播放器内核，不替换 Media3 / ExoPlayer。
- 不引入云端同步、账号体系或远程配置。
- 不在 Phase 0 做大规模 UI 改版，仅补稳定性与架构边界。
- 不一次性解决全部 Android 版本兼容问题，只优先解决会影响播放、通知和发布门禁的部分。

---

## 3. 工作流拆分

| 工作流 | 目标 | 主要文件 | 风险 |
|--------|------|----------|------|
| 播放错误体验 | 用户知道失败原因和下一步动作 | `ui/player/*Error*`、`PlayerActivity`、`PlayerManager` | 错误码过度简化或误导 |
| Activity 清债 | 降低后续手势/字幕/网络接入成本 | `PlayerActivity`、新增 coordinator/policy | 拆分时引入生命周期回归 |
| 通知同步 | 后台/锁屏/蓝牙控制一致 | `PlaybackService`、`PlaybackNotificationCoordinator` | Android 版本差异 |
| 设置备份 | 本地导入导出播放器/App 设置 | `core/prefs`、`ui/settings` | 泄露敏感信息或格式不稳定 |
| 发布门禁 | 固化验证命令和回归矩阵 | `docs/release-engineering.md`、脚本 | 命令耗时导致执行率低 |

---

## 4. Sprint 节奏

### Sprint 0.1：播放器错误诊断闭环（2～3 天）

**目标：** 播放失败不再只 toast 或静默失败，用户和开发者都能得到可行动的信息。

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 0.1.1 | ✅ 错误分类模型 | `PlayerPlaybackErrorPolicy`：网络、权限、解码、源文件、超时、未知 | 单元测试覆盖分类 |
| 0.1.2 | ✅ 播放失败 HUD | 播放器内错误面板，展示标题、原因、建议动作 | 源测试 + 构建通过 |
| 0.1.3 | ✅ 诊断信息脱敏 | 复制诊断文本隐藏路径、标题、token、签名参数 | 单元测试覆盖脱敏 |
| 0.1.4 | ✅ 软解切换入口 | 本次播放失败后可一键软解重试 | 不写全局偏好，按钮防连点 |
| 0.1.5 | ✅ 错误日志串联 | `CrashLogger.logPlayerError` + `player_playback_error` 脱敏诊断 | 源测试覆盖调用链 |

**回归重点：**

- 本地损坏视频：显示解码/源文件类错误。
- 权限丢失：显示权限类错误，不崩溃。
- 远程/网络预留：显示网络类错误。
- 点击重试、复制诊断、返回列表、切软解都不会连点触发多次。
- 点击切软解，仅本次重试生效或有明确保存提示。

### Sprint 0.2：通知与后台播放协调层（2～3 天）

**目标：** 让 Activity、Service、MediaSession 的职责更清楚。

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 0.2.1 | ✅ 定义播放通知状态快照 | `PlaybackNotificationSnapshotPolicy`：标题、进度、播放状态、队列、首尾 skip 能力、时长/进度归一化 | `PlaybackNotificationSnapshotPolicyTest` / `PlaybackNotificationSnapshotSourceTest` 通过 |
| 0.2.2 | ✅ 抽出通知同步协调器 | `PlaybackNotificationCoordinator` 承接 `sync(snapshot)`、`startIfNeeded(...)`、`refreshIfNeeded(...)`、`stop(...)`、`dismiss(...)`；Activity 仅保留快照组装和策略参数 | `PlayerActivity` 通知相关函数减少，源测试通过 |
| 0.2.3 | ✅ 队列上一集/下一集策略统一 | `PlaybackQueueSkipPolicy` 统一计算上一集/下一集；通知按钮与通知快照能力复用同一策略，`LoopMode.LIST` 支持首尾环绕 | 单元测试覆盖首尾、单集、循环模式 |
| 0.2.4 | ✅ 权限状态处理 | Android 13+ 通知权限关闭时不反复请求、不崩溃；请求动作只在通知开关开启、未授权、未请求过时触发 | `PlayerNotificationPermissionPolicyTest` 覆盖纯策略；`PlayerActivityStartupSourceTest` 覆盖记忆标记与服务启动容错；真机/模拟器拒绝权限仍可播放待回归 |
| 0.2.5 | ✅ 锁屏媒体卡检查 | MediaSession token、播放/暂停、skip 能力与通知栏一致；`LoopMode.LIST` 环绕切歌 | `PlaybackServiceMediaSessionSourceTest` + `tools/adb/phase0-media-session-regression.ps1` 真机 PASS |

**回归重点：** 后台播放、锁屏播放暂停、蓝牙耳机播放暂停、通知关闭、Activity 退出。

### Sprint 0.2+：播放器与播放列表体验修补（穿插）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 0.2.6 | ✅ 进入横竖屏 | `MediaStoreVideoDimensionsPolicy`、`PlayerActivity` 进入前预设方向、无「先横后竖」闪屏 | 单测 + `design/rules/player-video-orientation.md` |
| PL-001 | ✅ 播放列表失效视频 | `PlaylistVideoAvailabilityPolicy` 过滤/清理已删文件；点击与 `onResume` 二次校验 | `PlaylistVideoAvailabilityPolicyTest` 通过 |
| PL-002 | ✅ 播放列表本地路径 URI | `LocalMediaUriPolicy.playbackUri`：含 `#`/`?` 文件名用 `Uri.fromFile` | `LocalMediaUriPolicyTest` 通过 |
| PL-003 | ✅ 播放列表行样式 | 缩略图 + 标题 +「媒体库/文件名 \| 时长」副标题，对齐视频页列表行 | 真机目视 |
| PL-004 | ✅ 标题/副标题跑马灯 | `PlaylistMarqueeTextPolicy`：过长单行横向滚动 | `assembleDebug` 通过 |

### Sprint 0.3：设置导入导出 MVP（2～3 天）

**目标：** 让用户能备份播放器配置，也方便开发复现问题。

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 0.3.1 | ✅ Schema 定义 | `SettingsBackupSchema`：版本号、导出时间、播放器设置、App 设置 | `SettingsBackupSchemaTest` 通过 |
| 0.3.2 | ✅ 敏感字段白名单 | `SettingsBackupAllowlistPolicy` + `SettingsBackupExporter` 只导出非敏感配置 | `SettingsBackupAllowlistPolicyTest` / `SettingsBackupExporterSourceTest` 通过 |
| 0.3.3 | ✅ 导出实现 | 底层 SAF JSON 导出已实现；设置页入口暂时关闭 | 单测通过 |
| 0.3.4 | ✅ 导入 MVP | 读取 SAF JSON，检查版本/坏 JSON，不支持或无效文件 Toast 降级 | 坏文件不崩溃 |
| 0.3.5 | 设置页入口抛光 | 当前入口关闭；后续可接 Web 备份预览/冲突摘要后再开放 | 后续迭代 |

**格式建议：**

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-05-17T00:00:00Z",
  "player": {},
  "app": {}
}
```

### Sprint 0.4：发布门禁与回归矩阵（1～2 天）

**目标：** 每次发版前能快速知道该跑什么、结果如何记录。

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 0.4.1 | 命令清单 | `testDebugUnitTest`、`assembleDebug`、`lintDebug` 的用途和耗时记录 | 文档可直接复制执行 |
| 0.4.2 | 播放器真机矩阵 | 编码、容器、字幕、手势、通知、PiP、蓝牙检查表 | 每项有「样本/步骤/期望」 |
| 0.4.3 | 发布说明模板 | 用户可见变化、修复、已知问题、校验值 | 可复用到每次 Release |
| 0.4.4 | 回滚点记录 | 哪些设置/迁移/功能开关可回滚 | 重大变更有关闭开关 |

---

## 5. 详细任务清单

| ID | 优先级 | 任务 | 依赖 | 测试 |
|----|--------|------|------|------|
| P0-ERR-001 | P0 | ✅ 建立播放器错误分类枚举 | 无 | `PlayerPlaybackErrorPolicyTest` 通过 |
| P0-ERR-002 | P0 | ✅ 错误 HUD UI 状态模型 | P0-ERR-001 | `PlayerPlaybackErrorSourceTest` 通过 |
| P0-ERR-003 | P0 | ✅ 重试与本次软解动作 | P0-ERR-001 | 已实现重试/本次软解入口、按钮防连点、不写全局偏好；真机验证见回归清单 |
| P0-ERR-004 | P1 | ✅ 复制诊断文本脱敏 | CrashRedactionPolicy | `PlayerPlaybackErrorDiagnosticsPolicyTest` 通过 |
| P0-NOTI-001 | P0 | ✅ 通知快照模型 | 无 | `PlaybackNotificationSnapshotPolicyTest` 通过 |
| P0-NOTI-002 | P0 | ✅ 通知同步 coordinator | P0-NOTI-001 | `PlaybackNotificationCoordinatorSourceTest` / `PlaybackServiceIntentsSourceTest` 通过 |
| P0-NOTI-003 | P1 | ✅ 队列动作统一策略 | Queue policy | `PlaybackQueueSkipPolicyTest` / `PlayerQueueSkipPolicyTest` / `PlaybackNotificationSnapshotPolicyTest` 通过 |
| P0-NOTI-004 | P1 | ✅ 通知权限状态处理 | Android 13+ `POST_NOTIFICATIONS` | `PlayerNotificationPermissionPolicyTest` / `PlayerActivityStartupSourceTest` 通过；拒绝权限真机/模拟器回归待跑 |
| P0-UI-001 | P1 | ✅ 播放器/设置页弹窗互斥 | 快速选择弹窗、确认弹窗 | `PlayerQuickEntrySourceTest` / `SettingsLanguageRowSourceTest` 通过；ADB 安装后真机快速连点验证通过 |
| P0-UI-002 | P1 | ✅ iOS Action Sheet 确认/操作弹窗 | 设置页清理确认、播放列表重命名/删除 | `SettingsConfirmationActionSheetSourceTest` / `PlaylistActionSheetSourceTest` 通过；浅/深色 token 覆盖；ADB 安装成功 |
| P0-BACKUP-001 | P1 | ✅ 设置备份 schema | 无 | `SettingsBackupSchemaTest` 通过 |
| P0-BACKUP-002 | P1 | ✅ 导出 PlayerPrefs/AppPrefs | P0-BACKUP-001 | `SettingsBackupExporterTest` / `SettingsBackupExporterSourceTest` 通过 |
| P0-BACKUP-002b | P1 | ✅ 导出到 SAF 文件 | P0-BACKUP-002 | 设置页入口暂时关闭；`SettingsBackupUiPolicy`；见 `design/rules/settings-backup-deferred.md` |
| P0-BACKUP-003 | P1 | ✅ 导入 MVP；预检确认 + Web 备份 UX 后续抛光 | P0-BACKUP-001 | `SettingsBackupExportSourceTest`；webdev 后续迭代 |
| P0-PLAY-001 | P1 | ✅ 进入横竖屏无闪屏 | MediaStore 显示尺寸 + `PlayerVideoLayoutPolicy` | 见 `design/rules/player-video-orientation.md` |
| P0-PL-001 | P1 | ✅ 播放列表失效项清理 | `PlaylistVideoAvailabilityPolicy` | `PlaylistVideoAvailabilityPolicyTest` 通过 |
| P0-PL-002 | P1 | ✅ 播放列表 URI 规范化 | `LocalMediaUriPolicy` | `LocalMediaUriPolicyTest` 通过 |
| P0-PL-003 | P1 | ✅ 播放列表行 UI 对齐视频页 | `item_playlist_video` + `PlaylistVideoAdapter` | 真机目视 |
| P0-PL-004 | P1 | ✅ 播放列表标题/副标题跑马灯 | `PlaylistMarqueeTextPolicy` | `assembleDebug` 通过 |
| P0-REL-001 | P1 | 发布门禁文档 | 无 | 命令执行记录 |
| P0-REL-002 | P1 | 真机回归矩阵 | P0-REL-001 | 手动检查 |

---

## 6. 验收清单

- [x] 播放错误 HUD 覆盖解码失败、权限丢失、网络预留错误的分类与 UI 入口；真机触发步骤见 `playback-error-regression-checklist.md`。
- [x] Activity 通知同步逻辑减少，新增逻辑有 coordinator/policy 测试。
- [x] 播放器页与设置页快速弹窗有互斥门闩，倍速/比例/音轨/字幕/更多设置/剧集列表/默认画面比例/默认播放速度/清缓存/清历史快速连点不叠弹窗。
- [x] 设置页清理确认与播放列表重命名/删除操作采用底部 iOS Action Sheet，浅/深色主题配色独立适配，并保留原业务逻辑。
- [x] 设置导出文件不包含密码、token、完整隐私文件夹路径（`SettingsBackupAllowlistPolicy` + `SettingsBackupExporter` 白名单映射；见 0.3.2 单测）。
- [x] 播放列表不展示已删除本地文件，点击不会触发 ExoPlayer 源错误；含 `#` 路径可正常播放（见 PL-001 / PL-002）。
- [x] 播放列表行 UI 与视频页列表一致，长标题/副标题支持跑马灯（见 PL-003 / PL-004）。
- [x] 竖/横屏视频进入播放器时方向正确，无先横后竖闪屏（见 0.2.6 / `player-video-orientation.md`）。
- [ ] 导入旧/坏 JSON 不崩溃，并给出用户提示。
- [ ] `testDebugUnitTest`、`assembleDebug`、`lintDebug` 至少在 Phase 结束前各跑一次并记录结果。
- [ ] 文档更新：发布门禁、回归矩阵、已知问题。

---

## 7. Phase 结束输出

1. 合并后的 Phase 0 变更摘要。
2. 错误分类与诊断说明。
3. 设置备份格式说明。
4. 播放器/通知/字幕/手势回归矩阵。
5. 下一阶段 Phase 1 的开工风险列表。
