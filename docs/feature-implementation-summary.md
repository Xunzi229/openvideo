# OpenVideo 已实现功能整理（工程快照）

> 本文汇总近期已落地、且值得后续功能对齐的 UI/交互与工程约定。**细则仍以各专题文档与源码为准。**
>
> **最近更新：** 2026-05-17  

---

## 1. 视频页（Home）— 高级筛选 Popover

**目标：** 在「视频」分类下用右上角 anchored popover 做时长 / 格式 / 添加日期筛选（深色半透明卡片 + pill）；筛选数据走 `MediaLibraryAdvancedFilters` + `HomeViewModel`。

**已实现要点**

- `PopupWindow` 全屏轻 scrim + 面板；宽度受屏宽比例与左右边距约束（见 `VideoLibraryFilterPopover`）。
- **布局坑：** `overlay` 里 `<include>` **勿加** `android:id`，否则会顶替被 include 根视图 id，找不到 `filter_popover_root` 会 NPE。
- 未在整块内容区上使用 `RenderEffect` 强模糊（曾导致文字不可读）；质感以令牌色与半透明为主。
- **`DateFilter.TODAY`**：`MediaLibrarySearchPolicy` 按约 24h 过滤；Popover 草稿 ↔ 应用用 `VideoLibraryFilterUiState`。

**核心文件**

| 类型 | 路径 |
|------|------|
| Popover | `app/src/main/java/com/example/openvideo/ui/home/VideoLibraryFilterPopover.kt` |
| 草稿态 | `app/src/main/java/com/example/openvideo/ui/home/VideoLibraryFilterUiState.kt` |
| 筛选策略 | `app/src/main/java/com/example/openvideo/ui/home/MediaLibraryAdvancedFilters.kt`、`app/src/main/java/com/example/openvideo/ui/home/MediaLibrarySearchPolicy.kt` |
| 布局/令牌 | `app/src/main/res/layout/overlay_video_library_filter.xml`、`view_video_library_filter_popover.xml`、`values/filter_popover_tokens.xml`、`values-night/filter_popover_tokens.xml` |
| 入口 | `HomeFragment.kt`、`fragment_home.xml`（`btn_library_filter`） |

**首页类目/列表格等其它约定**（与筛选独立）：[`roadmap/home-experience-handoff.md`](roadmap/home-experience-handoff.md)。

---

## 2. 播放器 — 居中「玻璃卡片」快捷弹窗

**目标：** 长宽比、倍速、音轨/字幕快捷单列统一为：**半透明圆角外壳 + 行内圆角卡片 + 自定义 radio**；居中、适中宽度、选项区可滚动、背后 dim +（API 31+）约 18dp 模糊。

**已实现要点**

- 布局：`dialog_player_glass_sheet.xml`、`item_player_glass_sheet_row.xml`。
- 令牌：`aspect_ratio_dialog_tokens.xml`（`player_aspect_*`）。
- **`applyPlayerGlassSheetChrome()`**：透明 window、`decorView.alpha = 1`；勿与 **`applyPlayerSheetStyle()`** 叠在同一弹窗。
- **`capPlayerGlassSheetScroll`**：标题不滚、仅 `NestedScrollView` 内滚动；小屏多 `post` 重试测宽。
- `NestedScrollView`：`overScrollMode="ifContentScrolls"`（**勿**写成 `if_content_scrolls`）。

**Kotlin：** `PlayerActivity` — `inflatePlayerGlassSheet`、`applyGlassSheetRowVisual`、`showAspectRatioQuickDialog`、`showSpeedPickerDialog`、`showQuickEntryDialog`。

**专职约定：** [`player-glass-sheet-dialog.md`](player-glass-sheet-dialog.md)  

**Cursor 规则：** [`.cursor/rules/player-glass-sheet-dialog.mdc`](../.cursor/rules/player-glass-sheet-dialog.mdc)

---

## 3. 播放器 — 字幕快捷弹窗（P6-3 随访）

**已实现：** ± 延迟行使用 **`player_quick_subtitle_delay_minus_in_dialog` / `plus_in_dialog`**，文案中带 **当前偏移 ms**（`playerPrefs.subtitleDelayMs`）。字幕设置页短按钮仍为 `player_quick_subtitle_delay_minus/plus`。

**涉及：** `PlayerActivity.showSubtitleQuickDialog`；`values/strings.xml`、`values-zh-rCN/strings.xml`。

路线图记录：[`roadmap/player-p5-continuation.md`](roadmap/player-p5-continuation.md) → 「### 2. P6-3 Follow-up Polish」。

---

## 4. 维护约定

| 触发条件 | 建议 |
|----------|------|
| 新增同类居中玻璃弹窗 | 更新 §2 入口清单；校对 `player-glass-sheet-dialog.md` |
| Home 筛选行为/令牌变更 | 更新 §1 表与策略类名 |
| 快捷字幕等 quick entry 文案策略变更 | 更新 §3 |

粗粒度路线图档案：[`roadmap/player-optimization-roadmap.md`](roadmap/player-optimization-roadmap.md)、[`roadmap/player-p5-continuation.md`](roadmap/player-p5-continuation.md)。本文定位为 **可实现功能索引**，避免信息只残留在对话或散落注释中。
