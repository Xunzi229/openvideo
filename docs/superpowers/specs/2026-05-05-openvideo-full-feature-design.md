# OpenVideo 全功能设计规范

> 纯离线 Android 视频播放器的功能、逻辑与兼容性设计。聚焦"看不见但决定体验"的底层逻辑。

---

## 目录

1. [P0 — 播放器设置：画面](#一播放器设置--画面)
2. [P0 — 播放器设置：声音](#二播放器设置--声音)
3. [P0 — 播放器设置：字幕](#三播放器设置--字幕)
4. [P0 — 播放器设置：手势](#四播放器设置--手势)
5. [P0 — 播放器设置：其他](#五播放器设置--其他)
6. [P0 — 播放引擎核心逻辑](#六播放引擎核心逻辑)
7. [P0 — 文件列表右键菜单](#七文件列表--右键菜单)
8. [P0 — 软件设置](#八软件设置应用级)
9. [P1 — 播放器增强](#p1--播放器增强)
10. [P1 — 文件列表增强](#p1--文件列表增强)
11. [P1 — 播放列表管理](#p1--播放列表管理)
12. [P2 — 高级功能](#p2--高级功能)
13. [页面自适应设计](#页面自适应设计)
14. [架构兼容设计](#架构兼容设计x86--arm)
15. [Android 厂商兼容设计](#android-厂商兼容设计)

---

## P0 — 必做（核心体验）

### 一、播放器设置 — 画面

#### UI 设置项

| 功能项 | 类型 | 可选值 | 说明 |
|--------|------|--------|------|
| 画面比例 | 单选列表 | 适应(Fit) / 填充(Fill) / 裁剪(Crop) / 拉伸(Stretch) / 4:3 / 16:9 | 适应=等比缩放黑边；填充=等比裁剪铺满；拉伸=变形铺满 |
| 画面旋转 | 单选列表 | 0° / 90° / 180° / 270° | 修正拍摄方向错误的视频 |
| 画面镜像 | 开关 | 开 / 关 | 水平翻转画面，用于舞蹈教学等场景 |

#### 底层逻辑

- **渲染引擎选择**：默认使用 `SurfaceView`（独立窗口合成，性能好，不跟随 View 动画）。当用户截图或需要画面叠加效果时自动切换到 `TextureView`。渲染模式存储在设置中，切换时重建播放器 Surface。
- **画面比例实现**：通过 `AspectRatioFrameLayout` 的 `setResizeMode()` 实现。4:3 和 16:9 模式下计算目标比例，限制 View 裁剪区域。裁剪模式下使用 `Matrix` 变换缩放中心区域。
- **旋转实现**：通过 `PlayerView` 的 `setRotation()` 结合 `Activity.setRequestedOrientation()` 实现。旋转时需要重新计算 Surface 尺寸，避免画面拉伸闪烁。90°/270° 时自动切换为横屏布局。
- **帧率匹配**：检测视频原始帧率（如 24fps 电影），在支持的设备上自动请求显示刷新率切换（如切换到 24Hz/48Hz），消除 3:2 pulldown 抖动。此功能默认开，设置中不暴露，遇到设备不支持时静默回退。

---

### 二、播放器设置 — 声音

#### UI 设置项

| 功能项 | 类型 | 可选值 | 说明 |
|--------|------|--------|------|
| 倍速不变调 | 开关 | 开 / 关 | 变速时保持原音调，默认开 |
| 音量增强 | 开关 | 开 / 关 | 突破系统音量上限，软件增益，默认关 |
| 音轨选择 | 单选列表 | 自动 / 音轨1 / 音轨2 / ... | 多音轨视频切换 |
| 声道选择 | 单选列表 | 立体声 / 左声道 / 右声道 | 早期视频常有国粤双声道 |

#### 底层逻辑

- **Audio Focus 管理**：播放时请求 `AUDIOFOCUS_GAIN`。来电/通知时：来电→暂停并记录状态，通话结束后自动恢复；通知→短暂降低音量（duck 到 20%），结束后恢复。离开播放页时根据"暂停"设置决定是否释放焦点。
- **音频会话**：使用固定 `AudioSessionId`，关联到 `Visualizer`（预留频谱分析）和音效框架。播放器重建时保持同一 SessionId 避免音频中断。
- **音量增强实现**：通过 `ExoPlayer.setVolume(1.5f)` 软件增益，上限设为 2.0f。超过 1.0f 时在 UI 上显示警告标记（可能失真）。内部对 PCM 数据做 soft clipping 防止爆音。
- **声道选择实现**：通过 `AudioProcessor` 通道映射实现。左声道=复制左到双通道、右声道=复制右到双通道。切换时不需要重建播放器，通过 `DefaultAudioSink` 的通道映射实时切换。
- **音频延迟补偿**：检测到音画不同步时（视频帧时间戳与音频时间戳差值超过阈值），自动进行 A/V sync 调整。保留手动微调入口（±500ms，步进 25ms），在字幕设置中作为"声音提前/延后"暴露。
- **后台音频**：开启后，Activity 进入后台时启动前台 Service 通知栏控制（播放/暂停/上一个/下一个），保持音频流不中断。通知栏使用 `MediaStyle` 通知，关联 MediaSession。

---

### 三、播放器设置 — 字幕

#### UI 设置项

| 功能项 | 类型 | 可选值 | 说明 |
|--------|------|--------|------|
| 加载外挂字幕 | 文件选择器 | — | 支持 .srt / .ass / .ssa / .vtt |
| 字幕轨选择 | 单选列表 | 关闭 / 内嵌1 / 内嵌2 / 外挂1 / ... | 统一管理内嵌+外挂 |
| 字幕大小 | 滑块 | 14sp ~ 32sp，步进2sp | 默认18sp |
| 字幕颜色 | 颜色选择器 | 白/黄/青/绿/红/自定义 | 默认白色 |
| 字幕背景 | 单选+透明度 | 无 / 半透明黑 / 全黑 / 自定义颜色+透明度 | 默认半透明黑 |
| 字幕位置 | 滑块 | 底部~中部，百分比 | 默认最底部 |
| 字幕编码 | 单选列表 | 自动 / UTF-8 / GBK / GB2312 / Big5 / Shift_JIS / EUC-KR | 中文用户刚需 |
| 声音提前/延后 | 微调器 | ±500ms，步进25ms | 音画不同步时手动补偿 |

#### 底层逻辑

- **外挂字幕自动发现**：打开视频时，自动扫描同目录下同名文件（如 `video.srt`、`video.chs.srt`、`video.ass`）。优先级：同名 > 同名.语言标签 > 手动加载。扫描结果静默加载，不弹提示。
- **字幕解析引擎**：
  - SRT：正则解析序号、时间轴、文本，支持 `<b><i><u>` 标签和 `{\\pos(x,y)}` 样式覆盖
  - ASS/SSA：解析 `[Script Info]`、`[V4+ Styles]`、`[Events]` 三个段，提取样式定义（字体、大小、颜色、描边、阴影、位置），渲染时应用 override tags（`\\an`, `\\pos`, `\\fad`, `\\bord`, `\\shad`, `\\c&H...&`）
  - VTT：WebVTT 标准解析，支持 cue settings（position, align, line, size）
- **字幕编码检测**：读取文件前 8KB，按优先级检测：BOM 标记 → `chardet` 库自动检测 → 默认 UTF-8。检测失败时回退到用户手动选择的编码。
- **字幕渲染**：使用 ExoPlayer 的 `SubtitleView` + 自定义 `SubtitlePainter`。ASS 字幕需要自定义渲染器，支持 `\pos` 绝对定位、`\fad` 淡入淡出、`\an` 对齐点。字幕样式变更实时生效，不需要重建播放器。
- **字幕时间轴同步**：支持手动提前/延后（±500ms），调整后重新计算所有 cue 的显示时间。支持 ASS 文件中的 `Timer` 缩放因子。
- **多字幕轨切换**：ExoPlayer 的 `TrackSelectionOverride` 实现。切换时记录用户选择，下次打开同一视频时自动应用。

---

### 四、播放器设置 — 手势

#### UI 设置项

| 功能项 | 类型 | 可选值 | 说明 |
|--------|------|--------|------|
| 左侧上下滑动 | 单选列表 | 调节亮度 / 无操作 | 默认亮度 |
| 右侧上下滑动 | 单选列表 | 调节音量 / 无操作 | 默认音量 |
| 双击操作 | 单选列表 | 播放暂停 / 快进10秒 / 快退10秒 / 无操作 | 默认播放暂停 |
| 长按操作 | 单选列表 | 倍速播放 / 无操作 | 默认倍速播放 |
| 水平滑动 | 单选列表 | 快进快退 / 无操作 | 默认快进快退 |
| 手势灵敏度 | 滑块 | 低 / 中 / 高 | 影响滑动距离与数值变化的比例，默认中 |

#### 底层逻辑

- **手势识别器**：使用自定义 `GestureDetector` + `VelocityTracker`，不用系统 `ScaleGestureDetector`（太重）。状态机：IDLE → DETECTING（移动超过 touch slop 后判定方向）→ HORIZONTAL / VERTICAL_LEFT / VERTICAL_RIGHT / PINCH。判定后锁定方向，直到手指抬起。
- **快进快退逻辑**：
  - 水平滑动：位移映射到时间偏移，全屏宽度 = 60 秒（可配置），手指越快偏移越大。松手时执行 seek。
  - seek 使用 `ExoPlayer.seekTo()` 带 `C.SEEK_FLAG_CLOSEST_SYNC` 精确定位，避免跳到关键帧导致画面跳跃。对于高码率视频，seek 前暂停解码队列预加载，seek 后立即恢复。
  - seek 过程中实时更新进度条和时间标签，使用 `Player.Listener.onPositionDiscontinuity()` 回调校准。
- **亮度控制**：使用 `WindowManager.LayoutParams.screenBrightness`，范围 0.01f~1.0f。初始值读取当前窗口亮度（非系统亮度），首次滑动时记录起点。滑动全程显示亮度 overlay 百分比条。亮度变更立即写入 SharedPreferences，下次打开任何视频恢复。
- **音量控制**：使用 `AudioManager.adjustStreamVolume()` 或 `setStreamVolume()`。滑动全程显示系统音量条 overlay。静音时上滑先解除静音。
- **长按倍速**：按下时设置 `player.setPlaybackParameters(PlaybackParameters(2.0f, 1.0f))`，抬起时恢复之前的 PlaybackParameters。如果当前已经是 2x 则不起效。显示"2x"浮层提示。
- **双击防抖**：两次点击间隔 < 300ms 判定为双击。双击位置决定快进/快退：屏幕左半 = 快退，右半 = 快进。双击后播放/暂停的反馈通过播放按钮动画体现。
- **手势冲突处理**：滑动判定阶段（移动 < touch slop）不触发任何操作。一旦判定为水平/垂直，锁定方向直到抬起。控件层（SeekBar、按钮）的触摸事件优先级高于手势层。
- **Pinch to Zoom**（P2 手势，此处预埋接口）：双指捏合切换画面比例，捏合放大 = Crop 模式渐进放大，最大 3x。

---

### 五、播放器设置 — 其他

#### UI 设置项

| 功能项 | 类型 | 可选值 | 说明 |
|--------|------|--------|------|
| 记住播放进度 | 开关 | 开 / 关 | 默认开 |
| 自动播放下一个 | 开关 | 开 / 关 | 默认开 |
| 循环模式 | 单选列表 | 关闭 / 单曲循环 / 列表循环 | 默认关闭 |
| 屏幕常亮 | 开关 | 开 / 关 | 默认开 |
| 控制栏自动隐藏 | 单选列表 | 3秒 / 5秒 / 8秒 / 不自动隐藏 | 默认3秒 |
| 播放时锁定屏幕 | 按钮（播放器内） | — | 锁定后屏蔽所有手势和返回键 |
| 跳过片头片尾 | 开关 + 时间输入 | 开/关 + 片头X秒 / 片尾X秒 | 默认关 |

#### 底层逻辑

- **进度记忆与恢复**：
  - 保存时机：暂停时、Activity `onPause()` 时、播放器销毁前，写入 Room `history.lastPosition`
  - 恢复时机：`PlayerActivity.onCreate()` 中，从 Room 读取上次位置。如果 `lastPosition > 0` 且 `lastPosition < duration - 10s`，seek 到该位置并显示"从上次位置继续播放"的 Toast/Snackbar（3秒自动消失）。如果剩余不足 10 秒，视为已看完，从头播放
  - 写入防抖：播放过程中不实时写入，仅在上述三个时机写入，避免频繁 IO
- **播放列表引擎**：
  - 数据模型：`PlaylistEntity(id, name, createdAt)` + `PlaylistVideoEntity(playlistId, videoId, order)` 多对多关系
  - 播放顺序：线性顺序 / 随机（Fisher-Yates 洗牌，记录洗牌序列支持上一个） / 单曲循环
  - 上一个/下一个：线性模式直接 ±1，随机模式从洗牌序列取。到达列表末尾：列表循环→回到第一个，关闭→停止播放
  - 列表来源：首页全部视频 / 指定播放列表 / 文件夹
- **跳过片头片尾**：
  - 用户设置片头 N 秒 + 片尾 M 秒
  - 每个视频播放开始时自动 seek 到 N 秒
  - 播放到 `duration - M` 秒时自动触发下一个（或停止）
  - 片头片尾时间可按播放列表/全局两种粒度设置
- **屏幕锁定**：
  - 锁定后：屏蔽所有触摸事件（除解锁按钮）、屏蔽返回键、屏蔽音量键（可选）
  - 解锁：屏幕中央显示一个小锁图标（半透明），点击后弹出确认（防止误触解锁）
  - 锁定状态存储在 ViewModel 中，旋转屏幕不丢失
- **自动隐藏控制栏**：
  - `Handler.postDelayed()` 实现，每次触摸重置计时器
  - 播放暂停状态下不自动隐藏
  - 隐藏时使用 alpha 动画（200ms 渐隐），显示时同样动画

---

### 六、播放引擎核心逻辑

用户看不到但决定"好不好用"的核心。

#### 解码策略

- 优先使用硬件解码器（`MediaCodecSelector.DEFAULT`）。播放失败时自动回退到软件解码，同时记录失败的 MIME type + 设备型号到本地日志，避免下次再尝试硬件解码
- 解码器选择存储在 `MediaItem` 的 `preferredAudioLanguage` 和 `preferredVideoMimeType` 中，下次播放同类型文件直接使用已验证的解码器

#### 缓冲策略

- 本地文件：`DefaultLoadControl.Builder().setBufferDurationsMs(min=5s, max=15s, playback=2s, rebuffer=5s)`。本地文件不需要大缓冲，小缓冲减少 seek 延迟
- 预加载下一个视频：当前视频播放到 90% 时，静默 prepare 下一个视频的 `MediaSource`，切换时零延迟

#### Seek 优化

- 短距离 seek（< 3s）：使用 `ExoPlayer.seekTo()` 默认关键帧 seek，速度快
- 长距离 seek（> 3s）：使用 `C.SEEK_FLAG_CLOSEST_SYNC` 精确 seek，用户体验好
- Seek 过程中冻结当前帧作为预览，seek 完成后才刷新画面，避免花屏

#### 生命周期管理

- `onPause()`：暂停播放 + 保存进度
- `onResume()`：恢复播放（如果设置允许）+ 恢复亮度/音量
- `onDestroy()`：释放播放器资源 + 保存进度
- `configChanges="orientation|screenSize"`：旋转时保留播放器实例，只切换布局，不中断播放
- 后台 Service：仅在"后台音频"开启时启动，前台通知保活

#### 错误处理与恢复

- `Player.Listener.onPlayerError()` 分类处理：
  - `BehindLiveWindowException`：自动重试 prepare
  - `UnknownHostException`（网络相关，虽然离线但可能有 NAS 场景）：提示无网络
  - `MediaCodecDecoderException`：自动切换到软件解码并重试
  - 其他：显示错误信息 + "重试"按钮
- 连续 3 次解码失败：提示"该视频格式可能不受支持"，提供"使用软解"选项

#### 内存管理

- 播放器生命周期跟随 Activity，不使用 Application 级单例（避免内存泄漏）
- Surface 在 `onPause()` 时释放，在 `onResume()` 时重建
- Glide 缓存策略：内存缓存 = 屏幕可显示数量的 2 倍；磁盘缓存 250MB，LRU 淘汰

---

### 七、文件列表 — 右键菜单

触发方式：点击视频项右侧"更多"按钮 或 长按视频项。

| 菜单项 | 行为 |
|--------|------|
| 播放 | 打开 PlayerActivity |
| 添加到收藏 / 取消收藏 | 调用 `VideoRepository.toggleFavorite()`，UI 即时刷新星标 |
| 添加到播放列表 | 弹出播放列表选择对话框（带"新建列表"选项） |
| 查看详情 | 底部弹出面板：文件名、完整路径、分辨率、时长、文件大小、视频编码(H.264/H.265等)、音频编码、帧率、修改时间 |
| 分享 | `ACTION_SEND` Intent，类型 `video/*`，附带文件 content URI |
| 删除 | 二次确认对话框"确定删除此文件？删除后不可恢复" → `ContentResolver.delete()` 或 SAF `DocumentFile.delete()` |
| 重命名 | 弹出内联编辑框，确认后 `ContentResolver.update()` 修改 MediaStore `DISPLAY_NAME` |

---

### 八、软件设置（应用级）

| 分组 | 功能项 | 类型 | 可选值 | 说明 |
|------|--------|------|--------|------|
| 通用 | 主题模式 | 单选列表 | 深色 / 浅色 / 跟随系统 | 当前仅深色 |
| 通用 | 语言 | 单选列表 | 跟随系统 / 中文 / English | |
| 播放器 | 默认画面比例 | 单选列表 | 同播放器设置 | 新视频的默认值 |
| 播放器 | 默认倍速 | 单选列表 | 0.5x ~ 2.0x | 新视频的默认值 |
| 存储 | 缩略图缓存 | 展示大小 + 清除按钮 | — | Glide 磁盘缓存 |
| 存储 | 播放历史 | 展示条数 + 清除按钮 | 二次确认 | Room history 表 |
| 关于 | 版本号 | 展示 | — | |
| 关于 | 开源许可 | 展示 | — | Apache 2.0 |
| 关于 | GitHub 地址 | 可点击 | — | |

---

## P1 — 重要增强

### P1 — 播放器增强

#### 画面增强

| 功能项 | 类型 | 可选值 | 说明 |
|--------|------|--------|------|
| 视频滤镜 | 单选列表 | 无 / 锐化 / 柔和 / 色彩增强 / 黑白 / 复古 | 实时画面后处理 |
| 亮度/对比度/饱和度 | 三个滑块 | -100 ~ +100 | 精细画面调节 |
| 播放器内手势截图 | 按钮（播放器内） | — | 截取当前帧保存到相册 |

底层逻辑：
- **视频滤镜**：通过 `ExoPlayer` 的 `VideoFrameProcessor` 或 `Effect` API 实现。使用 OpenGL shader 做实时后处理，不影响解码性能。滤镜链：解码帧 → OpenGL 纹理 → shader 处理 → 输出到 Surface。切换滤镜时不需要重建播放器，只替换 shader program。
- **截图实现**：使用 `PixelCopy` API（API 26+）或 `SurfaceView.getHolder().lockCanvas()` 获取当前帧像素。保存为 JPEG/PNG 到 `Pictures/OpenVideo/` 目录，同时插入 MediaStore 让系统相册可见。保存后显示 Snackbar 带"查看"按钮。

#### 声音增强

| 功能项 | 类型 | 可选值 | 说明 |
|--------|------|--------|------|
| 均衡器预设 | 单选列表 | 普通 / 摇滚 / 流行 / 古典 / 爵士 / 低音增强 / 人声增强 / 自定义 | |
| 自定义均衡器 | 5~10段滑块 | 各频段增益 ±12dB | 自定义预设下展开 |
| 自动音量 | 开关 | 开 / 关 | 不同视频间音量差异大时自动归一化 |

底层逻辑：
- **均衡器**：使用 `AudioEffect` 框架的 `Equalizer` 类，绑定到播放器的 `AudioSessionId`。预设通过 `Equalizer.usePreset()` 实现，自定义通过 `Equalizer.setBandLevel()` 实现。设置变更实时生效，不需要暂停播放。
- **自动音量**：分析音频流的 RMS 响度，计算增益因子使平均响度对齐到 -14 LUFS（广播标准）。通过 `AudioProcessor` 在 PCM 层面处理。此功能可能增加 CPU 开销，默认关闭。

#### 字幕增强

| 功能项 | 类型 | 可选值 | 说明 |
|--------|------|--------|------|
| 字幕字体 | 单选列表 | 系统默认 / 宋体 / 黑体 / 楷体 / 自定义字体文件 | ASS 字幕内嵌字体优先 |
| 字幕描边 | 单选列表 | 无 / 细描边 / 粗描边 | 黑色描边增强可读性 |
| 字幕阴影 | 单选列表 | 无 / 轻阴影 / 重阴影 | |
| 双字幕显示 | 开关 + 轨道选择 | 开/关 + 第二字幕轨 | 同时显示两种语言字幕，用于语言学习 |

底层逻辑：
- **字体加载**：`Typeface.createFromFile()` 加载自定义字体文件，缓存在内存中。ASS 字幕的 `Fontname` 字段匹配已加载字体，匹配不到时回退到默认字体。
- **描边/阴影实现**：`TextView` 的 `Paint.setStrokeWidth()` + `setShadowLayer()` 实现。ASS 字幕的 `Outline` 和 `Shadow` 字段自动映射。
- **双字幕**：两个独立的 `SubtitleView` 层叠，上层显示第二语言（较小字号），下层显示主字幕。两个 SubtitleView 各自独立 seek 到相同时间轴。

#### 手势增强

| 功能项 | 类型 | 可选值 | 说明 |
|--------|------|--------|------|
| 双击快进/快退秒数 | 单选列表 | 5秒 / 10秒 / 15秒 / 30秒 | 双击左右区域的跳转幅度 |
| 长按倍速值 | 单选列表 | 1.5x / 2.0x / 3.0x | 长按时的临时速度 |
| 滑动快进灵敏度 | 滑块 | 全屏滑动对应 30s / 60s / 90s / 120s | 水平滑动映射的时间范围 |
| 边缘滑动返回 | 开关 | 开 / 关 | 屏幕左边缘右滑 = 返回（防与快进冲突） |

底层逻辑：
- **快进快退动画**：seek 后在目标位置前后 3 帧范围内做快速逐帧播放（1x 速度），模拟"拖动感"而非直接跳切。如果视频关键帧间隔大（> 2s），则直接跳到目标位置不做过渡。
- **边缘手势判定**：触摸起始点在屏幕宽度 5% 以内 = 边缘手势（返回），5%~50% = 左侧手势，50%~95% = 右侧手势。边缘区域宽度可配置。

---

### P1 — 文件列表增强

| 功能项 | 说明 |
|--------|------|
| 搜索 | 顶部搜索栏，实时过滤标题匹配的视频 |
| 排序 | 按名称/日期/大小/时长，升序/降序 |
| 网格/列表切换 | 已有布局但未实现，补通逻辑 |
| 按文件夹浏览 | MediaStore 按 `RELATIVE_PATH` 分组，显示目录树 |
| 多选操作 | 长按进入多选模式，批量收藏/删除/添加到播放列表 |
| 视频信息面板 | 右键"查看详情"展开：编解码器信息（从 MediaExtractor 提取）、音频通道、字幕轨列表 |

底层逻辑：
- **搜索**：在 Adapter 层做过滤，`Filterable` 接口实现。搜索词匹配标题（`contains`，不区分大小写）。过滤在协程中执行，避免阻塞主线程。空结果时显示"未找到匹配视频"。
- **排序**：封装 `VideoComparator` 工厂类，按字段+方向生成 `Comparator<VideoItem>`。排序切换时调用 `adapter.submitList(sortedList)`，使用 DiffUtil 做动画。
- **文件夹浏览**：`VideoScanner` 增加按 `RELATIVE_PATH` 分组逻辑。顶层显示文件夹列表，点击进入文件夹显示该目录下的视频。支持返回上级目录。
- **多选**：`RecyclerView` 的 `ChoiceMode` + `ActionMode`（顶部操作栏）。选中项高亮，操作栏显示：全选/取消全选 + 收藏/删除/添加到列表。退出多选模式时重置选中状态。

---

### P1 — 播放列表管理

| 功能项 | 说明 |
|--------|------|
| 创建播放列表 | 输入名称，创建空列表 |
| 编辑播放列表 | 重命名、排序（拖拽排序）、删除列表 |
| 视频管理 | 从列表中移除视频、调整顺序 |
| 播放列表导入/导出 | 导出为 .m3u 文件 / 从 .m3u 导入 |
| 最近播放列表 | 首页快速入口，显示最近使用的播放列表 |

底层逻辑：
- **数据模型**：Room 三张表——`playlists(id, name, createdAt, updatedAt)`、`playlist_videos(playlistId, videoId, position)`、索引 `(playlistId, position)`。使用 `@Transaction` 保证批量操作原子性。
- **拖拽排序**：`ItemTouchHelper` + `RecyclerView`，拖拽过程中实时更新 `position` 字段。松手后批量写入数据库（debounce 500ms）。
- **M3U 导入/导出**：导出时生成标准 M3U 文件（`#EXTM3U` + `#EXTINF` + 文件路径）。导入时解析 M3U，路径匹配本地文件，匹配不到的提示用户。

---

## P2 — 锦上添花

### P2 — 播放器高级功能

| 功能项 | 类型 | 说明 |
|--------|------|------|
| AB 循环 | 按钮 | 设置 A 点和 B 点，区间内反复播放，用于学习/复习场景 |
| Pinch to Zoom | 手势 | 双指捏合放大画面，最大 3x，双击重置 |
| 画中画(PiP) | 按钮 | 进入系统 PiP 模式，支持 PiP 窗口内播放/暂停 |
| 倍速记忆 | 按视频 | 每个视频独立记忆倍速设置（而非全局） |
| 视频封面提取 | 按钮 | 用当前帧替换缩略图（写入 Glide 缓存或独立存储） |
| 字幕搜索 | 输入框 | 在字幕文本中搜索关键词，跳转到对应时间点 |
| 手势自定义映射 | 高级设置 | 将任意手势映射到任意操作（如双指下滑 = 截图） |

底层逻辑：
- **AB 循环**：`Player.Listener` 监听播放位置，到达 B 点时 seek 到 A 点。A/B 点显示在 SeekBar 上作为高亮区间。退出 AB 循环时清除标记。
- **画中画**：`enterPictureInPictureMode()` + `PictureInPictureParams.Builder` 设置比例（16:9）。PiP 模式下隐藏自定义控件，使用系统默认控件。`onPictureInPictureModeChanged()` 中暂停/恢复 UI 更新。
- **倍速记忆**：Room 新增 `video_settings(videoId, speed, ...)` 表。进入播放器时查询该表，有记录则覆盖全局默认值。

### P2 — 文件管理高级功能

| 功能项 | 说明 |
|--------|------|
| 视频缩略图预览 | SeekBar 上显示关键帧缩略图预览（类似 YouTube 进度条） |
| 文件夹隐藏 | 标记特定文件夹不显示（如 WhatsApp 视频、广告缓存） |
| 隐私文件夹 | 加密或隐藏特定视频（需密码/指纹解锁） |
| 重复文件检测 | 扫描相同文件大小+MD5 的重复视频，提示清理 |

底层逻辑：
- **缩略图预览**：使用 `MediaMetadataRetriever.getFrameAtTime()` 按固定间隔（如每 10 秒）提取帧，生成缩略图条带（sprite sheet）。缓存到磁盘。SeekBar 的 `OnSeekBarChangeListener.onProgressChanged()` 中计算当前时间对应的缩略图，显示在 SeekBar 上方。
- **隐私文件夹**：使用 `EncryptedSharedPreferences` 存储加密的文件路径列表。隐私视频从主列表中隐藏，需密码验证后进入"隐私空间"查看。文件本身不加密（性能考虑），仅隐藏索引。

### P2 — 系统集成

| 功能项 | 说明 |
|--------|------|
| 通知栏控制 | 媒体通知：播放/暂停、上一个/下一个、进度条、封面缩略图 |
| 锁屏控制 | 锁屏界面显示媒体控制卡片 |
| 耳机控制 | 线控单击=播放暂停、双击=下一个、三击=上一个 |
| 蓝牙设备 | 蓝牙连接/断开时自动暂停/恢复 |
| 车载模式 | 简化大按钮 UI，仅保留基本控制 |

底层逻辑：
- **MediaSession**：创建 `MediaSessionCompat`，设置 `MediaButtonReceiver` 处理硬件按键。`MediaSession.setCallback()` 实现 `onPlay/onPause/onSkipToNext/onSkipToPrevious/onSeekTo`。通过 `MediaSession.setMetadata()` 同步当前视频信息到系统 UI。
- **通知栏**：`MediaStyle` 通知，`setMediaSession()` 关联 MediaSession。`setShowActionsInCompactView(1, 2, 3)` 显示三个操作按钮。通知优先级 `PRIORITY_LOW`，不发声不振动。
- **耳机拔出**：监听 `AudioManager.ACTION_AUDIO_BECOMING_NOISY` 广播，自动暂停播放，防止外放尴尬。

---

## 页面自适应设计

### 屏幕尺寸与形态适配

#### 断点系统

| 断点 | 宽度范围 | 设备类型 | 布局策略 |
|------|----------|----------|----------|
| Compact | 0~599dp | 手机竖屏 | 单列布局，底部导航栏 |
| Medium | 600~839dp | 手机横屏 / 小平板 | 双列布局，侧边导航栏 |
| Expanded | 840dp+ | 大平板 / 折叠屏展开 | 三列布局，侧边导航栏 + 详情面板 |

#### 各页面适配方案

**视频列表页：**
- Compact：单列列表（现有布局）
- Medium：2列网格
- Expanded：3~4列网格 + 右侧视频详情面板（Master-Detail 模式）
- 搜索栏：Compact 时折叠为图标，点击展开；Medium/Expanded 时始终显示

**播放器页：**
- 手机竖屏：底部控制栏，视频居上，下方可选信息区
- 手机横屏：全屏沉浸，控制栏叠加在视频底部（现有布局）
- 平板：视频居中，两侧留黑边（或显示视频信息/播放列表侧栏）
- 折叠屏半折叠（Flex Mode）：上半屏显示视频，下半屏显示控制面板和字幕

**设置页：**
- Compact：左侧导航 + 右侧内容分两屏切换（现有布局）
- Medium/Expanded：左侧导航栏常驻 + 右侧内容并排显示

**底部导航栏：**
- Compact：底部 4 tab（现有）
- Medium：侧边 NavigationRail（图标 + 文字竖排）
- Expanded：侧边 NavigationDrawer（图标 + 文字横排，可展开/收起）

#### 底层逻辑

- 使用 `WindowSizeClass`（Jetpack 库）自动计算断点
- 布局文件分三套：`layout/`、`layout-w600dp/`、`layout-w840dp/`
- Fragment 使用 `Navigation Component`，不同断点下 `NavHost` 的 `popBackStack` 行为不同（Compact 返回栈，Expanded 关闭详情面板）
- 横竖屏切换使用 `configChanges` 保留播放器状态，不重建 Activity

### 刘海屏/挖孔屏/瀑布屏

| 场景 | 处理策略 |
|------|----------|
| 刘海/挖孔（Display Cutout） | 播放器沉浸模式下使用 `LAYOUT_IN_DISPLAY_CUTOUT_SHORT_EDGES`，视频延伸到刘海区域 |
| 瀑布屏（曲面边缘） | 控制栏内缩 `systemWindowInsetBottom`，手势热区避开曲面区域（左右各留 20dp 安全区） |
| 折叠屏铰链区域 | 检测 `FoldingFeature`，如果视频跨越铰链则自动调整布局避免内容被遮挡 |
| 状态栏/导航栏 | 沉浸模式下 `WindowInsetsController.hide()` 全部隐藏；非沉浸模式下内容避开系统栏 |

底层逻辑：
- API 28+：`WindowInsets` 检测 cutout 区域
- API 30+：`WindowInsets.Type` 精确获取系统栏、IME、cutout 的 inset
- 旧版本回退：`fitsSystemWindows` + `ViewCompat.setOnApplyWindowInsetsListener()`
- 每次 `onConfigurationChanged()` 时重新计算 insets，因为折叠状态可能变化

### 多窗口模式

| 模式 | 行为 |
|------|------|
| 分屏（Split Screen） | 播放器继续播放，控件自适应小窗口布局。视频比例切换为 Fit 模式避免裁剪 |
| 自由窗口（Freeform） | 同分屏，窗口可拖拽调整大小，实时响应尺寸变化 |
| 画中画（PiP） | P2 功能，进入 PiP 后隐藏自定义控件，使用系统默认 |
| 多实例（Multi-Instance） | 不支持，`android:launchMode="singleTask"` 保证全局唯一播放器 |

底层逻辑：
- `isInMultiWindowMode` 检测多窗口状态
- 多窗口下 `onResume` 不代表可见，使用 `hasWindowFocus()` 判断真正可见性
- 分屏模式下音量控制需要区分哪个窗口有焦点

---

## 架构兼容设计（x86 / ARM）

### ABI 策略

| ABI | 优先级 | 说明 |
|-----|--------|------|
| arm64-v8a | 最高 | 主流 Android 设备（2016 年后几乎所有手机） |
| armeabi-v7a | 高 | 老旧 32 位 ARM 设备 |
| x86_64 | 中 | ChromeOS、部分 Android 模拟器、Intel 平板 |
| x86 | 中 | 老旧模拟器、少量 Intel 手机 |

APK 策略：
- 使用 Android App Bundle (AAB) 发布，Google Play 按设备 ABI 按需分发 native 库
- 侧载场景：提供通用 APK（包含所有 ABI）或按 ABI 分包（`splits.abi`）
- `build.gradle.kts` 中配置 `ndk.abiFilters` 明确支持范围

底层逻辑：
- **解码器选择优先级**：硬件解码器 → 架构优化的软解码器 → 通用软解码器
- **x86 设备特殊处理**：x86 Android 设备可能通过 ARM 转译层运行 ARM native 库，性能损失 10~30%。检测到 x86 架构时优先使用 x86 原生解码器（如 MediaCodec 提供的 Intel 硬件解码）
- **模拟器检测**：通过 `Build.FINGERPRINT` 包含 `generic` 或 `sdk` 判断模拟器环境，模拟器下默认使用软解码（模拟器硬件解码常有兼容问题）

### 解码器兼容矩阵

| 视频编码 | ARM 硬解 | ARM 软解 | x86 硬解 | x86 软解 |
|----------|----------|----------|----------|----------|
| H.264 (AVC) | 全支持 | 全支持 | 全支持 | 全支持 |
| H.265 (HEVC) | API 21+ 设备基本支持 | FFmpeg 回退 | Intel 设备支持 | FFmpeg 回退 |
| VP9 | API 24+ 部分设备 | FFmpeg 回退 | Intel 支持 | FFmpeg 回退 |
| AV1 | API 29+ 新设备 | FFmpeg 回退 | 极少设备 | FFmpeg 回退 |
| MPEG-4/2/1 | 部分设备 | FFmpeg 回退 | 部分设备 | FFmpeg 回退 |
| RMVB/FLV | 不支持 | FFmpeg 回退 | 不支持 | FFmpeg 回退 |

底层逻辑：
- **解码能力探测**：`MediaCodecList` 枚举设备所有编解码器，按 MIME type 查询是否支持目标编码+分辨率组合。结果缓存到本地文件，避免每次启动重新探测
- **自动回退链**：目标 MIME → 查缓存 → 硬件解码器尝试 → 失败则软件解码器 → 再失败则提示不支持
- **分辨率限制**：部分低端设备硬件解码器有分辨率上限（如 1080p），超过时自动切换软解
- **软解性能保护**：检测 CPU 核心数和频率，低端设备（< 4 核或 < 1.5GHz）播放高码率视频时降低解码线程优先级，避免 UI 卡顿

### Native 库管理

| 库 | 用途 | ABI 策略 |
|----|------|----------|
| ExoPlayer FFmpeg 扩展 | 软解不常见格式 | 按 ABI 编译，优先使用 |
| libass | ASS/SSA 字幕渲染 | 按 ABI 编译，字体渲染引擎 |
| chardet | 字符编码检测 | 纯 Java 实现，无 ABI 限制 |

底层逻辑：
- `libs.versions.toml` 中 ExoPlayer FFmpeg 模块按 ABI 提供 `so` 文件
- 如果某个 ABI 缺少 `so` 文件，运行时 catch `UnsatisfiedLinkError`，标记该功能不可用并使用纯 Java 回退
- `OpenVideoApp.onCreate()` 中预加载 native 库（`System.loadLibrary`），失败时记录日志并设置降级标志

---

## Android 厂商兼容设计

### 厂商后台限制

Android 厂商为省电，各自实现激进的后台进程管理，影响后台音频播放和历史记录保存。

| 厂商 | ROM | 问题 | 解决方案 |
|------|-----|------|----------|
| 小米 | MIUI/HyperOS | 后台进程 15 分钟内被杀；自启动默认关闭 | 引导用户开启自启动权限；使用前台 Service + 通知保活；`PowerKeeper` 白名单请求 |
| 华为 | EMUI/HarmonyOS | 后台活动严格管控；锁屏后应用被冻结 | 引导用户关闭"应用启动管理"的自动管理；使用 `doze白名单` 请求（需用户确认） |
| OPPO | ColorOS | 后台冻结；推送延迟 | 引导用户在"电池"设置中关闭"后台冻结"；前台 Service 保活 |
| vivo | OriginOS/FuntouchOS | 后台高耗电自动清理 | 同 OPPO 策略 |
| 三星 | OneUI | 相对宽松但"深度睡眠"功能会冻结不活跃应用 | 引导用户将应用从"深度睡眠"列表移除；前台 Service |
| OnePlus | OxygenOS | 继承 OPPO 策略（同 ColorOS） | 同 OPPO |
| 魅族 | Flyme | 后台管控严格 | 前台 Service + 引导设置 |

底层逻辑：
- **厂商检测**：通过 `Build.MANUFACTURER` + `Build.BRAND` 判断厂商，维护一份 `VendorCompat` 工具类
- **权限引导**：首次使用后台音频功能时，检测是否为目标厂商设备，弹出引导对话框："为了保证后台播放不被中断，请在系统设置中允许本应用的自启动/后台活动权限"。提供"一键跳转"按钮，使用各厂商的 Intent Action 跳转到对应设置页
- **各厂商设置页 Intent**：
  - 小米：`Intent("miui.intent.action.APP_PERM_EDITOR")` + packageName
  - 华为：`Intent("huawei.systemmanager")` + 组件名
  - OPPO：`Intent("oppo.intent.action.AUTO_START")` 或 Settings → 电池
  - 三星：`Settings.ACTION_BATTERY_SAVER_SETTINGS` 或 `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`
  - 通用回退：`Settings.ACTION_APPLICATION_DETAILS_SETTINGS` → 应用详情页
- **前台 Service**：后台播放使用 `startForeground()` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 类型（API 29+）。通知常驻，用户不可滑动关闭。Service 被系统杀死时通过 `START_STICKY` 自动重启
- **电量优化白名单**：`PowerManager.isIgnoringBatteryOptimizations()` 检查，未加入白名单时提示用户。使用 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 请求（需 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 权限，Google Play 审核严格，开源项目可用）

### 存储权限兼容

| Android 版本 | 权限模型 | 视频访问策略 |
|-------------|----------|-------------|
| 5.0~9 (API 21~28) | `READ_EXTERNAL_STORAGE` | 授权后直接 MediaStore 查询 |
| 10 (API 29) | `READ_EXTERNAL_STORAGE` + Scoped Storage 可选 | 使用 `requestLegacyExternalStorage` 过渡 |
| 11+ (API 30+) | `READ_MEDIA_VIDEO` (API 33+) 或 `MANAGE_EXTERNAL_STORAGE` | MediaStore 查询无需额外权限；文件删除需要 `ContentResolver.delete()` |

底层逻辑：
- **权限请求**：运行时检查 → 未授权则请求 → 被永久拒绝则引导到设置页
- **文件删除**：API 29 以下使用 `File.delete()`；API 29+ 使用 `ContentResolver.delete(contentUri)`；如果被拒绝（权限不足），弹出 SAF 授权对话框
- **文件重命名**：`ContentResolver.update()` 修改 `MediaStore.Video.Media.DISPLAY_NAME`
- **外挂字幕文件访问**：字幕文件可能不在 MediaStore 中。API 30+ 使用 `ACTION_OPEN_DOCUMENT` 获取字幕文件的 content URI，通过 `ContentResolver.openInputStream()` 读取

### 显示与渲染兼容

| 问题 | 涉及厂商 | 解决方案 |
|------|----------|----------|
| SurfaceView 在某些设备上 z-order 异常 | 部分联发科设备 | 回退到 TextureView；设置 `setZOrderOnTop(true)` |
| 折叠屏展开/折叠时 Surface 销毁 | 三星 Fold、华为 Mate X | 使用 `configChanges` 捕获配置变更；在 `onConfigurationChanged` 中重建 Surface 而非重建 Activity |
| 高刷新率屏幕（90Hz/120Hz）帧率不匹配 | 大多数新设备 | 使用 `Display.getRefreshRate()` 获取刷新率，向 `Choreographer` 注册 VSync 回调同步帧 |
| HDR 视频在非 HDR 屏幕上颜色发灰 | 部分设备 | 检测 `Display.isHdr()`，非 HDR 屏幕播放 HDR 视频时使用 tone mapping（ExoPlayer `MediaItem` 的 `colorInfo` 处理） |
| 曲面屏边缘误触 | 三星 Edge、部分国产曲面屏 | 手势热区内缩 20dp；提供"边缘防误触"开关 |

### 通知与媒体控制兼容

| 问题 | 涉及版本/厂商 | 解决方案 |
|------|--------------|----------|
| Android 13+ 通知权限 | API 33+ | 首次启动请求 `POST_NOTIFICATIONS` 权限 |
| 通知渠道 | API 26+ | 创建 `IMPORTANCE_LOW` 渠道"播放控制"，不可删除 |
| 媒体通知样式 | 不同厂商 ROM | 使用 `MediaStyle` + `setMediaSession()`，系统自动适配 |
| 锁屏控制 | 部分厂商锁屏不显示 | 通过 `MediaSession.setActive(true)` 注册，系统级别保证显示 |
| 蓝牙 AVRCP | 各厂商蓝牙栈 | 通过 `MediaSession` 元数据同步，蓝牙设备自动读取 |

### Android 版本兼容层

| 功能 | 最低 API | 降级方案 |
|------|----------|----------|
| Picture-in-Picture | 26 (O) | API < 26 隐藏 PiP 按钮 |
| 自动旋转 | 18 (JB MR2) | API < 18 使用固定横屏 |
| 前台 Service 类型 | 29 (Q) | API < 29 使用普通前台 Service（不需要指定类型） |
| `READ_MEDIA_VIDEO` | 33 (T) | API < 33 使用 `READ_EXTERNAL_STORAGE` |
| 通知运行时权限 | 33 (T) | API < 33 无需请求 |
| `WindowInsets` API | 30 (R) | API < 30 使用 `ViewCompat.setOnApplyWindowInsetsListener` |
| `setVideoEffects` (ExoPlayer) | 29 (Q) | API < 29 不支持视频滤镜，隐藏该选项 |

底层逻辑：
- 封装 `CompatUtil` 工具类，集中管理版本判断和降级逻辑
- 每个功能入口先调用 `CompatUtil.isSupported(feature)` 判断，不支持时隐藏 UI 或显示灰色提示
- 使用 `@RequiresApi` 注解标记需要特定 API 的方法，Lint 辅助检查

---

## 功能总览

| 模块 | P0 | P1 | P2 |
|------|----|----|-----|
| 画面 | 3项 + 3层逻辑 | 3项 + 2层逻辑 | 2项 |
| 声音 | 4项 + 5层逻辑 | 3项 + 2层逻辑 | — |
| 字幕 | 8项 + 5层逻辑 | 4项 + 3层逻辑 | 2项 |
| 手势 | 6项 + 7层逻辑 | 4项 + 2层逻辑 | 1项 |
| 其他 | 7项 + 5层逻辑 | — | — |
| 播放引擎 | 6层核心逻辑 | — | 3项 |
| 文件列表 | 7项菜单 | 6项增强 | 4项高级 |
| 软件设置 | 9项 | — | 3项系统集成 |
| 播放列表 | — | 5项 + 3层逻辑 | — |
| 页面自适应 | 3套断点 + 4类设备 | — | — |
| 架构兼容 | 4 ABI + 6编码 + 3 native库 | — | — |
| 厂商兼容 | 7厂商 + 5类兼容问题 | — | — |
