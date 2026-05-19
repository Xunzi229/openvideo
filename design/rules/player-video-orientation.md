# 播放器视频横竖屏规则（不可破坏）

> **状态：强制（Phase 0 稳定化）**  
> **问题背景：** 竖拍视频 MediaStore 常为编码尺寸 1920×1080 + `ORIENTATION=90`，若按原始宽高判横屏，会先横屏进入再被 `onVideoSizeChanged` 转成竖屏，产生可见闪屏。

---

## 1. 用户可见行为（验收标准）

1. **竖屏视频**：进入 `PlayerActivity` 后**直接**以竖屏 UI 播放，不得先横屏再自动转竖屏。
2. **横屏视频**：进入后**直接**以横屏 UI 播放。
3. **切歌 / 会话队列切换**：下一首视频进入前应用其显示尺寸预设方向；用户未手动改方向时，自动方向仍生效。
4. **用户点全屏按钮**：本次会话内自动方向暂停，直到切到下一首视频（`userOverrodeOrientation` 复位）。
5. **关闭「按视频自动方向」**（`playerPrefs.autoOrientationByVideo = false`）：始终默认横屏，不受视频尺寸影响。

---

## 2. 架构：唯一决策链（禁止分叉）

```
MediaStore 原始 WIDTH/HEIGHT + ORIENTATION
    → MediaStoreVideoDimensionsPolicy.displayDimensions()   // 扫描层，写入 VideoItem
    → Intent / SessionQueue 传递 display width/height
    → PlayerActivity.applyInitialVideoOrientation()         // onCreate，setContentView 前
        → PlayerOrientationPolicy.initialOrientationForVideo()
            → PlayerVideoLayoutPolicy.orientationForVideo() // 唯一阈值实现
    → ExoPlayer onVideoSizeChanged
        → PlayerVideoOrientationApplyPolicy.shouldApply()
        → PlayerVideoLayoutPolicy.orientationForVideo(含 rotation + PAR)
        → 仅当 target ≠ 当前 requestedOrientation 时才赋值
```

### 2.1 宽高比 → 方向（唯一阈值，勿复制）

| 显示宽高比 | `requestedOrientation` |
|------------|------------------------|
| ≥ 1.2 | `SCREEN_ORIENTATION_LANDSCAPE` |
| ≤ 0.8 | `SCREEN_ORIENTATION_PORTRAIT` |
| 中间 | `SCREEN_ORIENTATION_SENSOR` |
| 无效（≤0） | `SCREEN_ORIENTATION_UNSPECIFIED`（等解码回调，**禁止**默认锁横屏） |

实现位置：**仅** `PlayerVideoLayoutPolicy.orientationForVideo()`。  
`PlayerOrientationPolicy.orientationForVideo()` 必须委托上述方法，不得再写一套 ratio 判断。

### 2.2 显示尺寸 vs 编码尺寸

| 层级 | 规则 |
|------|------|
| **MediaStore 扫描** | `VideoScanner.readVideoItem()` 必须通过 `MediaStoreVideoDimensionsPolicy` 把 ORIENTATION（API 29+）换算为**显示宽高**再写入 `VideoItem.width/height`。禁止把原始 WIDTH/HEIGHT 直接当 UI 方向依据。 |
| **ExoPlayer 回调** | `onVideoSizeChanged` 使用 `PlayerVideoLayoutPolicy`，传入 `unappliedRotationDegrees` 与 `pixelWidthHeightRatio`，与布局/画面比例共用同一套 `displayFrameSize` 逻辑。 |
| **进入播放器** | `PlayerActivity.resolveInitialVideoDimensions()`：优先 `EXTRA_VIDEO_WIDTH/HEIGHT`，否则从 `sessionVideoQueue()` 按 `video_id` 取当前项。尺寸仍未知时走 `UNSPECIFIED`，等 `onVideoSizeChanged`。 |

---

## 3. 关键文件（改动需同步更新测试）

| 文件 | 职责 |
|------|------|
| `data/scanner/MediaStoreVideoDimensionsPolicy.kt` | MediaStore ORIENTATION → 显示宽高 |
| `data/scanner/VideoScanner.kt` | 投影含 ORIENTATION；写入 VideoItem 前归一化 |
| `ui/player/PlayerVideoLayoutPolicy.kt` | 显示帧、宽高比、**方向阈值**（含 rotation/PAR） |
| `ui/player/PlayerControlState.kt` → `PlayerOrientationPolicy` | 初始方向入口；auto 关时默认横屏 |
| `ui/player/PlayerVideoOrientationApplyPolicy.kt` | 是否允许自动改方向 |
| `ui/player/PlayerActivity.kt` | `applyInitialVideoOrientation`、`applyVideoOrientation`、`preApplyOrientationForItem`、`userOverrodeOrientation` |
| `AndroidManifest.xml` | `PlayerActivity` 的 `android:screenOrientation="unspecified"` |

---

## 4. 禁止事项（AI / 人工均不得违反）

1. ❌ 在 `PlayerActivity.onCreate` 或 Manifest 把播放器**默认锁横屏**或 **`screenOrientation="sensor"`**，导致进入时传感器抢先转屏。
2. ❌ 尺寸未知（0）且 `autoOrientationByVideo=true` 时 fallback 到 `SCREEN_ORIENTATION_LANDSCAPE`。
3. ❌ 在 Home/Scanner/Fragment 等处**复制** 1.2 / 0.8 阈值或手写 `width > height` 判方向。
4. ❌ 跳过 `MediaStoreVideoDimensionsPolicy`，用 MediaStore 原始宽高启动播放器。
5. ❌ `onVideoSizeChanged` 无条件 `requestedOrientation = …`（必须先比较，相同则 no-op）。
6. ❌ 用户手动全屏后仍被 `onVideoSizeChanged` 覆盖（必须尊重 `userOverrodeOrientation`）。
7. ❌ 切下一首视频时不复位 `userOverrodeOrientation`、不调用 `preApplyOrientationForItem`。

---

## 5. 允许事项

- ✅ 调整 **1.2 / 0.8** 阈值：只改 `PlayerVideoLayoutPolicy`，并更新对应单测。
- ✅ 新增启动入口：必须传 `EXTRA_VIDEO_WIDTH/HEIGHT`（来自已归一化的 `VideoItem`）或保证 session queue 项含正确 display 尺寸。
- ✅ 退出播放器时 `settleOrientationBeforeExit` 锁竖屏：仅用于退出动画，与进入逻辑无关。

---

## 6. 必跑测试（改上述任一文件后）

```bash
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.data.scanner.MediaStoreVideoDimensionsPolicyTest" --tests "com.example.openvideo.ui.player.PlayerOrientationPolicyTest" --tests "com.example.openvideo.ui.player.PlayerVideoLayoutPolicyTest" --tests "com.example.openvideo.ui.player.PlayerVideoOrientationApplyPolicyTest"
```

真机：竖拍一条 + 横屏一条，确认无「先横后竖」；队列切歌各测一次。

---

## 7. 变更流程

1. 先读本文 + 跑第 6 节测试。
2. 若必须改行为，在本文件末尾追加 **Changelog** 一行并更新阈值/文件表。
3. 禁止「临时修复」式在 `PlayerActivity` 内联方向逻辑绕过 Policy。

### Changelog

| 日期 | 说明 |
|------|------|
| 2026-05-19 | 初版：MediaStore 显示尺寸归一化 + 进入前预设方向 + 禁止未知尺寸默认横屏 + Manifest unspecified |
