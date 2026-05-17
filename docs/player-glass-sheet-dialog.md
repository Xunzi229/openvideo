# 播放器「玻璃浮层卡片」单列选择弹窗 — 设计与实现约定

本文档固化 **OpenVideo** 播放器内单列选项弹窗的视觉与工程约定（长宽比、倍速、音轨/字幕快捷入口等共用）。后续新增同类「居中、深色半透明卡片 + 行间圆角卡片 + 自选 radio」功能时 **应沿用本约定**，除非产品明确要求分叉。

---

## 1. 何时使用这套样式

**适用：** 居中 `AlertDialog` + 单列可点选项（每行图标 + 文案），需要与播放器 UI 一致的现代深色玻璃质感。

**不要混用：** 仍使用列表型 `MaterialAlertDialog.setItems` / 系统自带 `RadioButton` + `applyPlayerSheetStyle()`（该路径会对整块 `decorView` 施加 alpha，易出现发灰、发糊）。

**不适用：** BottomSheet、`PlayerSettingsDialog` 全页设置、`DialogFragment` 复杂表单等（需另行设计）。

---

## 2. 视觉规格（令牌）

令牌集中在：

`app/src/main/res/values/aspect_ratio_dialog_tokens.xml`

| 语义 | Token / 说明 |
|------|----------------|
| 面板背景 | `player_aspect_dialog_panel_bg` ~ `rgba(12,16,28,0.72)` |
| 面板描边 | `player_aspect_dialog_stroke`，1dp 半透明白 |
| 面板圆角 | `player_aspect_dialog_corner`（约 28dp） |
| 面板最大宽度 | `player_aspect_dialog_max_width`，与窗口 `setLayout` 联用 |
| 行未选中底/框 | `player_aspect_row_*` + `drawable/player_aspect_ratio_row_unselected.xml` |
| 行选中高亮 | `player_aspect_ratio_row_selected.xml`（蓝边框 + 淡蓝底） |
| 文案颜色 | `player_aspect_row_label_normal` / `_selected` |
| 自定 radio | `ic_player_aspect_radio_off` / `_on`，禁止系统 Radio 主题 |
| 行间间距 | `player_aspect_option_spacing` |
| 列表区滚动上限 | `player_aspect_dialog_option_scroll_cap`，配合屏幕比例封顶 |

浅色主题：若需在非播放器场景复用，可另设 `values-night` 对等文件或拆分 `player_glass_*` 令牌；播放器当前路径以深色令牌为准。

---

## 3. 布局骨架（必读）

**外壳：** `app/src/main/res/layout/dialog_player_glass_sheet.xml`

- 根：`MaterialCardView`（半透明底、圆角、细描边、适度 `cardElevation`）
- 内：顶栏 **`player_glass_sheet_title`**（加粗白字）
- 其下 **`NestedScrollView` `player_glass_sheet_scroll`** 包裹单列容器 **`LinearLayout` `player_glass_sheet_option_list`**
- `NestedScrollView` 使用：`android:overScrollMode="ifContentScrolls"`（注意枚举拼写 **`ifContentScrolls`**，不可用 `if_content_scrolls`）

**行：** `app/src/main/res/layout/item_player_glass_sheet_row.xml`

- 根：**`LinearLayout`** `player_glass_sheet_row_root` — 不要用 `RadioButton`
- 左：`ImageView` `player_glass_sheet_radio`
- 右：`TextView` `player_glass_sheet_label`

---

## 4. Kotlin 接线模板（参考 [PlayerActivity.kt](../app/src/main/java/com/example/openvideo/ui/player/PlayerActivity.kt)）

公共方法（保持仅此一处扩展，避免重复造轮子）：

1. **`inflatePlayerGlassSheet(titleRes: Int)`**  
   → 返回 `(contentView, optionList, scrollView)`。

2. **`applyGlassSheetRowVisual(row: View, selected: Boolean)`**  
   → 绑定行背景、radio 图标、文字色、`translationZ`（选中微弱浮起）。

3. **`AlertDialog.applyPlayerGlassSheetChrome()`**  
   - `window` 透明背景，`decorView.alpha = 1f`（禁止与 `applyPlayerSheetStyle` 叠在同弹窗）。  
   - 宽度：`min(max_width_px, screenWidth * 0.9)`，高度 `WRAP_CONTENT`，`Gravity.CENTER`。  
   - `FLAG_DIM_BEHIND`，适度 `dimAmount`。  
   - API 31+：`Window.setBackgroundBlurRadius` ≈ **18dp** 换算像素（背后模糊）。

4. **`capPlayerGlassSheetScroll(scrollView, attempt)`**  
   - 测量 `option_list` 总高；超过 `min(屏高比例, scroll_cap_dimen)` 时固定 `NestedScrollView.height`，标题不滚动、仅列表滚动。  
   - 首轮 `scrollView.width==0` 时 `post` 重试若干次。

**标准调用顺序：**

```text
inflate → MaterialAlertDialogBuilder.setView(content).setOnDismissListener { scheduleHideControls() }.create()
→ 遍历向 option_list inflate 各行，设 margin/spacing，设点击回调（逻辑与旧版完全一致）
→ dialog.show()
→ dialog.applyPlayerGlassSheetChrome()
→ scroll.post { capPlayerGlassSheetScroll(scroll, 0) }
```

**禁用行（如快捷入口某项不可用）：** `isEnabled/isClickable/isFocusable = false`、`alpha` 降低、去掉 `foreground` 波纹、`applyGlassSheetRowVisual(..., false)`。

---

## 5. 与 `applyPlayerSheetStyle()` 的差异

| 项目 | Glass sheet | applyPlayerSheetStyle |
|------|-------------|------------------------|
| 用途 | 自定义 `setView` 整块不透明内容 | 旧式 Material 列表/简单容器 |
| `decorView.alpha` | **保持 1** | 随设置面板透明度变化 |
| 背后模糊 | 固定约 18dp（本系列） | 随 `PlayerSettingsSheetStylePolicy`/prefs |

**规则：** 新做的播放器居中「卡片 + 行间选项」一律走 Glass sheet API；不要为了省事再 `setItems + applyPlayerSheetStyle`。

---

## 6. 检查清单（PR / Code review）

- [ ] 布局是否使用 **`dialog_player_glass_sheet`** + **`item_player_glass_sheet_row`**？
- [ ] 颜色/圆角是否只改动 **tokens**，避免在 Activity 硬编码色值？
- [ ] `show()` 之后是否 **`applyPlayerGlassSheetChrome()`**？
- [ ] 是否 **`capPlayerGlassSheetScroll`**（选项可能变多或小屏）？
- [ ] 业务逻辑是否与改造前等价（prefs / ViewModel / 回调序列）？
- [ ] **未**对已使用 Glass sheet 的弹窗再调用 `applyPlayerSheetStyle`

---

## 7. 参考实现锚点（当前仓库）

- 令牌：`aspect_ratio_dialog_tokens.xml`
- 外壳行布局：`dialog_player_glass_sheet.xml`、`item_player_glass_sheet_row.xml`
- Row  drawable：`player_aspect_ratio_row_*.xml`、`ic_player_aspect_radio_*.xml`
- 示例：`showAspectRatioQuickDialog`、`showSpeedPickerDialog`、`showQuickEntryDialog`

如需把同一套外壳抽到 **共用类**（例如 `PlayerGlassSheetDialog.kt`），在遵守上述令牌与.chrome 语义的前提下重构即可。
