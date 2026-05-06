# OpenVideo 全功能实现计划

> 基于 `2026-05-05-openvideo-full-feature-design.md` 设计规范。

---

## Phase 1：基础架构（Foundation）

> 所有后续功能的基石。必须最先完成。

### Task 1.1：创建 CompatUtil 兼容工具类

**目标**：封装 Android 版本判断和 API 降级逻辑，供所有功能模块调用。

**文件**：
- 新建 `app/src/main/java/com/example/openvideo/core/compat/CompatUtil.kt`
- 新建 `app/src/main/java/com/example/openvideo/core/compat/CompatUtilTest.kt`（单元测试）

**内容**：
```kotlin
object CompatUtil {
    // 版本判断
    fun isAtLeastApi(api: Int): Boolean = Build.VERSION.SDK_INT >= api
    fun isPiPSupported(): Boolean = isAtLeastApi(26)
    fun isNotificationPermissionNeeded(): Boolean = isAtLeastApi(33)
    fun isReadMediaVideoNeeded(): Boolean = isAtLeastApi(33)
    fun isVideoEffectsSupported(): Boolean = isAtLeastApi(29)
    fun isForegroundServiceTypeNeeded(): Boolean = isAtLeastApi(29)
    fun isWindowInsetsApiAvailable(): Boolean = isAtLeastApi(30)
    fun isCutoutApiAvailable(): Boolean = isAtLeastApi(28)

    // 功能开关
    fun isFeatureSupported(feature: String): Boolean
}
```

**验证**：编译通过，单元测试通过。

---

### Task 1.2：创建 VendorCompat 厂商兼容工具类

**目标**：封装厂商检测、后台限制引导、设置页跳转逻辑。

**文件**：
- 新建 `app/src/main/java/com/example/openvideo/core/compat/VendorCompat.kt`

**内容**：
```kotlin
object VendorCompat {
    enum class Vendor { XIAOMI, HUAWEI, OPPO, VIVO, SAMSUNG, ONEPLUS, MEIZU, UNKNOWN }

    fun detectVendor(): Vendor  // 基于 Build.MANUFACTURER + Build.BRAND
    fun needsBackgroundPermissionGuide(): Boolean
    fun getBackgroundSettingsIntent(context: Context): Intent?
    fun showBackgroundPermissionDialog(activity: Activity)
    fun isEmulator(): Boolean  // Build.FINGERPRINT 包含 generic/sdk
}
```

**验证**：编译通过。

---

### Task 1.3：创建 PrefsManager 统一偏好管理

**目标**：将分散的 SharedPreferences 调用统一到一个管理类中，支持类型安全的 key 管理。

**文件**：
- 新建 `app/src/main/java/com/example/openvideo/core/prefs/PrefsManager.kt`
- 新建 `app/src/main/java/com/example/openvideo/core/prefs/PlayerPrefs.kt`
- 新建 `app/src/main/java/com/example/openvideo/core/prefs/AppPrefs.kt`

**PlayerPrefs 内容**（从 PlayerSettingsDialog 迁移）：
```kotlin
class PlayerPrefs(context: Context) {
    // P0 - 播放
    var speed: Float           // 默认 1.0f
    var loopMode: LoopMode     // OFF/SINGLE/LIST
    var seekInterval: Int      // 5/10/15
    var rememberProgress: Boolean
    var autoPlayNext: Boolean
    var hwAcceleration: Boolean
    var pauseOnExit: Boolean
    var bgAudio: Boolean
    var skipIntroOutro: Boolean
    var introSeconds: Int
    var outroSeconds: Int
    var keepScreenOn: Boolean
    var controlsAutoHide: Int  // 3/5/8/0(不隐藏)

    // P0 - 画面
    var aspectRatio: AspectRatio  // FIT/FILL/CROP/STRETCH/4_3/16_9
    var rotation: Int             // 0/90/180/270
    var mirror: Boolean

    // P0 - 声音
    var speedPreservePitch: Boolean
    var volumeBoost: Boolean
    var audioChannel: AudioChannel  // STEREO/LEFT/RIGHT

    // P0 - 字幕
    var subtitleSize: Int
    var subtitleColor: Int
    var subtitleBgStyle: SubtitleBgStyle
    var subtitlePosition: Float
    var subtitleEncoding: String
    var audioDelay: Int  // ms

    // P0 - 手势
    var leftVerticalGesture: GestureAction
    var rightVerticalGesture: GestureAction
    var doubleTapAction: DoubleTapAction
    var longPressAction: LongPressAction
    var horizontalSwipeAction: GestureAction
    var gestureSensitivity: Int  // 1/2/3

    // P1 - 增强（预留）
    var doubleTapSeconds: Int
    var longPressSpeed: Float
    var swipeRange: Int
    var edgeSwipeBack: Boolean
}
```

**AppPrefs 内容**：
```kotlin
class AppPrefs(context: Context) {
    var themeMode: ThemeMode  // DARK/LIGHT/SYSTEM
    var language: String      // "system"/"zh"/"en"
    var defaultAspectRatio: AspectRatio
    var defaultSpeed: Float
    var brightness: Float     // 窗口亮度，0.01~1.0
}
```

**验证**：编译通过，PlayerPrefs 能正确读写所有 key。

---

### Task 1.4：集成 WindowSizeClass 页面自适应

**目标**：在 MainActivity 中集成 WindowSizeClass，为断点布局做准备。

**文件**：
- 修改 `app/build.gradle.kts`（添加 `androidx.window:window` 依赖）
- 修改 `app/src/main/java/com/example/openvideo/ui/MainActivity.kt`

**内容**：
- 添加依赖：`androidx.window:window:1.2.0`
- 在 MainActivity 中计算 `WindowSizeClass`
- 将 `WindowSizeClass` 传递给 Fragment（通过 ViewModel 或 arguments）
- 创建 `ScreenBreakpoint` 枚举：`COMPACT / MEDIUM / EXPANDED`

**验证**：编译通过，旋转屏幕时 breakpoint 正确更新。

---

## Phase 2：播放器设置 UI（Player Settings Dialog）

> 在现有 PlayerSettingsDialog 基础上，补全 6 个分组的 UI。

### Task 2.1：补全画面设置 UI

**目标**：在设置对话框的"画面"分组中添加 3 个设置项。

**文件**：
- 修改 `app/src/main/java/com/example/openvideo/ui/player/PlayerSettingsDialog.kt`
- 修改 `app/src/main/res/layout/dialog_player_settings.xml`

**内容**：
- 画面比例：单选列表（适应/填充/裁剪/拉伸/4:3/16:9），点击弹出选择对话框
- 画面旋转：单选列表（0°/90°/180°/270°）
- 画面镜像：开关

**验证**：打开设置对话框，画面分组显示 3 个设置项，切换值后 SharedPreferences 正确更新。

---

### Task 2.2：补全声音设置 UI

**文件**：同 Task 2.1

**内容**：
- 倍速不变调：开关
- 音量增强：开关
- 音轨选择：单选列表（动态读取当前视频的音轨列表）
- 声道选择：单选列表（立体声/左声道/右声道）

**验证**：声音分组显示 4 个设置项。

---

### Task 2.3：补全字幕设置 UI

**文件**：同 Task 2.1

**内容**：
- 加载外挂字幕：文件选择器按钮（`ACTION_OPEN_DOCUMENT`，MIME `application/x-subrip` + `text/vtt` + `application/ttml+xml`）
- 字幕轨选择：单选列表（动态：关闭/内嵌1/外挂1/...）
- 字幕大小：SeekBar 滑块（14~32sp）
- 字幕颜色：颜色选择器（预设白/黄/青/绿/红 + 自定义）
- 字幕背景：单选列表（无/半透明黑/全黑/自定义）
- 字幕位置：SeekBar 滑块
- 字幕编码：单选列表（自动/UTF-8/GBK/GB2312/Big5/Shift_JIS/EUC-KR）
- 声音提前/延后：±按钮 + 数字显示

**验证**：字幕分组显示 8 个设置项。

---

### Task 2.4：补全手势设置 UI

**文件**：同 Task 2.1

**内容**：
- 左侧上下滑动：单选列表（调节亮度/无操作）
- 右侧上下滑动：单选列表（调节音量/无操作）
- 双击操作：单选列表（播放暂停/快进10秒/快退10秒/无操作）
- 长按操作：单选列表（倍速播放/无操作）
- 水平滑动：单选列表（快进快退/无操作）
- 手势灵敏度：低/中/高三档

**验证**：手势分组显示 6 个设置项。

---

### Task 2.5：补全其他设置 UI

**文件**：同 Task 2.1

**内容**：
- 记住播放进度：开关（已有，确认连接 PlayerPrefs）
- 自动播放下一个：开关（已有）
- 循环模式：单选列表（已有）
- 屏幕常亮：开关
- 控制栏自动隐藏：单选列表（3秒/5秒/8秒/不自动隐藏）
- 跳过片头片尾：开关 + 两个数字输入（片头X秒/片尾X秒）

**验证**：其他分组显示 6 个设置项，所有值正确持久化。

---

### Task 2.6：迁移现有设置到 PlayerPrefs

**目标**：将 PlayerSettingsDialog 中硬编码的 SharedPreferences 调用替换为 PlayerPrefs。

**文件**：
- 修改 `PlayerSettingsDialog.kt`
- 修改 `PlayerActivity.kt`（读取设置的地方）
- 修改 `PlayerViewModel.kt`

**验证**：设置对话框读写 PlayerPrefs，切换后值正确保持。

---

## Phase 3：播放器引擎对接（Player Engine Wiring）

> 将设置值实际应用到播放器行为。
>
> 范围修正：设置页不能只保存 `PlayerPrefs` 就标记完成。每个设置项必须明确归类为：
> - 已接线：当前播放器实例会立即消费设置值。
> - 待引擎支持：需要播放队列、AudioProcessor、TrackSelector 或平台能力后再开放。
> - 预留：仅保留数据结构，不在 UI 中承诺已可用。
>
> 当前已接线基础项：倍速/倍速不变调、循环模式、音量增强、画面 resize mode、旋转、镜像、字幕大小/颜色/背景/位置、屏幕常亮、控制栏自动隐藏、后台音频暂停策略、基础跳片头。
>
> 当前仍应标记为待引擎支持：自动播放下一个、真实软/硬解切换、声道选择、音频延迟补偿、音轨选择、4:3/16:9 强制画幅、跳片尾进入下一集。

### Task 3.1：画面设置对接

**目标**：画面比例、旋转、镜像的设置值实际控制播放器。

**文件**：
- 修改 `app/src/main/java/com/example/openvideo/core/player/PlayerManager.kt`
- 修改 `app/src/main/java/com/example/openvideo/ui/player/PlayerActivity.kt`
- 修改 `app/src/main/res/layout/activity_player.xml`
- 修改 `app/src/main/res/layout-land/activity_player.xml`

**内容**：
- `AspectRatioFrameLayout.setResizeMode()` 根据 PlayerPrefs.aspectRatio 切换
- `PlayerView.setRotation()` 根据 PlayerPrefs.rotation 切换
- 镜像通过 `Matrix.setScale(-1, 1, centerX, centerY)` 实现
- 设置变更时实时更新，不需要重建播放器

**验证**：切换画面比例/旋转/镜像后，视频画面立即响应。

---

### Task 3.2：声音设置对接

**文件**：
- 修改 `PlayerManager.kt`
- 修改 `PlayerActivity.kt`

**内容**：
- `player.setPlaybackParameters(PlaybackParameters(speed, pitch))` 实现倍速不变调
- `player.volume` 实现音量增强（>1.0f 时 soft clipping）
- `DefaultAudioSink` 通道映射实现声道选择
- Audio Focus 请求/释放逻辑
- 音频延迟补偿（`AudioProcessor` 偏移）

**验证**：倍速、音量增强、声道切换实时生效。

---

### Task 3.3：字幕系统实现

**目标**：实现外挂字幕加载、解析、渲染。

**文件**：
- 新建 `app/src/main/java/com/example/openvideo/core/subtitle/SrtParser.kt`
- 新建 `app/src/main/java/com/example/openvideo/core/subtitle/AssParser.kt`
- 新建 `app/src/main/java/com/example/openvideo/core/subtitle/VttParser.kt`
- 新建 `app/src/main/java/com/example/openvideo/core/subtitle/SubtitleLoader.kt`
- 新建 `app/src/main/java/com/example/openvideo/core/subtitle/CharsetDetector.kt`
- 修改 `PlayerActivity.kt`
- 修改 `PlayerManager.kt`

**内容**：
- `SubtitleLoader`：扫描同目录同名字幕文件，自动发现
- `CharsetDetector`：BOM 检测 → chardet → 默认 UTF-8
- `SrtParser`：正则解析序号+时间轴+文本
- `AssParser`：解析 Script Info/Styles/Events 三段
- `VttParser`：WebVTT 标准解析
- ExoPlayer `SubtitleView` 样式配置（大小/颜色/背景/位置）
- 字幕轨选择通过 `TrackSelectionOverride`

**验证**：播放带同名 .srt 文件的视频，字幕正确显示。切换字幕大小/颜色实时生效。

---

### Task 3.4：手势系统重构

**目标**：将手势逻辑从硬编码改为由设置驱动。

**文件**：
- 修改 `PlayerActivity.kt`（手势处理部分）

**内容**：
- 手势识别器状态机：IDLE → DETECTING → HORIZONTAL / VERTICAL_LEFT / VERTICAL_RIGHT
- 各手势行为根据 PlayerPrefs 配置决定执行什么操作
- 左侧上下：根据设置 = 亮度调节 或 无操作
- 右侧上下：根据设置 = 音量调节 或 无操作
- 双击：根据设置 = 播放暂停 / 快进 / 快退 / 无
- 长按：根据设置 = 倍速 / 无
- 水平：根据设置 = 快进快退 / 无
- 灵敏度参数从 PlayerPrefs 读取

**验证**：修改手势设置后，手势行为立即变化。

---

### Task 3.5：播放引擎核心逻辑对接

**目标**：对接 seek_interval、remember_progress、auto_play_next、loop_mode、keep_screen_on、controls_auto_hide、skip_intro_outro。

**文件**：
- 修改 `PlayerManager.kt`
- 修改 `PlayerActivity.kt`
- 修改 `PlayerViewModel.kt`

**内容**：
- `seekForward/seekBackward` 从 SharedPreferences 读取 seek_interval
- `onCreate` 从 Room 读取 lastPosition 并 seek（显示 Snackbar）
- `onPause/onDestroy` 保存进度到 Room
- auto_play_next + loop_mode：播放完成时根据设置决定行为
- `window.addFlags(FLAG_KEEP_SCREEN_ON)` 根据 keep_screen_on
- `Handler.postDelayed` 根据 controls_auto_hide 设置延迟
- skip_intro_outro：`onPlaybackStateChanged(STATE_READY)` 时 seek 到 introSeconds
- 片尾：监听位置，到达 `duration - outroSeconds` 时触发下一个

**验证**：
- 播放10秒后退出，重新打开应从10秒继续
- 快进快退步长与设置一致
- 控制栏自动隐藏时间与设置一致

---

### Task 3.6：解码器自动回退实现

**目标**：实现硬件解码 → 软件解码的自动回退链。

**文件**：
- 修改 `PlayerManager.kt`
- 新建 `app/src/main/java/com/example/openvideo/core/player/DecoderStrategy.kt`

**内容**：
- `DecoderStrategy`：封装 `MediaCodecList` 探测 + 缓存
- 解码能力缓存到本地文件（JSON 格式）
- `PlayerManager` 创建播放器时使用 `DecoderStrategy` 选择解码器
- `onPlayerError` 中 `MediaCodecDecoderException` → 自动切换软解并重试
- 连续 3 次失败 → 提示不支持

**验证**：播放一个编码不受支持的视频时，自动回退到软解。

---

## Phase 4：文件列表功能增强

### Task 4.1：右键菜单实现

**目标**：视频列表项的"更多"按钮弹出操作菜单。

**文件**：
- 修改 `app/src/main/java/com/example/openvideo/ui/home/VideoGridAdapter.kt`
- 修改 `app/src/main/java/com/example/openvideo/ui/home/HomeFragment.kt`
- 新建 `app/src/main/java/com/example/openvideo/ui/home/VideoOptionsSheet.kt`（BottomSheet）

**内容**：
- BottomSheet 菜单项：播放 / 收藏 / 添加到播放列表 / 查看详情 / 分享 / 删除 / 重命名
- 收藏：调用 `VideoRepository.toggleFavorite()`
- 查看详情：底部面板显示文件名、路径、分辨率、时长、大小、编码、修改时间
- 分享：`ACTION_SEND` Intent
- 删除：二次确认 → `ContentResolver.delete()`
- 重命名：内联编辑框 → `ContentResolver.update()`

**验证**：长按视频项弹出菜单，各菜单项功能正常。

---

### Task 4.2：搜索功能实现

**文件**：
- 修改 `fragment_home.xml`（添加 SearchView）
- 修改 `HomeFragment.kt`
- 修改 `VideoGridAdapter.kt`（实现 Filterable）

**内容**：
- 顶部 SearchView，实时过滤标题
- `Filterable` 接口，协程中执行过滤
- 空结果显示"未找到匹配视频"

**验证**：输入关键词，列表实时过滤。

---

### Task 4.3：排序功能实现

**文件**：
- 修改 `HomeFragment.kt`
- 新建 `app/src/main/java/com/example/openvideo/ui/home/VideoSorter.kt`

**内容**：
- 排序按钮弹出选择：按名称/日期/大小/时长 × 升序/降序
- `VideoComparator` 工厂类
- `adapter.submitList(sortedList)` + DiffUtil 动画

**验证**：切换排序方式后列表立即重排。

---

### Task 4.4：网格/列表视图切换

**文件**：
- 修改 `HomeFragment.kt`
- 修改 `fragment_home.xml`
- 新建 `app/src/main/res/layout/item_video_grid.xml`

**内容**：
- 列表按钮 → `LinearLayoutManager`
- 网格按钮 → `GridLayoutManager(2)`（Compact）/ `GridLayoutManager(3)`（Medium）/ `GridLayoutManager(4)`（Expanded）
- 网格项：缩略图卡片 + 标题 + 时长
- 切换时保存偏好到 AppPrefs

**验证**：切换视图模式后布局立即变化。

---

## Phase 5：软件设置页面

### Task 5.1：创建设置 Fragment

**文件**：
- 新建 `app/src/main/java/com/example/openvideo/ui/settings/SettingsFragment.kt`
- 新建 `app/src/main/java/com/example/openvideo/ui/settings/SettingsViewModel.kt`
- 新建 `app/src/main/res/layout/fragment_settings.xml`

**内容**：
- 通用分组：主题模式、语言
- 播放器分组：默认画面比例、默认倍速
- 存储分组：缩略图缓存大小 + 清除按钮、播放历史条数 + 清除按钮
- 关于分组：版本号、开源许可、GitHub 地址

**验证**：从底部导航进入设置页，各设置项可操作。

---

### Task 5.2：设置页接入导航

**文件**：
- 修改 `MainActivity.kt`
- 修改 `bottom_nav_menu.xml`
- 修改 `nav_graph.xml`（如有）

**内容**：
- "我的" tab 改为指向 SettingsFragment（或在"我的"中添加"设置"入口）
- 导航逻辑接入

**验证**：点击导航可进入设置页。

---

## Phase 6：P1 功能实现

### Task 6.1：播放列表数据模型

**文件**：
- 新建 `app/src/main/java/com/example/openvideo/data/local/PlaylistEntity.kt`
- 新建 `app/src/main/java/com/example/openvideo/data/local/PlaylistVideoEntity.kt`
- 新建 `app/src/main/java/com/example/openvideo/data/local/PlaylistDao.kt`
- 修改 `VideoDatabase.kt`（添加新表，版本升级到 2）

**内容**：
- `PlaylistEntity(id, name, createdAt, updatedAt)`
- `PlaylistVideoEntity(playlistId, videoId, position)`
- `PlaylistDao`：CRUD + 拖拽排序 + 批量操作

**验证**：Room 数据库编译通过，DAO 测试通过。

---

### Task 6.2：播放列表 UI

**文件**：
- 新建 `app/src/main/java/com/example/openvideo/ui/playlist/PlaylistFragment.kt`
- 新建 `app/src/main/java/com/example/openvideo/ui/playlist/PlaylistViewModel.kt`
- 新建 `app/src/main/java/com/example/openvideo/ui/playlist/PlaylistDetailFragment.kt`
- 修改 `bottom_nav_menu.xml`

**内容**：
- 播放列表列表页：创建/编辑/删除/排序
- 播放列表详情页：视频列表 + 拖拽排序 + 移除
- M3U 导入/导出

**验证**：可创建播放列表，添加视频，拖拽排序。

---

### Task 6.3：P1 播放器增强 — 滤镜/均衡器/截图

**文件**：
- 修改 `PlayerActivity.kt`
- 修改 `PlayerManager.kt`

**内容**：
- 视频滤镜：`ExoPlayer.setVideoEffects()` + OpenGL shader
- 均衡器：`AudioEffect` 框架的 `Equalizer` 绑定 AudioSessionId
- 截图：`PixelCopy` API 保存到 `Pictures/OpenVideo/`

**验证**：切换滤镜画面实时变化，均衡器预设生效，截图保存成功。

---

### Task 6.4：P1 字幕增强 — 字体/描边/双字幕

**文件**：
- 修改 `PlayerActivity.kt`

**内容**：
- 字幕字体：`Typeface.createFromFile()` 加载自定义字体
- 描边/阴影：`Paint.setStrokeWidth()` + `setShadowLayer()`
- 双字幕：两个 `SubtitleView` 层叠

**验证**：字幕样式实时变化，双字幕同时显示。

---

### Task 6.5：P1 手势增强 — 双击秒数/长按倍速/边缘返回

**文件**：
- 修改 `PlayerActivity.kt`

**内容**：
- 双击秒数从 PlayerPrefs 读取
- 长按倍速值从 PlayerPrefs 读取
- 边缘手势判定（5% 宽度内 = 返回）

**验证**：修改设置后手势行为变化。

---

### Task 6.6：P1 文件列表 — 多选/文件夹浏览

**文件**：
- 修改 `HomeFragment.kt`
- 修改 `VideoGridAdapter.kt`

**内容**：
- 长按进入多选模式 + ActionMode 操作栏
- 按 `RELATIVE_PATH` 分组的文件夹浏览
- 批量收藏/删除/添加到列表

**验证**：多选模式可批量操作，文件夹浏览正常。

---

## Phase 7：P2 功能实现

### Task 7.1：AB 循环

**文件**：修改 `PlayerActivity.kt`

**内容**：设置 A/B 点，SeekBar 高亮区间，位置监听循环 seek。

---

### Task 7.2：画中画 (PiP)

**文件**：修改 `PlayerActivity.kt`，修改 `AndroidManifest.xml`

**内容**：`enterPictureInPictureMode()` + PiP 控件适配。

---

### Task 7.3：MediaSession + 通知栏控制

**文件**：
- 新建 `app/src/main/java/com/example/openvideo/core/player/MediaSessionManager.kt`
- 新建 `app/src/main/java/com/example/openvideo/core/player/PlaybackService.kt`

**内容**：
- `MediaSessionCompat` 创建和回调
- `MediaStyle` 通知栏
- 耳机线控 / 蓝牙 AVRCP
- `ACTION_AUDIO_BECOMING_NOISY` 广播监听

---

### Task 7.4：屏幕锁定

**文件**：修改 `PlayerActivity.kt`

**内容**：
- 锁定按钮 → 屏蔽触摸/返回/音量
- 解锁图标 + 确认对话框

---

### Task 7.5：隐私文件夹

**文件**：
- 新建 `app/src/main/java/com/example/openvideo/ui/privacy/PrivacyFragment.kt`

**内容**：
- `EncryptedSharedPreferences` 存储隐藏路径
- 密码/指纹验证（`BiometricPrompt`）

---

## 依赖关系

```
Phase 1 (Foundation)
  ├── Task 1.1 CompatUtil
  ├── Task 1.2 VendorCompat
  ├── Task 1.3 PlayerPrefs
  └── Task 1.4 WindowSizeClass
         │
Phase 2 (Settings UI) ──── depends on Phase 1
  ├── Task 2.1~2.5 各分组 UI
  └── Task 2.6 迁移到 PlayerPrefs
         │
Phase 3 (Engine Wiring) ── depends on Phase 1 + Phase 2
  ├── Task 3.1 画面对接
  ├── Task 3.2 声音对接
  ├── Task 3.3 字幕系统
  ├── Task 3.4 手势重构
  ├── Task 3.5 核心逻辑对接
  └── Task 3.6 解码器回退
         │
Phase 4 (File List) ─────── depends on Phase 1
  ├── Task 4.1 右键菜单
  ├── Task 4.2 搜索
  ├── Task 4.3 排序
  └── Task 4.4 网格/列表
         │
Phase 5 (App Settings) ──── depends on Phase 1
  ├── Task 5.1 SettingsFragment
  └── Task 5.2 导航接入
         │
Phase 6 (P1) ────────────── depends on Phase 3
  ├── Task 6.1 播放列表数据
  ├── Task 6.2 播放列表 UI
  ├── Task 6.3 滤镜/均衡器/截图
  ├── Task 6.4 字幕增强
  ├── Task 6.5 手势增强
  └── Task 6.6 多选/文件夹
         │
Phase 7 (P2) ────────────── depends on Phase 6
  ├── Task 7.1 AB 循环
  ├── Task 7.2 PiP
  ├── Task 7.3 MediaSession
  ├── Task 7.4 屏幕锁定
  └── Task 7.5 隐私文件夹
```

## 预估工时

| Phase | 任务数 | 预估 |
|-------|--------|------|
| Phase 1 基础架构 | 4 | 2~3 天 |
| Phase 2 设置 UI | 6 | 2~3 天 |
| Phase 3 引擎对接 | 6 | 4~5 天 |
| Phase 4 文件列表 | 4 | 2~3 天 |
| Phase 5 软件设置 | 2 | 1 天 |
| Phase 6 P1 | 6 | 4~5 天 |
| Phase 7 P2 | 5 | 3~4 天 |
| **总计** | **33** | **18~24 天** |
