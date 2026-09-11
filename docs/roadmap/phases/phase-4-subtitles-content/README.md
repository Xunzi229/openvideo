# Phase 4：字幕与内容增强

**建议周期：** 3～6 周  
**目标版本：** v0.4.x / v0.5.0  
**阶段目标：** 在已有本地字幕解析、自动加载和字幕样式基础上，升级为强字幕体验：自动匹配 2.0、在线字幕、语言偏好、双字幕和字幕工具箱。

---

## 补充状态（2026-06-07）

- **P4-DUAL-003 第二字幕加载入口与开关：Done。** 播放器字幕设置 Sheet 已新增“加载第二字幕”入口和“显示第二字幕”开关；第二字幕加载复用现有 `PlayerSubtitleLoadCoordinator` / `SubtitleLoader` 路由，成功后写入既有 `DualSubtitleState.secondary` 并默认启用，开关通过 `PlayerViewModel.setSecondarySubtitlesEnabled(...)` 控制显示/隐藏。该切片不新增候选弹窗、不保存第二字幕到历史、不做主副字幕独立样式配置，也不触发在线字幕搜索。
- **P4-DUAL-004 主副字幕样式独立：Done。** `PlayerPrefs` 已新增副字幕字号、颜色、背景和位置偏好；未单独设置副字幕样式时默认继承主字幕当前样式，用户调整副字幕后独立保存。`PlayerDisplayController` 播放页基础样式、`PlayerPlaybackTickController` 的 ASS cue fallback、完整字幕设置 Sheet 和旧 `PlayerSubtitleSettingsActivity` 均按主/副字幕分别应用与预览。本切片不新增第二字幕历史保存、不做候选弹窗，也不做主副独立 ASS 绝对定位。
- **P4-MATCH-007 自动字幕扫描预算：Done。** `SubtitleFileCandidateScanner` 仍只扫描视频同目录和一层 `Subs` / `Subtitles` / `字幕` 子目录，并新增每个目录最多 40 个字幕候选的预算；排序会优先保留文件名以当前视频 basename 开头的候选，再按文件名稳定排序，避免大目录中大量无关字幕拖慢自动匹配启动。本切片不新增递归扫描、不扩大远程来源扫描范围。
- **P4-MATCH-005 多候选选择弹窗入口：Done。** `PlayerSubtitleLoadCoordinator` 新增 `PlayerSubtitleLoadOutcome`，在 sidecar 候选同优先级需要用户选择时返回候选列表而不是静默丢弃；`PlayerViewModel.loadSubtitles(...)` 将候选透传给 `PlayerSubtitleController`，Controller 使用单选弹窗展示候选文件名，用户点选后写入 `playerPrefs.externalSubtitleUri`，沿用既有 prefs listener 加载并记忆选择。低置信度候选仍由 `SubtitleCandidateSelectionPolicy` 排除，不会自动应用。
- **P4-ONLINE-003 在线字幕首次隐私提示入口：Done。** 字幕设置 Sheet 已新增“在线字幕搜索”手动入口，点击后先展示隐私确认框，说明会发送用户输入的标题、季、集和语言，默认不会上传文件 hash、视频或字幕文件；用户确认后才进入后续搜索流程。当前切片只接隐私提示与占位 Toast，不调用 `OnlineSubtitleClient.search(...)`，不接真实 OpenSubtitles API、结果列表、下载或缓存。
- **P4-LANG-001 字幕语言偏好设置 UI：Done。** 播放器字幕设置 Sheet 已新增“字幕语言偏好”区，支持循环设置首选语言、次选语言，并通过开关持久化“优先使用双语字幕”；写入复用既有 `PlayerPrefs.subtitlePrimaryLanguage` / `subtitleSecondaryLanguage` / `subtitlePreferBilingual`，自动匹配链路继续复用已完成的 `SubtitleLanguagePreference` 选择策略。本切片不新增候选弹窗、不加载第二字幕，也不重复现有编码/延迟设置。
- **P4-TOOLS-001 字幕缓存副本管理策略：Done。** 已新增纯策略 `SubtitleCacheCopyPolicy` / `SubtitleCacheTarget` / `SubtitleCacheRetentionPlan`，统一规划工具箱缓存副本的 `subtitles` 子目录、安全文件名、用途后缀（UTF-8 / delay-corrected）、时间戳和保留上限（默认 10 份）；`planRetention(...)` 按创建时间保留最新副本并规划删除旧副本。该切片不做真实文件写入/删除、不接设置页清缓存 UI，也不覆盖原字幕；后续若需要 app cache 内临时副本，可复用该策略接入文件系统边界。
- **P4-TOOLS-001 原字幕覆盖保护：Done。** `SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(...)` 已补齐导出目标保护，支持精确 `content://` URI、本地路径和 `file://` 路径归一化；`PlayerViewModel.writeCurrentSubtitleUtf8ExportTo(...)` 与 `writeCurrentSubtitleDelayCorrectionExportTo(...)` 会在 `openOutputStream(...)` 前拦截目标等于当前记忆外部字幕的未确认覆盖，并返回 `OriginalOverwriteBlocked`，字幕设置 Sheet 显示“请另存为新文件”提示。本切片不新增覆盖确认流程，默认继续只允许另存副本。
- **P4-TOOLS-001 延迟校正 SAF 导出入口：Done。** 字幕设置 Sheet 已新增“导出延迟校正字幕”入口，使用当前 `playerPrefs.subtitleDelayMs` 作为批量偏移值；非 0 且当前字幕非空时通过 `ActivityResultContracts.CreateDocument("application/x-subrip")` 让用户选择新文件名，并调用 `PlayerViewModel.writeCurrentSubtitleDelayCorrectionExportTo(...)`。ViewModel 复用 `SubtitleDelayCorrectionPolicy.planShiftedCopy(...)` 生成校正副本，再用 `SubtitleUtf8ExportPolicy` / `SubtitleExportWriter` 写出 UTF-8 SRT；0ms、空字幕、打开输出流失败和写入失败均 Toast 降级。该入口只生成新文件，不覆盖原字幕；真实缓存文件写入/删除仍留给后续切片。
- **P4-TOOLS-001 字幕信息摘要 UI：Done。** 字幕设置 Sheet 已新增紧凑“字幕信息”摘要区，`PlayerViewModel.currentSubtitleInfo()` 复用 `SubtitleInfoPolicy.summarize(...)` 汇总当前已加载字幕的来源、编码、行数、时间范围和样式行数，再由 `PlayerSubtitleInfoUiPolicy` 格式化展示；空字幕显示空态。该切片只展示诊断信息，不新增编辑器、不重复现有编码选择/延迟调整，也不接延迟批量校正保存入口。
- **P4-TOOLS-001 UTF-8 SAF 导出入口：Done。** 字幕设置 Sheet 已新增“导出 UTF-8 字幕”入口，当前已加载字幕非空时通过 `ActivityResultContracts.CreateDocument("application/x-subrip")` 让用户选择新文件名，并调用 `PlayerViewModel.writeCurrentSubtitleUtf8ExportTo(...)`；ViewModel 复用 `SubtitleUtf8ExportPolicy` 与 `SubtitleExportWriter` 写入用户选定 URI。空字幕、打开输出流失败和写入失败均 Toast 降级；该入口只生成新文件，不覆盖原字幕。完整字幕信息页、延迟批量校正保存入口和缓存副本管理仍留给后续切片。
- **P4-TOOLS-001 字幕导出写入边界：Done。** 已新增 `SubtitleExportWriter`，只把 `SubtitleUtf8ExportPlan.bytes` 写入调用方提供的 `OutputStream`，返回 `bytesWritten` 或 `WRITE_FAILED`。该边界不持有 `ContentResolver` / `Uri` / 文件路径，不创建 `FileOutputStream`，因此不会自行覆盖原字幕；UI、SAF `CreateDocument` 启动器和缓存副本接入留给后续切片。
- **P4-TOOLS-001 UTF-8 另存策略基础层：Done。** 已新增纯策略 `SubtitleUtf8ExportPolicy` / `SubtitleUtf8ExportPlan`，可把当前已解析字幕条目按 SRT 格式重新编号并序列化为 UTF-8 `content` / `bytes`，输出 `suggestedCopyName`、`charsetName = UTF-8`、`lineCount` 和 `overwritesOriginal = false`。本切片不做 UI、不写文件、不覆盖原字幕；SAF/缓存实际写入和格式选择留给后续切片。
- **P4-TOOLS-001 字幕延迟批量校正策略基础层：Done。** 已新增纯策略 `SubtitleDelayCorrectionPolicy` / `SubtitleDelayCorrectionPlan`，可对当前已解析字幕条目整体应用 ±ms 偏移并生成副本计划；负时间会裁剪到 0，文本、index 与 ASS `SubtitleCueStyle` 保留，输出包含 `suggestedCopyName`、`changedLineCount`、`deltaMs` 和 `overwritesOriginal = false`。本切片不做 UI、不写文件、不覆盖原字幕；真正另存 UTF-8 和缓存/SAF 写入留给后续切片。

## 补充状态（2026-06-06）

- **P4-MATCH-001 SubtitleCandidate 模型：Done。** 已新增 `core/subtitle/SubtitleCandidate` 纯模型，字段覆盖 `path`、`language`、`confidence`、`reason`；语言识别覆盖中文、英文、日文、韩文、双语和未知；匹配原因工厂将同名/语言后缀设为高置信度，剧集匹配/字幕目录设为中置信度，低置信度候选只保留提示语义。本切片只提供模型和单元测试，不接入自动加载链路，避免重复实现现有 `PlayerSubtitleAutoload` / `RemoteSidecarSubtitleMatcher` 的扫描逻辑。
- **P4-MATCH-002 同目录自动匹配增强：Done。** 已新增 `core/subtitle/SubtitleSidecarMatcher`，集中维护同目录同名、语言后缀、大小写无关扩展名和 `srt` / `ass` / `ssa` / `vtt` 支持规则，并返回 `SubtitleCandidate`；`PlayerSubtitleAutoload` 保留旧 API 但委托 core matcher，`SubtitleLoader.findSubtitleFiles(...)` 不再依赖 UI 包。本切片不做剧集规则、字幕子目录扫描或多候选弹窗，分别留给 4.1.3～4.1.5。
- **P4-MATCH-003 剧集规则匹配：Done。** `SubtitleSidecarMatcher.matchSameDirectory(...)` 已复用 Phase 2 `EpisodeNameParser`，在同目录候选中识别视频和字幕的同剧名、同季、同集候选，并以 `SubtitleCandidateReason.EPISODE_MATCH` / 中置信度返回；排序保持同名和语言后缀高置信度优先。本切片覆盖 `S01E02` 与中文 `第02集`，不新增目录递归、同系列邻近扫描或多候选弹窗。
- **P4-MATCH-004 字幕目录扫描：Done。** 已新增 `SubtitleFileCandidateScanner`，本地视频只枚举同目录字幕文件，以及一层 `Subs` / `Subtitles` / `字幕` 子目录内的字幕文件；不进入其它目录或更深层嵌套。`SubtitleLoader.findSubtitleFiles(...)` 现在通过 scanner 收集候选，再交给 `SubtitleSidecarMatcher.matchCandidates(...)` 排序；字幕目录候选以 `SUBTITLE_DIRECTORY` / 中置信度返回，并排在同目录同名、语言后缀和剧集匹配之后。
- **P4-MATCH-005 多候选选择策略：Done。** 已新增 `SubtitleCandidateSelectionPolicy`，按同名、语言后缀、剧集匹配、字幕目录、低置信度的优先级选择；唯一最高优先级候选会自动应用，多个同优先级候选返回 `RequiresUserChoice`，低置信度候选不自动应用。`PlayerSubtitleLoadCoordinator` 不再直接加载第一个 sidecar 文件，遇到需要用户选择时会通过 `PlayerSubtitleLoadOutcome.RequiresUserChoice` 把候选交给播放器 UI 单选弹窗。
- **P4-MATCH-006 记忆用户选择策略地基：Done。** 既有 `externalSubtitleUri` 已随播放历史保存和恢复，用于用户手动选择外部字幕后下次打开优先加载；本切片补齐 sidecar 候选层的 remembered-path 选择能力：`SubtitleCandidateSelectionPolicy.select(...)` 支持 `rememberedPath`，命中当前候选且非低置信度时优先自动应用。`PlayerViewModel` 调用自动字幕加载时传入已恢复的 `playerPrefs.externalSubtitleUri`；候选弹窗把用户选择写入 remembered path 的 UI 仍留给后续。
- **P4-LANG-001 字幕语言偏好策略、持久化与设置 UI：Done。** 已新增 `SubtitleLanguagePreference`，`PlayerPrefs` 持久化首选语言、次选语言与双语优先开关；`SubtitleCandidateSelectionPolicy.select(...)` 在同优先级候选中按双语优先、首选语言、次选语言打破平局，并保持同名等更高优先级匹配不被语言偏好覆盖。`PlayerViewModel` 自动字幕加载链路已传入当前偏好；播放器字幕设置 Sheet 已接入首选/次选语言与双语优先开关。
- **P4-DUAL-001 双字幕状态模型：Done。** 已新增 `PrimarySubtitle`、`SecondarySubtitle`、`DualSubtitleState` 与 `DualSubtitleText` 纯模型，支持按播放时间和字幕延迟解析主/副字幕文本，并在副字幕关闭时隐藏副字幕。`PlayerUiState` 已暴露 `dualSubtitles`，现有 `setSubtitles(...)` 会同步更新主字幕轨；双字幕 overlay 渲染、加载第二字幕和样式配置留给后续切片。
- **P4-DUAL-002 双字幕渲染 MVP：Done。** 播放器横竖屏布局已新增 `subtitle_stack` 与 `tv_subtitle_secondary`，主/副字幕在同一底部垂直栈内分别占位，避免互相重叠；`PlayerPlaybackTickController` 每次 tick 从 `PlayerViewModel.getCurrentDualSubtitle()` 取主/副文本并分别控制两个 TextView 的文字与可见性。第二字幕文件加载入口和快捷开关已由 P4-DUAL-003 接入，主副独立基础样式已由 P4-DUAL-004 接入。
- **P4-ONLINE-001 在线字幕服务选型：Done。** 已新增 `docs/roadmap/online-subtitle-service-selection.md`，选择 OpenSubtitles.com 作为首个手动在线字幕搜索目标，并记录 API key、User-Agent、账号/配额、错误处理和隐私边界。当前切片不添加运行时代码、SDK 依赖或自动联网；后续 `P4-ONLINE-002` 只从用户主动搜索、mockable client 和首次使用隐私提示开始。
- **P4-ONLINE-002 手动搜索/下载基础层：Done。** 已新增 `OnlineSubtitleSearchRequest`、`OnlineSubtitleSearchResult`、`OnlineSubtitleDownloadRequest`、`OnlineSubtitlePrivacyPolicy` 和 `OnlineSubtitleClient` mockable 边界；搜索请求只能通过 `manual(...)` 创建，默认不包含文件 hash，隐私策略会阻止自动打开视频触发的搜索和默认 hash 上传。`PlayerViewModel.loadSubtitles(...)` / `PlayerSubtitleController.loadSubtitlesAsync(...)` 已用 source test 固定不引用在线字幕客户端，避免自动字幕加载路径联网。本切片不接真实 OpenSubtitles API、API key、搜索 UI、下载缓存或账号登录。
- **P4-ASS-001 ASS 支持矩阵：Done。** 已新增 `docs/roadmap/ass-support-matrix.md`，按当前 `AssParser.parse(...)` / `SubtitleLoader` 路由记录 `.ass / .ssa` 的纯文本解析能力：`[Events]`、`Dialogue:`、Start/End 时间、`\N / \n` 换行和 override tags stripping；同时明确 `[V4+ Styles]`、字体、颜色、描边、定位、Karaoke、drawing/vector clips、动画、Layer/collision 与 WebDAV/NAS remote sidecar ASS/SSA 仍不支持或延后。本切片只补支持矩阵和 source test，不新增 ASS 渲染能力。
- **P4-ASS-002 ASS 常见样式增强基础层：Done。** `SubtitleItem` 已新增可选 `SubtitleCueStyle`，`AssParser` 能解析 `[V4+ Styles]` 的 `Format:` / `Style:` 行、`[Events]` 的 `Format:` 映射和 Dialogue 的 Style 引用，并把 Fontname、Fontsize、PrimaryColour、OutlineColour、Outline、Shadow、Alignment、MarginL/MarginR/MarginV 转为 per-cue style metadata；旧默认 Dialogue 格式仍保留。该基础层已给后续渲染应用提供稳定元数据。
- **P4-ASS-002 ASS 样式渲染应用 MVP：Done。** `DualSubtitleText` 现在携带主/副字幕当前 cue 的 `SubtitleCueStyle`；新增 `PlayerSubtitleCueStylePolicy`，在 `PlayerPlaybackTickController` 渲染主/副字幕时保守应用 ASS 字号、主色、描边/阴影近似和水平对齐，并在无样式时回退 `PlayerPrefs` 全局字幕字号/颜色。该切片不做 true outline stroke、字体文件加载、per-cue margin/absolute position、Karaoke、drawing 或动画。
- **P4-TOOLS-001 字幕工具箱信息诊断基础层：Done。** 已新增纯策略 `SubtitleInfoPolicy` / `SubtitleInfo` / `SubtitleInfoStatus`，可从当前已解析字幕条目生成诊断信息：`lineCount`、`time range`、总跨度、`styledLineCount`、是否包含样式、来源标签、编码标签和空字幕状态。现有字幕设置页已经包含手动编码选择和字幕延迟调整，本切片不重复 UI；信息页 UI 未包含，另存 UTF-8、延迟批量校正仍留给后续工具箱切片。

---

## 1. 成功定义

1. 常见本地剧集字幕能自动匹配，用户打开视频后无需手动找字幕。
2. 用户可设置字幕语言偏好，例如中文优先、英文次选、双语优先。
3. 支持双字幕 MVP：主字幕 + 副字幕，分别配置字号、位置和颜色。
4. 在线字幕搜索/下载默认关闭或需用户主动触发，隐私说明透明。
5. 字幕延迟、编码、另存、简单批量校正形成工具箱雏形。

---

## 2. 非目标

| 不做 | 原因 |
|------|------|
| 不做强制联网字幕 | 保护隐私，避免打开视频自动泄露标题/路径 |
| 不承诺完整 ASS 特效兼容 | Media3 字幕能力和自绘成本限制，先增强常见样式 |
| 不做 OCR 硬字幕识别 | 成本高、依赖模型、性能风险大 |
| 不做复杂字幕编辑器 | 只做播放体验相关的轻量工具 |
| 不默认上传文件 hash | 在线字幕查询需明确用户同意 |

---

## 3. 字幕匹配规则

### 3.1 本地字幕优先级

1. 同目录同完整文件名：`video.mkv` -> `video.srt`。
2. 同目录带语言后缀：`video.zh.srt`、`video.chs.ass`、`video.en.vtt`。
3. 剧集规则匹配：`S01E02`、`第02集`、`EP02`。
4. 字幕子目录：`Subs/`、`Subtitles/`、`字幕/`。
5. 同系列邻近匹配：剧名 + 集号一致。
6. 低置信度候选只提示，不自动应用。

### 3.2 语言识别

| 后缀/关键词 | 语言 |
|-------------|------|
| zh、chs、cht、cn、sc、tc、简、繁 | 中文 |
| en、eng、english | 英文 |
| ja、jp、jpn、日 | 日文 |
| ko、kr、kor、韩 | 韩文 |
| bilingual、双语、简英、chn-eng | 双语 |

---

## 4. Sprint 节奏

### Sprint 4.1：字幕自动匹配 2.0（5～7 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 4.1.1 | 字幕候选模型 | `SubtitleCandidate(path, language, confidence, reason)` | 单元测试覆盖置信度 |
| 4.1.2 | 同目录匹配增强 | 同名、语言后缀、大小写、扩展名 | 常见字幕可自动加载 |
| 4.1.3 | 剧集匹配 | 复用 Phase 2 EpisodeNameParser | S01E02/第02集 字幕匹配 |
| 4.1.4 | 字幕目录扫描 | Subs/Subtitles/字幕 子目录 | 不递归过深拖慢启动 |
| 4.1.5 | 多候选选择策略 | 自动选择高置信度；多个同分则弹出选择 | 不误选低置信度字幕 |
| 4.1.6 | 记忆用户选择 | 用户手动选过的字幕优先 | 下次打开自动应用 |

### Sprint 4.2：字幕语言偏好与双字幕 MVP（5～8 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 4.2.1 | 语言偏好设置 | 字幕首选语言、次选语言、双语优先 | 设置可持久化 |
| 4.2.2 | 主/副字幕模型 | `PrimarySubtitle` + `SecondarySubtitle` 状态 | 播放页状态清晰 |
| 4.2.3 | 双字幕渲染 MVP | 两个 TextView/overlay 或统一 presentation | 主副位置不重叠 |
| 4.2.4 | 双字幕样式 | 主/副字号、颜色、背景、位置 | 设置即时预览 |
| 4.2.5 | 快捷切换 | 播放页字幕面板可开关副字幕 | 2 次点击内完成 |
| 4.2.6 | 回退策略 | 设备/格式不支持时只显示主字幕 | 不影响播放 |

### Sprint 4.3：在线字幕搜索 MVP（5～10 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 4.3.1 | 服务选型 | OpenSubtitles 或兼容服务调研 | API、许可、账号、隐私记录 |
| 4.3.2 | 手动搜索 UI | 标题/季/集/语言输入，用户主动发起 | 不自动联网 |
| 4.3.3 | 搜索结果列表 | 语言、文件名、下载量/评分、来源 | 可预览和下载 |
| 4.3.4 | 下载与缓存 | 保存到 app cache 或同目录可选 | 缓存可清理 |
| 4.3.5 | 隐私提示 | 首次使用说明会发送哪些信息 | 用户确认后请求 |
| 4.3.6 | 失败处理 | API 限流、无结果、网络失败 | 文案清楚 |

### Sprint 4.4：ASS 样式增强与字幕工具箱（4～8 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 4.4.1 | ASS 支持矩阵 | 当前支持/忽略的样式清单 | 文档明确 |
| 4.4.2 | 常见样式增强 | 字体大小、颜色、描边、位置优先级 | 样本字幕可读性提升 |
| 4.4.3 | 编码转换 | 手动选择编码后另存 UTF-8 | 不破坏原字幕 |
| 4.4.4 | 延迟批量校正 | 当前字幕整体 ±ms 并保存副本 | 生成新文件或缓存副本 |
| 4.4.5 | 字幕信息页 | 编码、语言、行数、时间范围 | 诊断字幕问题 |

---

## 5. 详细任务清单

| ID | 优先级 | 任务 | 依赖 | 验证 |
|----|--------|------|------|------|
| P4-MATCH-001 | P0 | SubtitleCandidate 模型 | 无 | **Done（2026-06-06）：`SubtitleCandidate(path, language, confidence, reason)` 纯模型 + 语言识别 + 置信度单元测试** |
| P4-MATCH-002 | P0 | 自动匹配 2.0 | Phase 2 parser 可选 | **Done（2026-06-06）：同目录同名/语言后缀/大小写/扩展名 matcher + loader 复用；剧集/子目录/多候选选择未包含** |
| P4-MATCH-003 | P0 | 剧集规则匹配 | P4-MATCH-002、Phase 2 parser | **Done（2026-06-06）：复用 `EpisodeNameParser` 匹配同剧名/同季/同集字幕，覆盖 S01E02 与 第02集；子目录/邻近扫描未包含** |
| P4-MATCH-004 | P0 | 字幕目录扫描 | P4-MATCH-002 | **Done（2026-06-06）：一层扫描 `Subs` / `Subtitles` / `字幕`，不深递归；loader 已接入 scanner + matcher** |
| P4-MATCH-005 | P0 | 多候选选择策略 | P4-MATCH-004 | **Done（2026-06-07）：唯一最高优先级自动应用；多个同优先级通过播放器单选弹窗让用户选择；点选后写入 `externalSubtitleUri` 以复用既有加载与记忆路径；低置信度不自动应用** |
| P4-MATCH-006 | P0 | 记忆用户选择 | P4-MATCH-005 | **Done（2026-06-06）：复用历史 `externalSubtitleUri`，selection policy 支持 remembered path 优先；候选弹窗写入 UI 未包含** |
| P4-MATCH-007 | P0 | 自动字幕扫描预算 | P4-MATCH-004 | **Done（2026-06-07）：`SubtitleFileCandidateScanner` 每个扫描目录最多保留 40 个支持的字幕候选，并优先保留与视频 basename 相关的文件；仍只扫同目录和一层字幕目录，不递归更深目录** |
| P4-LANG-001 | P1 | 字幕语言偏好 | P4-MATCH-001 | **Done（2026-06-07）：`SubtitleLanguagePreference` + `PlayerPrefs` 三项持久化字段；同优先级候选按双语/首选/次选语言打破平局；自动加载链路已接入；字幕设置 Sheet 已接首选语言、次选语言和双语优先开关** |
| P4-DUAL-001 | P1 | 双字幕状态模型 | P4-LANG-001 | **Done（2026-06-06）：`PrimarySubtitle` / `SecondarySubtitle` / `DualSubtitleState` / `DualSubtitleText` 纯模型；`PlayerUiState.dualSubtitles` 已接入；渲染与副字幕加载 UI 未包含** |
| P4-DUAL-002 | P1 | 双字幕渲染 MVP | P4-DUAL-001 | **Done（2026-06-06）：横竖屏新增主/副字幕垂直 overlay；tick 分别渲染主/副文本并隐藏空行；基础样式/位置沿用当前字幕设置** |
| P4-DUAL-003 | P1 | 第二字幕加载入口与开关 | P4-DUAL-002 | **Done（2026-06-07）：字幕设置 Sheet 新增加载第二字幕与显示第二字幕开关；复用 `PlayerSubtitleLoadCoordinator` 加载并写入 `DualSubtitleState.secondary`；不保存第二字幕历史，不做主副独立样式** |
| P4-DUAL-004 | P1 | 主副字幕样式独立 | P4-DUAL-003 | **Done（2026-06-07）：副字幕字号、颜色、背景、位置拥有独立 `PlayerPrefs` 偏好；未设置时继承主字幕样式；播放页、ASS fallback、Sheet 和旧 Activity 均分别应用与预览** |
| P4-ONLINE-001 | P1 | 在线字幕服务选型 | 无 | **Done（2026-06-06）：新增 `online-subtitle-service-selection.md`，选择 OpenSubtitles.com 作为首个手动搜索目标；记录 API key / User-Agent / 账号与配额 / 隐私边界；未添加运行时依赖或自动联网** |
| P4-ONLINE-002 | P1 | 手动搜索/下载 | P4-ONLINE-001 | **Done（2026-06-06，基础层）：手动搜索请求/结果/下载模型 + mockable `OnlineSubtitleClient` + 隐私门禁；自动字幕加载路径不触发在线搜索；真实 API/UI/缓存未包含** |
| P4-ONLINE-003 | P1 | 首次隐私提示入口 | P4-ONLINE-002 | **Done（2026-06-07）：字幕设置 Sheet 新增手动“在线字幕搜索”入口；点击先展示隐私提示，说明会发送标题、季、集和语言且默认不上传文件 hash；用户确认后才进入后续搜索流程。当前只占位提示，不调用真实搜索 API** |
| P4-ASS-001 | P2 | ASS 支持矩阵 | 无 | **Done（2026-06-06）：新增 `docs/roadmap/ass-support-matrix.md`，按当前 parser/loader 记录 `.ass / .ssa` 纯文本解析能力和样式/定位/特效/远程 sidecar 限制；样式增强留给 P4-ASS-002** |
| P4-ASS-002 | P2 | ASS 常见样式增强 | P4-ASS-001 | **Done（2026-06-06）：`SubtitleCueStyle` + `[V4+ Styles]` / Dialogue Style 解析；主/副字幕 TextView 保守应用字号、主色、描边/阴影近似和水平对齐；true stroke/字体加载/绝对定位/Karaoke/drawing/动画未包含** |
| P4-TOOLS-001 | P2 | 字幕工具箱 MVP | P4-MATCH-002 | **Done（2026-06-07，基础层 + UTF-8 导出入口 + 信息摘要 UI + 延迟校正导出入口 + 缓存副本策略 + 原字幕覆盖保护）：`SubtitleInfoPolicy` 汇总 lineCount、time range、styledLineCount、编码/来源标签和空字幕状态；字幕设置 Sheet 已通过 `currentSubtitleInfo()` / `PlayerSubtitleInfoUiPolicy` 展示来源、编码、行数、时间范围和样式行数；`SubtitleDelayCorrectionPolicy` 生成整体 ±ms 校正副本计划；`SubtitleUtf8ExportPolicy` 生成 UTF-8 SRT content/bytes，并通过 `targetsOriginalSubtitle(...)` 识别目标等于当前记忆外部字幕的未确认覆盖；`SubtitleExportWriter` 可写入调用方提供的 OutputStream；字幕设置 Sheet 已接 `CreateDocument("application/x-subrip")` 和 `writeCurrentSubtitleUtf8ExportTo(...)` 导出当前已加载字幕副本，也已接 `writeCurrentSubtitleDelayCorrectionExportTo(...)` 按当前字幕延迟导出校正副本；两条导出路径会在 `openOutputStream(...)` 前以 `OriginalOverwriteBlocked` 拦截原字幕覆盖；`SubtitleCacheCopyPolicy` 已规划 app cache 字幕副本命名与保留策略，真实缓存文件写入/删除 UI 未包含** |

---

## 6. QA 字幕样本

| 类型 | 样本 |
|------|------|
| SRT UTF-8 | 中文、英文、双语 |
| SRT GBK/Big5 | 中文乱码修复验证 |
| VTT | Web 字幕 |
| ASS 简单样式 | 字体、颜色、描边 |
| ASS 复杂样式 | 特效忽略但不崩溃 |
| 多字幕候选 | zh/en/双语同时存在 |
| 剧集字幕 | S01E02、第02集、EP02 |
| 远程字幕 | WebDAV 同目录字幕 |

---

## 7. 验收清单

- [x] 自动字幕匹配 2.0 不明显拖慢视频启动。
- [x] 多候选时不会低置信度误选。
- [x] 字幕语言偏好影响自动选择。
- [x] 双字幕可开关，主副字幕样式独立。
- [x] 在线字幕搜索必须用户主动触发，并有隐私提示。
- [x] 字幕工具箱不会覆盖原文件，除非用户明确确认。

---

## 8. Phase 结束输出

1. 字幕匹配规则文档。
2. 字幕语言偏好说明。
3. 双字幕功能说明与已知限制。
4. 在线字幕隐私说明。
5. ASS 支持矩阵。
