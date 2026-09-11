# P5 Player Core Slimming Continuation

Updated: 2026-05-22.

This note is the handoff point for continuing P5 work. The ignored local roadmap at
`docs/roadmap/player-optimization-roadmap.md` has also been updated, but this file is intended
to be tracked by Git.

**Cross-module 「已实现索引」：** [`feature-implementation-summary.md`](../feature-implementation-summary.md)（Home 筛选 Popover、玻璃弹窗、P6-3 文案等文件名速查）。

## Current P5 Baseline

Completed:

- Gesture HUD, double-tap seek, horizontal/vertical seek preview, long-press speed, lock gesture,
  gesture presets, and brightness/volume adjustment policies.
- Control chrome visibility, PiP chrome hiding, first-frame scrim, subtitle presentation,
  progress-save throttling, playback tick seek priority, AB loop, queue end behavior, video-switch
  session reset, lifecycle pause/resume, background playback service start, and exit release policy.
- Video layout/orientation decisions are now extracted through `PlayerVideoLayoutPolicy`, including
  pixel aspect ratio and rotation metadata handling.
- PiP entry decisions are now extracted through `PlayerPipPolicy`.
- Play/pause icon state decisions are now extracted through `PlayerPlayPausePolicy`.
- Exit presentation timing and transition strategy are now policy-driven through `PlayerExitPolicy`.
- Subtitle load routing is now partially extracted through `PlayerSubtitleLoadPolicy`, so explicit
  subtitle URI vs. local sidecar selection no longer lives directly in `PlayerActivity`.
- Brightness/volume vertical adjustment now uses an effective travel ratio of 50% of screen height,
  so a half-screen vertical drag can cover 0-100%.
- Media info loading in settings is now asynchronous and cached to avoid visible jank on first open.
- Returning to the player foreground now re-applies display-only state through
  `PlayerActivity.applyDisplaySettings()`, covering aspect/stretch/rotation/mirror drift without
  re-running the full playback parameter pipeline.
- P6-1 quick entry work is complete: portrait subtitle/audio controls now open dedicated quick
  selection dialogs backed by `PlayerQuickEntryPolicy`.
- P6-2 local subtitle auto-matching is complete: same-folder subtitle candidates are ranked by
  exact basename first, then language suffix matches.
- P6-3 is now complete (2026-05-22): subtitle quick entry supports `-500 ms`, `+500 ms`, and reset;
  live style preview covers size, color, background, and position;「更多字幕设置」opens
  `PlayerSubtitleSettingsSheet` (not the player settings grid); player settings subtitle page uses
  four solid color swatches; sheet chrome matches `PlayerSettingsDialog` via `PlayerSettingsSheetChrome`;
  overlay open/close clears and restores player chrome; switching videos dismisses the subtitle sheet.
  See [`播放器字幕设置.md`](../播放器字幕设置.md).
- P6-4 is now complete for the intended slice: audio track and runtime decoder/input diagnostics are
  formatted through `PlayerAudioDiagnosticsPolicy`, and both info-page rows and quick audio labels
  now expose codec, language, channels, sample rate, bitrate, and fallback state more clearly.
- Player quick choice chrome is now responsive (2026-05-24): aspect ratio, speed, audio, and
  subtitle quick dialogs all use `PlayerGlassSheetDialog`; portrait uses the episode-list-style
  bottom slide-up chrome, while landscape reuses `PlayerSettingsSheetChrome` for the settings-panel
  width, height, backdrop dim/blur, and panel opacity. Quick dialogs hide player controls while open.
- P7-1 has started: per-video playback history now also persists and restores `speed` and
  `aspectRatioKey` through Room history, repository wiring, `PlayerViewModel`, and player startup.
- P7-1 has now advanced to a second slice: per-video history also persists and restores external
  subtitle URI, subtitle enabled state, audio mute state, and selected audio track indices; startup
  and in-session video switching both wait for restored playback memory before re-applying settings
  and subtitle loading.
- P7-2 core continue-watching is in place: history shows progress/last watched/duration/missing-file
  state, and Home `RECENT` reuses continue-watching badges with unavailable items dimmed and disabled.
- P7-3 first strategy slice is in place: playback-ended decisions now go through
  `PlayerPlaybackEndPolicy`, covering next item, replay current, stop at end, and a modeled
  return-to-list action; AB loop replay wins before queue advance.
- P7-4 first folder-queue slice is in place: `PlayerEpisodeOrderingPolicy` recognizes common
  episode names (`S01E02`, `1x02`, `EP02`, `E02`, and `第02集`), and `FolderVideosFragment`
  now sends an episode-ordered same-folder queue to the player. The local continue-playback FAB
  also scopes its player queue to the current video's folder before applying episode ordering.
- P7-2 polish slice (2026-05-16): continue-watching labels are localized through
  `HistoryContinueWatchingLabels`; stale history rows are pruned after published scans through
  `HistoryCleanupPolicy` + `VideoRepository.pruneStaleHistory()`.
- P7-3 selector slice (2026-05-16): `PlaybackEndBehavior` preference and settings UI now expose
  follow / play next / replay / stop / return-to-list end behavior.
- P7-4 playlist slice (2026-05-16): `orderQueueIfEligible()` applies episode ordering to playlist
  detail playback when the queue is same-folder or mostly episode-numbered; mixed playlists keep
  manual order.
- P8-1 MediaStore refresh (2026-05-16): observer debounce, `VideoScanOutcome`, permission/scan-error
  UI, incremental diff refresh, and first-scan progress UI on Home/Local (`VideoScanOutcome.Progress`).
- P8-2 folder polish (2026-05-16): pinned folders, hide empty folder chips/lists, and folder counts
  tied to the active category/search surface.
- P8-2 folder sorting polish (2026-05-24): pinned folders still stay first, and Home/Local folder
  surfaces now sort each pinned/unpinned group by video count descending before name/key tie-breaks.
- P8 category count polish (2026-05-24): Home category chips now show filtered All/Recent/Favorites
  counts, so Favorites and Recent availability is visible from the category row.
- P8-3 search/filters (2026-05-16): path-aware search plus optional duration/format/date filters on
  Home via `MediaLibrarySearchPolicy`.
- Scanner robustness pass (2026-05-16): `VideoRepository.shareIn` prevents duplicate scans across
  Home/Local; progress events use `channel.send`; delete paths invalidate the scanner cache; queries
  use the active `videoCollectionUri()`; `_ID IN (...)` is batched (200/page) for large libraries.
- Player pre-orientation hook (2026-05-16): `PlayerActivity.preApplyOrientationForItem()` uses the
  MediaStore-cached `VideoItem.width/height` to set Activity orientation **before** ExoPlayer decode,
  eliminating the landscape→portrait flicker when continuously playing portrait clips after a
  landscape one. `applyVideoOrientation` now also ignores invalid (0×0) sizes.
- 手动方向锁定 (2026-05-17): 用户点击全屏按钮手动切换方向后，本视频会话内禁止 `onVideoSizeChanged`
  等回调再次依据视频宽高把方向覆盖回去；切换到下一首视频时自动复位，恢复"按视频宽高自动方向"。
  修复"切到横屏后过一会被自动转回竖屏"的体验问题。
- 文件夹播放队列与列表顺序对齐 (2026-05-17): `PlayerEpisodeOrderingPolicy.shouldOrderQueue` 不再因
  「同文件夹」就一律按字典序排，只在识别到剧集编号（`S01E02` / `第3集` / `EP04` 等）时才介入，
  其他场景保留 `FolderVideosFragment` / `LocalFolderFragment` / `PlaylistDetailFragment` 传入的
  展示顺序。修复"本地文件夹播放时队列顺序与文件夹列表显示顺序不一致"的问题。
- 横屏右侧浮动栏整理 + 快速对话框样式统一 (2026-05-17，2026-05-24 更新):
  - 删除横屏右侧浮动栏中实际行为是"打开播放器设置"的重复按钮，避免与右上角设置入口重复。
  - 同位置恢复宽高比图标按钮，绑定真正的「宽高比快速选择」对话框（适应屏幕 / 填充屏幕 /
    16:9 / 4:3 / 裁切 / 拉伸），选中后即时生效。
  - 当前实现已迁移为 `PlayerGlassSheetDialog` + `PlayerGlassSheetChrome`：竖屏快捷项使用
    `PLAYER_BOTTOM` 底部滑出；横屏快捷项使用 `PLAYER_SETTINGS_PANEL`，通过
    `PlayerSettingsSheetChrome` 复用播放器设置面板的宽高、背景暗化/模糊和面板透明度。
  - 倍速、宽高比、音轨、字幕快捷弹窗打开前会隐藏播放/暂停/进度条等 controls，只保留设置项。
- 快速弹窗互斥门闩 (2026-05-18):
  - `PlayerActivity.activePlayerDialog` 统一覆盖播放器页倍速、比例、音轨、字幕、更多设置、剧集列表，
    避免竖屏/横屏快速连点叠多个弹窗。
  - `SettingsFragment.activeSettingsDialog` 覆盖设置页默认画面比例、默认播放速度、清缓存、清历史，
    修复"我的/设置"页默认项快速点击出现多个相同弹窗的问题。
  - `PlayerQuickEntrySourceTest` / `SettingsLanguageRowSourceTest` 覆盖入口约束；2026-05-18 ADB
    安装到设备 `838eac33` 后，用户真机验证通过。
- iOS Action Sheet 确认/操作弹窗 (2026-05-19):
  - `SettingsConfirmationActionSheet` 替换设置页清除缩略图缓存、清除播放历史确认框，底部双卡片、
    SafeArea 避让、遮罩、上滑淡入/下滑淡出；浅色与深色主题使用独立 token。
  - 播放列表页每个列表的"更多"操作改为 `PlaylistOptionsActionSheet`，重命名输入改为
    `PlaylistRenameActionSheet`，删除确认复用 `SettingsConfirmationActionSheet`；保留
    `renamePlaylist` / `deletePlaylist` 原业务回调。
  - `SettingsConfirmationActionSheetSourceTest` / `PlaylistActionSheetSourceTest` 覆盖 UI 契约；
    `testDebugUnitTest`、`assembleDebug` 通过并安装到设备 `838eac33`。
- P9-2 崩溃归类与日志脱敏 (2026-05-17):
  - 新增 `CrashCategoryPolicy` + `CrashCategory`，依据来源标签、`Throwable` 类型 / message /
    堆栈以及 `cause` 链路映射 playback / permission / media_store / resource_inflate /
    release_script / unknown 六类。
  - 新增 `CrashRedactionPolicy`，写盘前对堆栈 / 诊断正文做路径脱敏：
    `/storage/emulated/...`、`/sdcard/...`、`file:///...` 收敛为 `<file>` 或 `<file>.<ext>`；
    `content://media/external/video/<id>` 收敛为 `content_media_id_<id>`；其他 `content://`
    收敛为 `<content_uri>`。
  - `CrashLogger` 接入两者，文件名改为 `<source>_<category>_<ts>.txt`，日志头部追加
    `source=` 与 `category=` 行，便于后续问题排序与远端 Webhook 通知归类。
- P9-3 启动 / 首帧指标 (2026-05-17):
  - `PlayerStartupTrace` 增加 `Events` 常量与 `recordOnce(name)` / `hasRecorded(name)` 接口，
    所有现有埋点（`activity_created` / `player_initialized` / `player_view_attached` /
    `subtitle_scan_finished` / `prepare_ready` / `first_frame_rendered` / `first_frame_timeout`）
    走常量，避免字符串漂移。
  - 新增 `PlayerFirstFrameTimeoutPolicy`（纯函数）：默认 `8s` 阈值，根据视频轨道存在 / 首帧
    已渲染 / 已超时三种状态决定是否排程超时 runnable，并提供 `isFirstFrameLate()` 供日志侧用。
  - `PlayerActivity.onPlaybackStateChanged(STATE_READY)` 第一次进入时调用新的 `onPrepareReady()`：
    打 `prepare_ready` 埋点 + 调 `scheduleFirstFrameTimeoutCheck`；`onRenderedFirstFrame` 取消
    超时检查；超时则记录一条 `player_first_frame_timeout` 诊断日志（已经过 `CrashRedactionPolicy`
    脱敏）。
- P9-3 follow-up: decoder / fallback 事件埋点 (2026-05-17):
  - 新增 `PlayerDecoderEventPolicy`：根据 decoder 名 / 错误类名映射成 startup trace 事件名
    （`video_decoder=<name>` / `audio_decoder=<name>` / `*_decoder_software` /
    `video_codec_error=<short>` / `audio_codec_error=<short>`），常见前缀
    `omx.google.` / `c2.android.` / `ffmpeg` / `libffmpeg` 视为软解。
  - `PlayerActivity.attachStartupAnalyticsListener()` 给 `viewModel.player` 挂 Media3
    `AnalyticsListener`，把 `onVideoDecoderInitialized` / `onAudioDecoderInitialized` /
    `onVideoCodecError` / `onAudioCodecError` 转发到 `startupTrace.recordOnce`，`onDestroy`
    时 `removeAnalyticsListener` 解绑。
  - 与 first-frame 诊断日志一起，能定位"用了什么 decoder / 是否走软解 / 是否触发 codec error"。
- 浅色主题视觉修复 (2026-05-17):
  - Player 退出后主页中间区域显示深色 ("上下浅、中间深")：根因是
    `PlayerActivity.onCreate` 之前硬编码 `delegate.localNightMode = MODE_NIGHT_YES`，
    AppCompat 会通过 `Resources.updateConfiguration({uiMode = NIGHT_YES})` 修改进程级
    Resources 配置，退出 Player 后 `bg_app_root` 渐变 drawable 继续按 night token 解析，
    而 `BottomNavigationView` 又重新拿 light token，于是错配。修复：直接删掉
    `localNightMode = MODE_NIGHT_YES`，Player 主题已硬编码深色，无需再强制全局夜间模式；
    原位置补中文注释说明这个坑。
  - 浅色主题下视频缩略图右下角"播放时长"角标看不见：根因是 `item_video.xml` 的
    `tv_duration` 背景固定深色 `#BF000000`（必须深色，叠在缩略图上），但 `textColor` 用了
    跟随主题的 `@color/ov_text_primary`，浅色下解析为接近黑，与黑底融成一片。修复：
    `tv_duration` 文字色固定为 `@android:color/white`，与固定深色背景始终保持对比。
    `item_video_grid.xml` / `item_playlist_video.xml` 的 `tv_duration` 无深色背景，
    继续跟随 `ov_text_secondary` 即可。
- P9-1 PlayerActivity 微切片 (2026-05-17):
  - 新增 `PlayerTimeFormatter`：把 `HH:MM:SS` / `MM:SS` 格式化逻辑从 Activity 抽出，统一处理
    负数 / 不足 1 秒 / 跨小时边界。`PlayerActivity.formatTime` 现在是一行 delegate。
  - 新增 `PlayerOrientationTogglePolicy`：全屏按钮按 `Configuration.orientation` 决定下一个
    `requestedOrientation`，把原本的 `== 1` magic number 换成具名常量比较。
  - 新增 `PlayerSpeedLabel`：倍速文本 `Nx` / `N.Nx` 输出逻辑抽离，`landSpeedLabel` 改为
    一行 delegate；负值 / 不支持速度都先走 `DefaultPlayerSettings.supportedSpeedOrDefault`。
  - 新增 `PlayerLandscapeBadgePolicy`：4K 角标显示阈值（`UHD_4K_MIN_WIDTH = 3840`）从 Activity
    抽出，未来扩展 8K / HDR 等角标只改策略侧。
  - 新增 `PlaybackServiceIntents`：`PlaybackService` 的 start / stop Intent 构造内聚到一处，
    Activity 不再直接持有 `ACTION_START` / `EXTRA_TITLE` / `EXTRA_IS_PLAYING` 常量；顺手去掉
    Activity 中冗余的 `SDK_INT >= O` 分支（`ContextCompat.startForegroundService` 已向下兼容）。
  - 新增 `PlayerLandscapeGeometry` / `PlayerLandscapeGeometryPolicy`：把 `applyLandscapePlayerGeometry`
    里 7 个横屏比率 + 4 组 dp clamp 全部打包到一个纯函数；Activity 只剩"读 controlsContainer
    尺寸 → 把策略结果回填 `ConstraintLayout` / `LinearLayout.LayoutParams`"。未测量容器返回
    `null`，非正 density 自动回退 1x，避免 dp clamp 全部归零；source test 守护 Activity 中
    不再出现这些比率与 dp clamp 字面量。
  - 扩展 `PlayerDisplayAdjustment`：新增 `mirrorScaleX(mirror)` 与
    `subtitleTranslationY(playerViewHeightPx, position)` 两个纯函数。`applyDisplaySettings`
    里的 `if (mirror) -1f else 1f` 三目与 `applyPlayerSettings` 里的 `playerView.height * 0.6f`
    + `position.coerceIn(0f, 1f)` 都从 Activity 中收敛到策略；source test 同时守护两条路径
    走 helper，防止 magic number 回流。
- Each extracted policy has focused unit/source tests.

Latest verification baseline used during P5:

- `.\gradlew.bat :app:testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:lintDebug`
- `git diff --check`
- Device install/startup checks have been run repeatedly on device `838eac33`.

Known existing warning:

- KSP generated-source integration should stay on the normal AGP 9 build path without experimental bridge properties.

Known deferred display issue:

- On device `838eac33`, the short-video file
  `2024-06-27 23-00-00_突破英语听口_第58天_坚持30天_盲听_逐句详解_..._video.mp4`
  was investigated because stretch/crop appeared ineffective while other videos behaved normally.
- Findings:
  - App prefs were already set to `aspect_ratio=stretch`.
  - `player_view`, `exo_content_frame`, and the video Surface all occupied the landscape player
    bounds `[104,0][2296,1036]`.
  - MediaStore/ffprobe reported the file as `1080x1920`, SAR `1:1`, DAR `9:16`.
  - An extracted frame confirmed the large black background, title text, bottom subtitle text, and
    smaller horizontal sitcom window are baked into the source frame.
- Conclusion:
  - Existing aspect modes resize the whole decoded frame; they cannot isolate and enlarge an
    embedded content window inside a portrait canvas.
  - Done (2026-05-23): manual zoom/pan and R-04 smart crop POC now cover the first advanced
    framing path. The smart crop path is session-only, landscape-only, and now uses video
    render-layer non-black content bounds first, with final `PlayerView` capture only as a
    fallback; per-video persistence remains a separate product decision.

## P5 Status

P5 is effectively complete for the originally recommended continuation slice.

- Done in this continuation:
  - Video layout/orientation policy extraction.
  - PiP entry policy extraction.
  - Play/pause icon state policy extraction.
  - Exit flow final slimming.
- Also completed as enabling cleanup for P6:
  - Subtitle load routing extraction.
  - Media info async loading/caching.
  - Display settings foreground re-sync split into a dedicated helper.
  - Fast subtitle/audio quick entry dialogs.
  - Local subtitle auto-matching.
  - Subtitle delay quick controls and settings preview first slice.

Remaining P5 work is optional review only:

- Audit any small leftover inline UI-only decisions in `PlayerActivity` if a follow-up refactor is
  already touching the file.
- Do not expand subtitle IO/state redesign under P5.

## Next Recommended Work

> **统一排期见 [ROADMAP.md](./ROADMAP.md)**（含 PS 优先级分、Wave 1～5、R-01～R-15 backlog）。

### 1. Continue P9 Stability And Release

P8 media-library slices are complete. **Current default next steps** (from master [ROADMAP.md](./ROADMAP.md)):

1. Wave 3 — P9-1b **smart crop / content-window detection** (manual pinch/pan + R-04 POC Done; remaining work is regression assets and polish).
2. Wave 4 — P9-4 release automation (KSP generated-source verification, CI warning gate).
3. P2 polish — ~~landscape subtitle quick entry~~ **Done (2026-05-22):** `btn_land_subtitles` in `layout-land/player_controls.xml`.
4. P7 follow-ups — R-06 permission-loss history, R-07 queue-order UI label (optional).

P9-2 crash taxonomy and P9-3 startup metrics are already complete (2026-05-17). Pre-orientation hook for queue switching was pulled in as a P9-1 quick win (2026-05-16).

- `P7-1` current state:
  - Done: per-video `speed`, `aspectRatioKey`, external subtitle URI, subtitle enabled state,
    audio mute state, selected audio track index persistence/restore, and scan-based stale-history
    cleanup for deleted/unindexed files.
  - Next: if subtitle/audio systems grow more complex, move from URI/index-based memory to a more
    stable identity model.
- `P7-2` current state:
  - Done: history/home continue-watching presentation, availability dimming, non-clickable missing
    files, localized labels, and scan-based stale-history cleanup.
  - Done: permission-loss flows hide stored history fallbacks instead of deleting entries; stale cleanup still only runs after a successful published scan confirms the item is gone.
- `P7-3` current state:
  - Done: `PlayerPlaybackEndPolicy`, loop/auto-next mapping, AB-loop priority, and user-facing
    `PlaybackEndBehavior` selector (follow / next / replay / stop / return to list).
  - Next: continue-watching completed-state presentation if product wants different grouping.
- `P7-4` current state:
  - Done: same-folder queues, local continue-playback FAB, and eligible playlist detail queues use
    `PlayerEpisodeOrderingPolicy.orderQueueIfEligible()`; Home/History playback do not reorder.
  - Done: the player list panel explains that detected episodes play in episode order, otherwise the queue keeps the list order.
- P6-3 subtitle settings UI/chrome is **Done (2026-05-22)**; do not reopen unless regression.
- R-15 smart-crop capture hygiene (2026-05-24): smart-crop Toasts are now cancelled before a new
  capture attempt, so visual fallback screenshots are not polluted by stale Toast overlays.

Suggested tests:

- History restore edge cases.
- Continue-watching grouping/sorting.
- Playlist or recent-playback integration boundaries.

### 2. P6-3 Subtitle Settings — Done

Status: **Done (2026-05-22)**. Full handoff: [`播放器字幕设置.md`](../播放器字幕设置.md).

- Done (2026-05-17): quick dialog ±500 ms rows show current offset in label text.
- Done (2026-05-22): landscape subtitle quick entry — `btn_land_subtitles` on the land right float column
  binds `showSubtitleQuickDialog()` (same as `portrait_btn_subtitles`); `PlayerQuickEntrySourceTest` guards wiring.
  - `OpenSubtitleSettings` → `openSubtitleSettingsSheet()` → `PlayerSubtitleSettingsSheet`.
  - `PlayerSettingsSheetChrome` shared with `PlayerSettingsDialog` / `BaseSettingsSheet`.
  - Subtitle color swatches in player settings grid (`PlayerSubtitleColorSwatchBinder`).
  - Chrome hide/restore on sheet open/dismiss; dismiss on `switchSessionVideo`.
  - `BaseSettingsSheet`: `DialogFragment` + plain `Dialog` (not BottomSheet) for consistent opacity.

Tests in place: `PlayerSubtitleSettingsSheetTest`, `PlayerSettingsDialogTest`, `PlayerQuickEntrySourceTest`,
`PlayerSettingsActivityIntegrationTest`, `PlayerGestureHudSourceTest` (dismiss on switch).

Do not reopen unless a concrete regression appears.

## Defer From Current Next Slice

- Do not reopen completed P5 extraction work unless a concrete regression appears.
- Do not redesign cross-process `SharedPreferences` behavior as part of the display sync fix; the
  current `onResume -> applyDisplaySettings()` helper is the intended small-scope solution for now.

## Working Rules For Next Slice

Use the established P5/P6 pattern:

1. Add 3-5 pure behavior tests.
2. Add a source test when Activity integration matters.
3. Run the test red and confirm the failure is meaningful.
4. Make the smallest Activity/UI change.
5. Run the feature subset, full unit tests, assemble, lint, and `git diff --check`.
6. Install and launch on device when the change affects runtime playback UI.

## Historical Note

The following items from the previous version of this handoff have already been completed in the
2026-05-15 continuation and should not be treated as outstanding:

- Video Layout And Orientation Policy
- PiP Entry Policy
- Play/Pause Icon State Policy
- Exit Flow Final Slimming
- P6-1 Fast Subtitle/Audio Entry Points
- P6-2 Local Subtitle Auto-Matching
- P6-4 Audio Diagnostics Polish

Previously suggested tests for those slices were implemented through focused policy tests and source
tests, including:

- Unknown width/height keeps safe defaults.
- Rotation metadata and pixel aspect ratio are interpreted consistently.
- PiP invalid-dimension fallback uses a safe ratio.
- Play/pause icon state stays in sync during transient playback changes.
- Display-only settings re-sync on resume is explicitly source-tested.
