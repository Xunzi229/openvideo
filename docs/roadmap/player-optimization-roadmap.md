# OpenVideo Player Optimization Roadmap

> **执行入口已迁移**：优先级、分波计划与 backlog 评分见 **[ROADMAP.md](./ROADMAP.md)**。  
> 本文件保留 P5～P9 各条目的需求说明与 Done 明细，作为档案库使用。

This roadmap turns the next player work into a sequence of focused, testable increments.
The goal is to move OpenVideo from a feature-rich local player toward a player with
polished playback feel, strong subtitle/audio workflows, reliable watch continuity, and
repeatable release engineering.

## Principles

- Prefer high-frequency playback ergonomics before adding more low-frequency options.
- Keep each phase small enough to test with unit/source tests plus targeted device checks.
- Extract pure logic from Activity/Dialog code whenever a behavior becomes complex.
- Preserve existing user preferences and migrate only when the user-facing behavior improves.
- Treat missing files, permission changes, and playback errors as first-class states.

## Reference Players

- VLC Android: broad format support, network streams, subtitle/audio track controls, gestures.
- MX Player: smooth gestures, clear subtitle experience, quick playback controls.
- mpv/mpv-android: advanced playback tuning, hardware/software decode flexibility, precise controls.

OpenVideo already has many of these foundations: Media3 playback, FFmpeg extension preference,
subtitle loading, audio track selection, gestures, AB loop, bookmarks, screenshots, playlists,
privacy filtering, folder filters, recent playback, and release scripts. The next work should
connect these into a calmer and more predictable experience.

## P5: Playback Control Experience

Goal: make playback controls feel immediate, consistent, and low-friction.

### P5 Status Snapshot

Updated: 2026-05-16.

Completed:

- Extracted gesture HUD, double-tap seek, horizontal/vertical seek preview, long-press speed,
  lock gesture/back behavior, gesture presets, and brightness/volume adjustment policies.
- Extracted control chrome visibility, PiP chrome hiding, first-frame scrim, subtitle presentation,
  progress-save throttling, playback tick seek priority, AB loop, queue end behavior, video-switch
  session reset, lifecycle pause/resume, background playback service start, and exit release policy.
- Added focused unit/source tests for each extracted policy class.
- Fixed vertical brightness/volume travel so half-screen vertical movement can cover the full
  0-100% range in landscape. The current effective travel ratio is 50% of screen height.
- Current verification baseline after the latest P5 increment:
  - `.\gradlew.bat :app:testDebugUnitTest`
  - `.\gradlew.bat :app:assembleDebug`
  - `.\gradlew.bat :app:lintDebug`
  - `git diff --check`
  - Recent device installs/startup checks have been run on device `838eac33`.

Current P5 source hotspots:

- `PlayerActivity.kt` is still large and should not be considered fully slimmed yet.
- Keep using the existing pattern for each next slice:
  1. add 3-5 pure behavior tests,
  2. add a source test when Activity integration matters,
  3. run red,
  4. make the smallest Activity change,
  5. run the P5 strategy subset, full unit tests, assemble, lint, diff check, and device install when user-facing.

### P5-1 Unified Gesture HUD And Double-Tap Feedback

Status: mostly complete.

- Done:
  - One HUD model now covers seek, volume, brightness, speed, lock, and related gesture feedback.
  - Double-tap seek uses stable anchor state and avoids seeking when duration is unknown.
  - Long-press speed start/release is policy-driven.
  - Brightness/volume level math is policy-driven, including landscape-friendly 50% vertical travel.
- Remaining:
  - Polish visual HUD layout and animation after manual device testing.
  - Add explicit error/info HUD variants only when there is a concrete playback error workflow to wire.

### P5-2 Horizontal Seek Refinement

Status: complete for logic, visual polish remains optional.

- Done:
  - Horizontal and vertical seek previews clamp to timeline bounds.
  - Pending seek is committed only when seekable.
  - Swipe sensitivity maps to different seek windows.
- Remaining:
  - Thumbnail preview is intentionally not implemented yet. Keep this for a later UX increment.

### P5-3 Lock Mode And Minimal Controls

Status: mostly complete.

- Done:
  - Lock touch/back behavior is policy-driven.
  - Locked controls reveal and auto-hide through chrome presentation policy.
  - Pause-on-exit unlock path is covered.
- Done (2026-05-17, policy slice):
  - 新增 `PlayerLockedControlsPolicy`：`visibility` / `isChromeRegionVisible` /
    `allows(PlayerLockedInteraction)` 集中定义锁屏时仅显示锁按钮、禁止 transport/settings/seek/
    chrome 切换等交互。
  - `PlayerActivity.applyControlVisibility` 按 `PlayerChromeRegion` 逐项应用可见性；
    `setPlayerClickListener` 为所有 transport/settings/back 按钮与进度条拖动加锁屏守卫。
- Remaining:
  - 竖屏 / 横屏 layout 真机点检：确认锁屏时无遗漏的可点击控件（尤其横屏右侧浮动栏）。

### P5-4 Gesture Presets

Status: complete.

- Done:
  - Presets exist for Classic, Minimal, Binge, and Power User.
  - Presets write existing gesture preferences rather than introducing a second preference system.
  - Manual customization remains possible after applying a preset.

### P5-5 Video Layout And Orientation Policy

Status: complete.

- Done:
  - `applyVideoOrientation`, `setPlayerResizeMode`, and `applyPlayerContentAspectRatio`
    decisions are extracted through `PlayerVideoLayoutPolicy`.
  - Pixel aspect ratio and rotation metadata are handled in the policy layer.
  - Activity keeps the platform/UI calls such as `requestedOrientation`,
    `PlayerView.resizeMode`, and content frame application.
- Done (2026-05-17): 手动方向锁定
  - 用户点击全屏按钮手动切换横/竖屏后，`PlayerActivity.userOverrodeOrientation` 置为 true，
    本视频会话内禁止 `onVideoSizeChanged` 等回调再次依据视频宽高把方向覆盖回去。
  - 切换到下一首视频时（`preApplyOrientationForItem`）自动复位该标志，恢复"按视频宽高自动方向"。
  - 解决"切到横屏后过一会被自动转回竖屏"的体验问题。
- Covered by tests:
  - Unknown width/height safe defaults.
  - Portrait vs. landscape orientation choice.
  - Rotation metadata consistency.
  - Pixel ratio interpretation.
  - Resize mode preference mapping.
- Deferred follow-up:
  - Some short-video files are authored as a portrait 9:16 canvas with large black regions and a
    smaller horizontal video window baked into the actual frame. Example investigated on device
    `838eac33`: `2024-06-27 23-00-00_突破英语听口_第58天_坚持30天_盲听_逐句详解_..._video.mp4`
    reports `1080x1920`, SAR `1:1`, DAR `9:16`; screenshots and an extracted frame confirmed the
    black background/title/subtitle are part of the source frame. Existing fit/fill/crop/stretch
    modes only resize the whole decoded frame, so they cannot enlarge just the embedded center
    video window. Done (2026-05-23): manual pan/zoom and R-04 smart crop POC now provide the
    first separate content zoom/crop path. Smart crop is session-only, landscape-only, and based
    on video render-layer non-black content bounds first, with final `PlayerView` capture only as
    a fallback.
  - Done (2026-05-24): new smart-crop capture attempts cancel the previous smart-crop Toast before
    hiding controls and sampling fallback screenshots, avoiding Toast overlay pollution.

### P5-6 PiP Entry Policy

Status: complete.

- Done:
  - `enterPipModeIfSupported` decision logic is extracted through `PlayerPipPolicy`.
  - SDK support, feature support, re-entry guarding, and aspect ratio fallback are policy-driven.
  - Platform calls remain in Activity.
- Covered by tests:
  - Unsupported SDK never enters PiP.
  - Invalid dimensions use a safe fallback.
  - Valid dimensions produce expected aspect ratio.
  - Re-entering PiP while already in PiP is guarded.

### P5-7 Play/Pause Icon State Policy

Status: complete.

- Done:
  - `updatePlayPauseIcon`, `togglePlayPauseAndSyncIcon`, and `syncPlayPauseIcon`
    state decisions are extracted through `PlayerPlayPausePolicy`.
- Covered by tests:
  - `isPlaying=true` maps to pause icon.
  - `playWhenReady=true` transient states map consistently.
  - paused/stopped maps to play icon.
  - toggle action keeps icon and player state in sync.

### P5-8 Exit Flow Final Slimming

Status: complete.

- Done:
  - Release-after-exit gating is policy-driven through `PlayerExitPolicy`.
  - Exit presentation timing and transition strategy are policy-driven.
  - `finishPlayer` keeps Android platform calls, while pure timing/state decisions live in policy.

### P5-9 Subtitle Loading

Status: partial extraction done, larger redesign deferred to P6.

- Done:
  - Subtitle source routing is extracted through `PlayerSubtitleLoadPolicy`.
  - Explicit subtitle URI vs. local sidecar selection no longer lives directly in `PlayerActivity`.
- Remaining:
  - `loadSubtitlesAsync` / `loadSubtitles` still mix IO, ViewModel state, and Toast behavior.
  - Keep the larger subtitle loading redesign in P6.

## P6: Subtitle And Audio Track Experience

Goal: make subtitles and audio tracks quick to choose, adjust, and trust.

### P6-1 Fast Subtitle/Audio Entry Points

Status: complete.

- Add first-level playback controls for subtitle and audio track selection.
- Keep deeper options in the settings panel.
- Show current subtitle/audio state in the selection UI.
- Tests:
  - Selection state refresh.
  - Unsupported audio track disabled state.
  - No-track empty state.
- Done (2026-05-17): 横屏右侧浮动栏整理 + 快速对话框样式统一
  - 去除横屏右侧浮动栏中 `btn_land_aspect` 原本绑定到"打开播放器设置"的重复入口，避免与右上角
    `btn_settings` 重复。
  - 恢复同位置的宽高比图标按钮，绑定真正的「宽高比快速选择」对话框（`showAspectRatioQuickDialog`）：
    适应屏幕 / 填充屏幕 / 16:9 / 4:3 / 裁切 / 拉伸 六个选项，默认选中当前比例，选择后调
    `playerPrefs.aspectRatio` + `viewModel.setAspectRatio` + `applyDisplaySettings()` 即时生效。
  - 早期实现曾通过私有 `AlertDialog.applyPlayerSheetStyle()` 同步横屏快速对话框透明度；2026-05-24
    已迁移到 `PlayerGlassSheetDialog` + `PlayerGlassSheetChrome`，见下方响应式样式记录。
- Done (2026-05-18): 快速弹窗互斥门闩
  - 播放器页新增 `activePlayerDialog` / `showExclusivePlayerDialog`，覆盖倍速、比例、音轨、字幕、
    更多设置、剧集列表，快速连点不再叠多个弹窗。
  - 设置页新增 `activeSettingsDialog` / `showExclusiveSettingsDialog`，覆盖默认画面比例、默认播放速度、
    清缓存、清历史确认框。
  - `PlayerQuickEntrySourceTest` / `SettingsLanguageRowSourceTest` 通过；ADB 安装后用户真机验证通过。
- Done (2026-05-19): iOS Action Sheet 确认/操作弹窗
  - 设置页清除缩略图缓存、清除播放历史确认框迁移到 `SettingsConfirmationActionSheet`，采用底部
    iOS Action Sheet 样式，浅/深色主题分别适配。
  - 播放列表页单个列表的重命名/删除路径迁移到底部 Sheet：`PlaylistOptionsActionSheet`、
    `PlaylistRenameActionSheet`、删除确认复用 `SettingsConfirmationActionSheet`。
  - 保留原 `clearCache` / `clearHistory` / `renamePlaylist` / `deletePlaylist` 业务逻辑；
    `SettingsConfirmationActionSheetSourceTest` / `PlaylistActionSheetSourceTest` 通过。
- Done (2026-05-22): 横屏字幕快捷入口
  - `layout-land/player_controls.xml` 右侧浮动栏新增 `btn_land_subtitles`（倍速 → 宽高比 → 字幕 → PiP）。
  - 绑定 `showSubtitleQuickDialog()`，与竖屏 `portrait_btn_subtitles` 同路径；`PlayerQuickEntrySourceTest.landscapeSubtitleButtonUsesDedicatedQuickDialog` 守护。
- Done (2026-05-24): 播放器快捷弹窗响应式样式
  - 倍速、音轨、字幕、长宽比统一走 `PlayerGlassSheetDialog.showSingleChoice(...)`，由 `quickChoiceChrome()` 按方向分流。
  - 竖屏快捷弹窗使用 `PLAYER_BOTTOM`，复用选集同款底部滑出样式与 `dialog_player_quick_bottom_sheet.xml` / `item_player_quick_bottom_sheet_row.xml`。
  - 横屏快捷弹窗使用 `PLAYER_SETTINGS_PANEL`，复用 `PlayerSettingsSheetChrome.applyWindowLayout`、`applyBackdrop`、`applyPanelOpacity`，与播放器设置面板保持相同宽高、背景透明度/模糊和面板透明度。
  - 快捷弹窗打开前隐藏播放/暂停/进度条等播放器 controls，只显示设置项；`PlayerQuickEntrySourceTest` 覆盖响应式 chrome 与隐藏 controls。

### P6-2 Local Subtitle Auto-Matching

Status: complete.

- Auto-detect same-folder subtitle files by base name and common suffixes.
- Start with `.srt`, `.vtt`, `.ass`, and `.ssa` detection; render support can remain best-effort.
- Prefer exact base-name matches, then language suffix matches.
- Tests:
  - Match ranking.
  - Multiple subtitle files.
  - Permission and missing-file handling.

### P6-3 Subtitle Delay And Style Preview

Status: **complete (2026-05-22)**. Handoff: [`播放器字幕设置.md`](../播放器字幕设置.md).

- Done:
  - Quick subtitle delay controls: -500 ms, +500 ms, reset (quick dialog labels include current offset).
  - Live preview for size, color, background, and position in full subtitle settings sheet.
  -「更多字幕设置」opens `PlayerSubtitleSettingsSheet`; player settings grid uses four color swatches.
  - `PlayerSettingsSheetChrome` unifies window layout, backdrop dim/blur, and panel opacity with player settings dialog.
  - Sheet open hides player chrome; dismiss restores; video switch dismisses sheet.
  - Global `PlayerPrefs` first; per-video subtitle memory in P7-1.
- Tests:
  - `PlayerSubtitleSettingsSheetTest`, `PlayerSettingsDialogTest`, `PlayerQuickEntrySourceTest`,
    `PlayerSettingsActivityIntegrationTest`, `PlayerSubtitleColorPolicyTest`.

### P6-4 Audio Diagnostics Polish

Status: complete.

- Make audio track details easier to read: language, channels, sample rate, mime type, decoder.
- Add clearer fallback/error messaging when software decode is needed.
- Tests:
  - Audio diagnostics labels.
  - Software fallback hint policy.

## P7: Watch Continuity

Goal: turn progress, recent playback, playlists, and folders into a coherent continue-watching flow.

### P7-1 Per-Video Playback Memory

Status: third slice complete; polish remains.

- Store per-video progress, speed, aspect ratio, subtitle choice, audio choice, and last position.
- Keep global defaults as fallback.
- Avoid writing noisy values too often; batch or throttle writes.
- Done:
  - `play_history` now persists per-video `speed` and `aspectRatioKey`.
  - `PlayerViewModel` restores saved speed and aspect ratio from history.
  - `PlayerActivity` restores per-video playback preferences during startup alongside resume position.
  - `play_history` now also persists per-video external subtitle URI, subtitle enabled state,
    audio mute state, and selected audio track indices.
  - Player startup and in-session video switching now restore subtitle/audio memory before
    re-applying player settings and subtitle loading.
- Done (2026-05-16):
  - Added `HistoryCleanupPolicy` and `VideoRepository.pruneStaleHistory()` so published scans remove
    history rows only when the file is missing locally and the entry no longer appears in the latest
    scan by id or normalized path.
- Remaining:
  - Extend subtitle memory beyond external subtitle URI if embedded/multi-subtitle support is added later.
  - Replace fragile audio track index restore with a more stable identity if track ordering becomes inconsistent.
- Tests:
  - Per-video override fallback.
  - Throttled writes.
  - Deleted-file cleanup.

### P7-2 Continue Watching

Status: polish slice complete for labels and stale-history cleanup.

- Upgrade recent playback to show progress, last watched time, and file availability.
- Make missing files visible as a recoverable state or filter them according to context.
- Done:
  - History list now shows progress, last watched time, duration, and missing-file state.
  - Missing files remain visible but are dimmed and non-clickable.
  - Home `RECENT` now reuses continue-watching badges instead of plain video metadata.
- Done (2026-05-16):
  - Continue-watching labels are resource-backed through `HistoryContinueWatchingLabels` for Home
    `RECENT`, History, and `HistoryContinueWatchingPolicy` (English + zh-CN).
  - Stale history cleanup now runs after published scans from Home and Local folder flows.
- Remaining:
  - Revisit whether permission-loss flows should hide vs. delete stale history instead of the current
    scan-based prune rule.
- Tests:
  - Progress display.
  - Missing file state.
  - History cleanup after scanner refresh.

### P7-3 Playback End Strategy

Status: complete for policy and user-facing selector.

- Add explicit end behavior: next item, replay, stop at end, return to list.
- Keep playlist/folder queue behavior predictable.
- Done:
  - Playback-ended decisions now run through `PlayerPlaybackEndPolicy`.
  - Current preferences map to explicit actions: list loop + auto-next advances,
    single loop replays, no next item stops at end, and return-to-list is modeled
    for a future settings entry.
  - Active AB loop wins before queue advance, so a loop near the end replays from point A.
- Done (2026-05-16):
  - Added `PlaybackEndBehavior` preference with follow / play next / replay / stop / return to list.
  - Exposed the selector in player settings dialog, playback settings activity, and sheet.
  - Explicit end behavior overrides loop/auto-next except for active AB loop replay.
- Remaining:
  - Decide whether completed videos should be removed, dimmed, or grouped differently in
    continue-watching lists after scanner/history cleanup is defined.
- Tests:
  - End state for single video.
  - End state for playlist/folder queue.
  - Interaction with repeat and AB loop.

### P7-4 Episode Ordering

Status: complete for folder and eligible playlist queues.

- Improve same-folder next/previous ordering for common episode naming.
- Keep deterministic fallback sorting by name/date.
- Done:
  - Added `PlayerEpisodeOrderingPolicy` for same-folder playback queues.
  - Recognizes common numeric episode patterns including `S01E02`, `1x02`, `EP02`, `E02`,
    and Chinese `第02集` style names.
  - `FolderVideosFragment` now orders the session queue before passing it to `PlayerActivity`,
    so the player list and auto-next behavior follow episode order inside a folder.
  - The local continue-playback FAB now launches an episode-ordered queue from the current video's
    folder instead of passing the whole local library into the player.
- Done (2026-05-16):
  - Added `orderQueueIfEligible()` and `shouldOrderQueue()` so episode ordering runs for same-folder
    queues and playlist queues with a strong episode-number signal, while unrelated mixed playlists
    keep manual order.
  - `PlaylistDetailFragment` now uses eligible ordering; Home and History playback remain unchanged.
- Done (2026-05-17): 文件夹播放队列顺序与列表展示顺序对齐
  - `shouldOrderQueue` 不再因「同文件夹」一律触发剧集排序：只有真正识别到剧集编号（`S01E02` /
    `第3集` / `EP04` 等）时才介入，否则保留调用方传入的原顺序。
  - 修复"在本地文件夹页面点击视频播放，播放队列顺序与文件夹列表显示顺序不一致"的问题
    （以前文件名不含剧集编号时，会被默默退化为按文件名字典序排）。
  - `FolderVideosFragment` / `LocalFolderFragment` continue-playback FAB / `PlaylistDetailFragment`
    三处行为一致：默认尊重展示顺序，遇到剧集场景才自动按集号排。
- Remaining:
  - Done: the player list panel exposes a short ordering label so users can understand why detected
    episodes may follow episode order while non-episode queues keep the list order.
- Tests:
  - Episode number parsing.
  - Mixed naming fallback.
  - Previous/next queue boundaries.

## P8: Media Library Depth

Goal: keep large libraries fast, understandable, and manageable.

### P8-1 Incremental MediaStore Observer Hardening

Status: complete for observer coalescing, scan outcomes, incremental diff refresh, and first-scan
progress UI.

- Continue moving from one-shot scans to targeted refreshes.
- Coalesce rapid MediaStore changes.
- Track scan state and errors in the UI.
- Done (2026-05-16):
  - `VideoScanner` emits `VideoScanOutcome` (success / permission denied / error / progress) and
    uses `MediaStoreRefreshPolicy` for observer debounce coalescing.
  - `MediaLibraryPermissionPolicy` centralizes read-permission requirements across API levels.
  - Home and Local folder screens surface permission-denied and scan-error empty states; resume
    re-checks permission and reloads the library flow.
  - Incremental diff refresh for libraries with 100+ cached videos: lightweight index query, diff by
    id/metadata, targeted `_ID IN (...)` fetches for adds/changes, full scan fallback when change
    ratio exceeds 35%. Batched in groups of 200 IDs to stay under SQLite host-variable limits.
  - First full-scan progress UI on Home and Local folders: `VideoScanOutcome.Progress` with
    indeterminate bar plus live scanned-count label (`MediaLibraryScanLoadingUi`).
  - `VideoRepository` shares a single `scanVideos()` flow via `shareIn(WhileSubscribed(5s), replay=1)`,
    so Home + Local Fragments no longer register duplicate `ContentObserver`s or duplicate scans.
  - Progress reporting switched from `trySend` to `channel.send` (no dropped progress on buffer
    pressure).
  - Delete paths now invalidate `videoCache` (`@Synchronized removeCachedVideo`) so diff scans do
    not keep returning a just-deleted item.
  - `ContentUris.withAppendedId(collection, id)` uses the active collection URI on Android 10+
    instead of hard-coding `EXTERNAL_CONTENT_URI`.
- Tests:
  - Observer registration source tests.
  - Debounce/coalescing policy.
  - Permission revoked handling.
  - Diff policy (added/removed/changed/no-op).
  - Repository shared scan flow source test.
  - Scanner cache invalidation source test.

### P8-2 Folder Filter UI Polish

Status: complete for pinned folders, empty-folder hiding, folder-chip counts, and count-first folder sorting.

- Add favorite folders or pinned folders.
- Hide empty folders after filtering.
- Improve folder sorting and counts.
- Done (2026-05-16):
  - Added `VideoFolderFilterPolicy` plus `AppPrefs.pinnedFolderKeys` for long-press pin/unpin on
    Home folder chips and Local folder rows.
  - Folder chips now derive from the current category/search list (without the active folder
    filter), hide zero-count folders, sort pinned folders first, and prune stale pins after scans.
  - Selected folder keys are still invalidated when a folder disappears from the library.
- Done (2026-05-24):
  - Home folder chips and Local folder rows keep pinned folders first, then sort within pinned and
    unpinned groups by video count descending, with name/key as stable tie-breakers.
  - Home category chips show the current filtered All/Recent/Favorites counts, making empty or
    matching Favorites visible before the user switches categories.
- Tests:
  - Folder key stability.
  - Count updates after privacy filtering.
  - Selected folder invalidation.
  - Pinned-group folder sorting by video count.
  - Category chip count binding.

### P8-3 Search And Filters

Status: complete for path search and optional advanced filters.

- Search by filename and folder path.
- Add optional filters for duration, format, and date.
- Keep default UI simple; advanced filters should not crowd the main library.
- Done (2026-05-16):
  - `MediaLibrarySearchPolicy` matches title, file path, file name, and parent folder name.
  - `MediaLibraryAdvancedFilters` adds duration (short/medium/long), format (mp4/mkv/avi/webm),
    and date-added (last 7d / 30d / older) buckets behind one filter button on the Home sort row;
    active filters tint the button accent color.
  - Home list, folder chips, and empty states reuse the same search/filter pipeline.
- Tests:
  - Search matching (title/path/folder name).
  - Combined search + advanced filter behavior.
  - Advanced filter source/UI hookup.

## P9: Stability, Performance, And Release

Goal: make changes safer and releases less manual.

### P9-1 Continue PlayerActivity Slimming

- Extract remaining pure playback policies from PlayerActivity.
- Target gesture HUD, lock mode, playback end strategy, and per-video memory.
- Done (2026-05-16):
  - Pre-orientation hook: `preApplyOrientationForItem()` uses MediaStore-cached
    `VideoItem.width/height` to set Activity orientation **before** `switchToVideo()`, removing
    the visible landscape→portrait flicker when continuously playing portrait clips after a
    landscape one. `onVideoSizeChanged` still calibrates with the decoded video size.
  - `applyVideoOrientation` now skips invalid `width/height` (≤0) so MediaStore-missing metadata
    no longer mis-rotates the Activity.
- Done (2026-05-17, seven micro slices):
  - 新增 `PlayerTimeFormatter`：把 `HH:MM:SS` / `MM:SS` 格式化逻辑从 Activity 抽出，统一处理
    负数 / 不足 1 秒 / 跨小时边界。`PlayerActivity.formatTime` 现在是一行 delegate。
  - 新增 `PlayerOrientationTogglePolicy`：全屏按钮按 `Configuration.orientation` 决定下一个
    `requestedOrientation`。原 `if (resources.configuration.orientation == 1)` magic number
    替换为 `PlayerOrientationTogglePolicy.nextRequestedOrientation(...)`。
  - 新增 `PlayerSpeedLabel`：倍速文本 `Nx` / `N.Nx` 输出逻辑抽离，`landSpeedLabel` 改为一行
    `PlayerSpeedLabel.format(speed)` delegate；负值 / 不支持速度都走
    `DefaultPlayerSettings.supportedSpeedOrDefault` 归一化。
  - 新增 `PlayerLandscapeBadgePolicy`：4K 角标显示阈值 `UHD_4K_MIN_WIDTH = 3840`，
    Activity 中 `if (w >= 3840)` magic number 替换为 `is4kVideo(w)` 语义化调用。
  - 新增 `PlaybackServiceIntents`：集中构造 `PlaybackService` 的 start / stop Intent，
    Activity 中 `Intent(...).apply { action = ...; putExtra(...) }` 全部走 helper；同时去掉
    冗余的 `Build.VERSION.SDK_INT >= O` 分支（`ContextCompat.startForegroundService` 已经
    向下兼容到旧版本）；Activity 不再 import `PlaybackService` 常量。
  - 新增 `PlayerLandscapeGeometry` / `PlayerLandscapeGeometryPolicy`：把 `applyLandscapePlayerGeometry`
    里所有横屏控件几何参数（容器内边距 `0.022/0.028/0.032/0.026`，图标 `0.049 / 40..52dp`，
    主播放键 `0.060 / 52..64dp`，运输间距 `0.02 / 14..22dp`，相邻图标间距 `0.009 / 6..12dp`）
    打包成纯函数计算，Activity 只保留"读 view 尺寸 + 把结果回填 `ConstraintLayout` /
    `LinearLayout.LayoutParams`"。未测量容器（宽或高 ≤ 0）返回 `null`；非正 density 会
    回退到 1x 防止 dp clamp 全部归零。
  - Wave 1.1～1.3（2026-05-17）：`PlayerGestureHudPolicy` 复用 `PlayerTimeFormatter`；
    `PlayerPipAspectRatio.toRational()` + `PlayerPipPolicy.fallbackRational()`；
    新增 `PlayerTouchActionPolicy` 替代 Activity 内 `MotionEvent.toPlayerTouchAction()`。
  - Wave 1.4（2026-05-17）：`PlayerLockedControlsPolicy` + `setPlayerClickListener` 锁屏交互守卫；
    `PlayerControlState.visibilityFor` 委托至该策略。
  - 扩展 `PlayerDisplayAdjustment`：新增 `mirrorScaleX(mirror)` 与
    `subtitleTranslationY(playerViewHeightPx, position)` 两个纯函数，
    把 `applyDisplaySettings` 里的 `if (playerPrefs.mirror) -1f else 1f` 三目，以及
    `applyPlayerSettings` 里的 `playerView.height * 0.6f` + `subtitlePosition.coerceIn(0f, 1f)`
    收敛到策略侧；负 view height 自动 clamp 到 0，越界 position 自动 clamp 到 0..1。
- Tests:
  - Pure policy tests for each extracted class.
  - Source tests to prevent logic from drifting back into Activity.
  - 新增 `PlayerTimeFormatterTest`（5 用例覆盖 0 / 负数 / 不足 1 秒 / 跨小时 / >100h）。
  - 新增 `PlayerOrientationTogglePolicyTest`（3 用例覆盖竖横未定义）。
  - 新增 `PlayerSpeedLabelTest`（3 用例覆盖整数 / 小数 / 不支持速度回退）。
  - 新增 `PlayerLandscapeBadgePolicyTest`（3 用例覆盖阈值常量 / ≥3840 / <3840 与负数）。
  - 新增 `PlaybackServiceIntentsSourceTest`：守护 `start(...)` 含 `ACTION_START` + title +
    isPlaying extras；`stop(...)` 不绑定 action；Activity 通过 helper 调用，不再出现
    `PlaybackService.ACTION_START` 内联或 `>= Build.VERSION_CODES.O` 分支。
  - 新增 `PlayerLandscapeGeometryPolicyTest`（7 用例覆盖未测量容器返回 `null`、纯比率边距、
    中等宽度 + 1x density 让 raw ratio 胜出、phone 1920x1080 + 3x 触发全部 dp 下界、4K
    + 1x 触发全部 dp 上界、非正 density 回退 1x、margin 与 density 无关）。
  - 新增 `PlayerDisplayAdjustmentTest`（5 用例覆盖 mirror true/false、字幕顶部 0、
    字幕底部 -0.6*height、position 越界 clamp、负 view height 退化为 0）。
  - 扩展 `PlayerActivityP9SlimmingSourceTest`：守护 Activity 中 `formatTime` / `landSpeedLabel`
    仍是 delegate，全屏按钮不再出现 `configuration.orientation == 1`，4K 角标不再出现
    `>= 3840` magic number；新增 `landscapeGeometryUsesPolicy`：Activity 必须走
    `PlayerLandscapeGeometryPolicy.compute(...)`，且 7 个横屏比率（`0.022f / 0.026f / 0.028f /
    0.032f / 0.049f / 0.060f / 0.009f`）以及 7 个 dp clamp 端点（`40f * dm / 52f * dm /
    64f * dm / 14f * dm / 22f * dm / 6f * dm / 12f * dm`）都不允许在 Activity 中内联出现；
    新增 `mirrorAndSubtitlePositionGoThroughDisplayAdjustment`：mirror 与字幕 translationY
    必须走 `PlayerDisplayAdjustment.mirrorScaleX` / `subtitleTranslationY`，禁止
    `if (playerPrefs.mirror) -1f else 1f`、`playerView.height * 0.6f`、
    `playerPrefs.subtitlePosition.coerceIn(0f, 1f)` 再次出现在 Activity。
  - 更新 `PlayerDisplaySettingsSourceTest.applyPlayerSettingsDelegatesDisplayOnlyStateToDedicatedHelper`
    以匹配新的 `PlayerDisplayAdjustment.mirrorScaleX(...)` 路由。
  - 更新 `CompatibilityImplementationTest.backgroundAudioStartsForegroundPlaybackService` 以
    匹配新的 helper 路由。

### P9-1b Advanced Video Framing

- Add a player-side way to handle videos with baked-in black canvas or embedded content windows.
- This is separate from normal aspect ratio resize modes because the source frame itself may be
  portrait `9:16` while the useful content is a smaller landscape rectangle inside the frame.
- Candidate approaches:
  - Manual zoom and pan controls persisted per video.
  - Preset crop modes such as center-window, top/bottom trim, or portrait-short-video crop.
  - Optional frame analysis to detect large black regions/content bounds.
- Tests:
  - Pure crop-window math for source frame vs. viewport.
  - Per-video crop/zoom persistence.
  - Interaction with existing fit/fill/crop/stretch modes.

### P9-2 Crash And Playback Failure Taxonomy

Status: complete for category mapping, file naming, and path redaction.

- Classify crashes/failures: playback, resource inflate, permission, MediaStore, release script.
- Keep logs actionable without exposing private file paths unnecessarily.
- Done (2026-05-17):
  - 新增 `CrashCategoryPolicy`：依据 `Throwable` 类型 / message / 堆栈以及来源标签
    （`player` / `uncaught` / `release_*`）映射到 `CrashCategory`（playback / permission /
    media_store / resource_inflate / release_script / unknown），同时追溯 `cause` 链路。
  - 新增 `CrashRedactionPolicy`：写盘前对 stack trace 与诊断正文做轻量脱敏，
    `/storage/emulated/0/...`、`/sdcard/...`、`file:///...` 替换为 `<file>` 或 `<file>.<ext>`，
    `content://media/external/video/<id>` 压缩为 `content_media_id_<id>`，其余 `content://...`
    替换为 `<content_uri>`。
  - `CrashLogger.write()` 调用上面两者：文件名格式改为 `<source>_<category>_<ts>.txt`，日志头部
    新增 `source=` 与 `category=` 行，堆栈/诊断正文均经过脱敏。`logPlayerError` / `install` 都用
    新签名。`buildLog` 不再依赖外部文件名拼接。
- Tests:
  - Crash category mapping (`CrashCategoryPolicyTest`)：player 源、SecurityException、MediaStore
    堆栈、LayoutInflater、cause 链路、release 源、unknown fallback。
  - Redaction policy (`CrashRedactionPolicyTest`)：外存 / sdcard / file URI / media content URI /
    其他 content URI / 无扩展名路径 / 非路径文本保持原样。
  - Source test (`CrashLoggerSourceTest`)：`write` 使用 `CrashCategoryPolicy.categorize` 与
    `<source>_<category>_` 文件名前缀；`buildLog` 写入 `category=` 行并对堆栈做脱敏；
    `buildDiagnosticLog` 也对正文脱敏。

### P9-3 Startup And First-Frame Metrics

Status: complete — trace stabilized, first-frame timeout policy, and decoder / fallback event
mapping all in place.

- Stabilize startup trace output.
- Track prepare time, first-frame time, decoder info, and fallback events.
- Done (2026-05-17):
  - `PlayerStartupTrace` 新增 `Events` 常量、`recordOnce(name)` 与 `hasRecorded(name)`，所有埋点
    （`activity_created` / `player_initialized` / `player_view_attached` /
    `subtitle_scan_finished` / `prepare_ready` / `first_frame_rendered` / `first_frame_timeout`）
    通过常量引用，杜绝字符串漂移。
  - `PlayerActivity.onPlaybackStateChanged(STATE_READY)` 第一次到达时调 `onPrepareReady()`，
    打 `prepare_ready` 埋点并按 `PlayerFirstFrameTimeoutPolicy` 排程一次首帧超时检查。
  - 新增 `PlayerFirstFrameTimeoutPolicy`（纯函数）：依据「是否有视频轨道 / 首帧是否已渲染 /
    是否已超时」决定要不要安排超时 runnable，默认 `DEFAULT_TIMEOUT_MS = 8s`；并提供
    `isFirstFrameLate()` 给日志诊断侧复用同一阈值。
  - `onRenderedFirstFrame()` 会取消挂起的超时检查；如阈值到达仍未收到首帧，写一条
    `player_first_frame_timeout` 诊断日志，记录完整 `startupTrace.format()` 内容（已经过
    `CrashRedactionPolicy` 脱敏，因为底层走 `CrashLogger.logDiagnostic`）。
- Tests:
  - Trace event ordering (`PlayerStartupTraceTest`: `recordOnce` 不重复、常量名规范、相对时间
    输出格式)。
  - Missing first-frame timeout policy (`PlayerFirstFrameTimeoutPolicyTest`: 有/无视频轨、
    已渲染、已超时、自定义阈值、`isFirstFrameLate` 边界)。
  - Source 接入 (`PlayerFirstFrameTimeoutSourceTest`): `STATE_READY` → `onPrepareReady()`，
    `scheduleFirstFrameTimeoutCheck` 调 `PlayerFirstFrameTimeoutPolicy.scheduleDelayMs`，
    超时 runnable 写入 `player_first_frame_timeout` 诊断日志，首帧 listener 取消挂起检查。
- Done (2026-05-17, follow-up slice): decoder / fallback 事件埋点
  - 新增 `PlayerDecoderEventPolicy`：把 `(decoderName)` / `(errorClassName)` 映射为 startup
    trace 事件名列表（`video_decoder=<name>` / `audio_decoder=<name>` /
    `video_decoder_software` / `audio_decoder_software` / `video_codec_error=<short>` /
    `audio_codec_error=<short>`）。
  - 软解识别基于常见前缀：`omx.google.` / `c2.android.` / `ffmpeg` / `libffmpeg`。带厂商前缀
    （`c2.qti.*` / `c2.exynos.*` / `OMX.qcom.*` 等）视为硬解。
  - `PlayerActivity.attachStartupAnalyticsListener()` 给 `viewModel.player` 挂一个 Media3
    `AnalyticsListener`，回调 `onVideoDecoderInitialized` / `onAudioDecoderInitialized` /
    `onVideoCodecError` / `onAudioCodecError` 都走该策略，`startupTrace.recordOnce` 自动去重。
  - `onDestroy` 中 `removeAnalyticsListener` 解绑，避免 listener 泄漏。
- Remaining (optional):
  - `STATE_BUFFERING` -> `STATE_READY` 二次进入是否需要单独命名（rebuffer 而非 first-frame
    超时），后续如果有缓冲问题再加。

### P9-5 Light Theme Visual Fixes

Status: complete (2026-05-17).

集中处理"浅色主题下"几个能直接看到的视觉 bug，本节只记录"修一处算一处"的小型 fix，
不引入新策略类（必要时按需补抽离），但每个 fix 都要写清触发场景与判定依据。

- Done: Player 退出后主页中间区域变深色（"上下浅、中间深"）
  - 触发场景：浅色主题下播放任一视频，退出 `PlayerActivity` 回到主页 / 本地 / 播放列表 / 我的，
    系统状态栏与底部 `BottomNavigationView` 仍是浅色，但 fragment 中间的 `RecyclerView`
    背景变成深色。
  - 根因：`PlayerActivity.onCreate` 之前硬编码 `delegate.localNightMode = MODE_NIGHT_YES`，
    AppCompat 内部会通过 `Resources.updateConfiguration({uiMode = NIGHT_YES})` 修改
    **进程级共享的 Resources 配置**。退出 Player 后该配置不会自动还原，导致 fragment
    `bg_app_root`（`<gradient>` drawable）继续用 night token 解析；而 `BottomNavigationView`
    直接拿 `@color/ov_bg_elevated_strong` 又回到 light token，于是出现错配。
  - 修复：删掉 `delegate.localNightMode = MODE_NIGHT_YES`，PlayerActivity 自身的
    `Theme.OpenVideo.Player` 已经把 `colorBackground` / `windowBackground` / `colorSurface` /
    文字色等全部硬编码为深色（`player_bg=#FF000000` 等），不需要再强制全局夜间模式。
    原位置补一段中文注释说明这个坑，防止后续误加回来。
- Done: 浅色主题下视频缩略图右下角"播放时长"角标看不见
  - 触发场景：浅色主题下进入"视频"列表，列表里的卡片缩略图右下角应有时长角标
    （类似 `02:28:59`），但该角标几乎不可见。
  - 根因：`item_video.xml` 的 `tv_duration` 背景是 `@drawable/bg_video_duration`
    （固定 `#BF000000`，必须深色因为叠在视频缩略图上），但 `textColor` 用了跟随主题的
    `@color/ov_text_primary`——浅色主题下解析为 `#FF111827`（接近黑），与黑底融成一片。
  - 修复：把 `tv_duration` 的 `textColor` 固定为 `@android:color/white`，与固定深色背景
    保持对比，与 day/night 无关。
  - 验证：`item_video_grid.xml` / `item_playlist_video.xml` 的 `tv_duration` 没有深色背景，
    属于普通文本区域，继续跟随 `ov_text_secondary` 即可，无需改动。
- Tests: 本节为小型 layout / Activity 级 fix，已经覆盖在现有
  `:app:testDebugUnitTest` + `:app:assembleDebug` 之中；如果后续再出现类似"播放器副作用
  污染主进程 Resources"的问题，应该补一个 source test 守护
  `PlayerActivity` 不再出现 `delegate.localNightMode` 赋值。

### P9-4 Release Automation

- Keep version, signing, release notes, checksums, and artifact names from one source of truth.
- Keep Gradle 10 / AGP compatibility guarded with `--warning-mode fail`.
- Keep KSP generated-source integration covered by the normal AGP 9 build path after removing the temporary bridge property.
- Tests:
  - Script version resolution.
  - Checksum generation.
  - Release notes artifact presence.

## Recommended Execution Order

> **已过期**：下列 1～10 为 2026-05-16 之前的推荐顺序，多数已完成。  
> 当前请以 **[ROADMAP.md](./ROADMAP.md)** 中 Wave 1～5 与 backlog 表（R-01～R-15）为准。

**当前推荐（2026-05-17）**：

1. Wave 1 — P9-1 继续微切片（P0）
2. Wave 2 — 锁屏审计 + 字幕 IO 分层（P1～P2）
3. Wave 3 — P9-1b 高级画面裁切（P1）
4. Wave 4 — P9-4 发布自动化（P1）
5. Wave 5 — seek 缩略图 / HUD 抛光 / 产品待定项（P2～P3）

<details>
<summary>历史推荐顺序（归档）</summary>

1. P5-5 Video Layout And Orientation Policy
2. P5-6 PiP Entry Policy
3. P5-7 Play/Pause Icon State Policy
4. P5-8 Exit Flow Final Slimming
5. P6-1 Fast Subtitle/Audio Entry Points
6. P6-2 Local Subtitle Auto-Matching
7. P7-1 Per-Video Playback Memory
8. P7-2 Continue Watching
9. P8-1 Incremental MediaStore Observer Hardening
10. P9-4 Release Automation

</details>

## Definition Of Done For Each Increment

- Behavior is covered by focused unit/source tests.
- Existing player, settings, and media-library tests still pass.
- Debug assemble and lint pass.
- Any user-visible workflow gets at least one device or screenshot verification when practical.
- Release notes are updated when the increment is user-facing.
