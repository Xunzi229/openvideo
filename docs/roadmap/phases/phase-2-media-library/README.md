# Phase 2：本地影视库体验升级

**建议周期：** 3～6 周  
**目标版本：** v0.3.0 本地影视库版  
**阶段目标：** 从「视频文件列表」升级为「本地影视库」，重点解决稳定身份、剧集识别、海报缓存、智能列表和继续观看聚合。

**当前状态（2026-06-06）：** `P2-EP-001 EpisodeNameParser` 纯解析器切片已完成，覆盖常见英文季集、中文集数、括号动漫命名、父目录兜底与保守拒绝规则。`P2-ID-FOUNDATION` 媒体身份纯策略地基已完成，包含 `MediaPathNormalizer` 与 `MediaFingerprintPolicy`。`P2-ID-001` 媒体身份 Room schema 切片已完成，包含 `media_identity` / `media_path_history`、`MIGRATION_5_6`、数据库版本 6 与 `MediaIdentityDao` 基础 upsert/query；迁移复合索引名与 Room entity 显式对齐，identity upsert 不使用 `REPLACE`，避免更新身份时删除旧行并级联清空路径历史；adb 真机验证发现 `media_path_history.exists` 裸列名触发 SQLite 关键字语法错误后，已统一改为 `fileExists` 并补回归测试。`P2-ID-002` 多因子匹配策略已完成纯 Kotlin `MediaIdentityMatcher`，覆盖同 id、同路径、路径迁移、重命名候选、多候选冲突和指标不同不匹配；`testDebugUnitTest` / `assembleDebug` 通过。`P2-ID-003` 扫描 identity sync、`mediaIdentityId` 记录、identity-aware history prune、history lookup identity fallback、favorite identity fallback 和 playlist identity fallback 子切片已完成；历史、收藏、播放列表在旧 `videoId` 缺失但 identity 当前视频仍在扫描结果中时可尽量恢复；`testDebugUnitTest` 与 `assembleDebug` 通过。`P2-EP-002 Series/Episode 表` 已完成：新增 v8、`MIGRATION_7_8`、`SeriesEntity` / `EpisodeEntity`、`SeriesEpisodeDao` 与 Hilt DAO provider；本切片不接扫描写入、首页入口或剧集详情页；`testDebugUnitTest` / `assembleDebug` 通过。`P2-EP-003` 剧集详情页 MVP 已完成：repository 扫描生成 series/episode rows 已接入，`VideoRepository.getAllSeries()` 与 playable episode flow 已作为后续 ViewModel 的数据出口；`SeriesDetailViewModel` / `SeriesListViewModel` 只消费 repository flow，`SeriesEpisodeUiState` 覆盖季集/多集/无季标签并可转 `VideoItem`；`SeriesDetailFragment` / `SeriesEpisodeAdapter` / XML 可展示剧名、返回、空态与 episode rows，点击单集可启动 `PlayerActivity` 并带同剧集队列；`SeriesListFragment` / `SeriesAdapter` / XML 可展示剧集列表并导航到详情页；Local 文件夹页 header 已增加剧集入口。`P2-EP-004` 剧集观看状态已完成：history progress read model、完成判定 policy、详情页未看/进度/已完成 meta 文案、缺失文件标签、不可播放项置灰且不进入播放队列均已落盘。`P2-ART-001 LocalArtworkFinder` 纯策略已完成：同目录 `poster`、`folder`、`cover`、同名图片优先级和 jpg/jpeg/png/webp 支持已覆盖。`P2-ART-002` 剧集本地海报缓存/UI 已完成：扫描生成 series 时枚举同目录图片候选并写入/刷新 `SeriesEntity.posterPath`，Series 列表使用 Glide 加载本地海报，缺失时降级默认图标。`P2-LIST-001 MediaSmartListPolicy` 纯策略已完成：最近添加、未看完、已看完、大文件、UHD、HDR、带字幕过滤/排序/limit 规则已覆盖。`P2-LIST-002` Home 智能列表状态出口已完成：`HomeSmartListBuilder` 从扫描视频与历史进度生成非空 smart list sections，`HomeViewModel.smartLists` 暴露给后续首页 UI 消费。`P2-LIST-003` Home 智能列表 Chip 过滤 UI 已完成：All 分类下新增智能列表 chip 行，支持和文件夹、搜索、高级筛选、排序叠加；不改变 Recent/Favorites 结构。`P2-PL-001` 播放列表重排已完成：详情页支持长按拖拽重排并持久化 position。`P2-PL-002` 播放列表 M3U/JSON 导入导出 MVP 已完成：详情页可 SAF 导出 JSON、导入 JSON/M3U，并对无效/空文件 Toast 降级。`P2-PL-003` 播放列表批量清理策略入口已完成：`PlaylistCleanupPolicy` 统一规划缺失文件与重复项清理，重复项按 identity/path 保守识别并保留最早 position。`P2-PL-004` 播放列表确认式清理 UI 已完成：详情页新增清理图标按钮，确认后才调用 cleanup 入口；撤销仍留给后续抛光。

**补充状态（2026-06-06）：** `P2-LIST-004` Home 智能列表筛选联动已完成：切出 All 分类时重置智能列表选择，空态只在 All 分类把智能列表计为主动筛选，避免 Recent/Favorites 与隐藏 smart chip 状态互相打架。

**补充状态（2026-06-06）：** `P2-PL-005` 播放列表清理撤销已完成：确认清理后返回被移除 rows，详情页显示 Snackbar Undo，撤销时恢复原播放列表条目，补齐 2.5.4 的撤销验收。

**补充状态（2026-06-06）：** `P2-EP-005` 低置信度剧集聚合保护已完成：repository 遇到 `EpisodeMatchConfidence.LOW` 不自动写入 `series` / `episodes`，保留给后续人工确认；HIGH/MEDIUM 既有聚合路径不变。

**补充状态（2026-06-06）：** `R-08` Home 文件夹置顶提示抛光已完成：文件夹 chips 下方显示长按置顶/取消置顶提示，并与多个文件夹 chips 的可见状态同步，避免无 chips 时出现孤立提示。

**补充状态（2026-06-06）：** `R-13` Seek 缩略图内存缓存已完成：新增 1 秒时间桶 cache key 与进程内 LRU bitmap cache，拖动同一视频相邻时间点时优先复用已抽帧结果；后续仅保留设置入口与真机性能抛光。

**补充状态（2026-06-06）：** `R-13` Seek 缩略图设置入口已完成：播放器设置的播放页新增缩略图预览开关，复用已有 `PlayerPrefs.seekThumbnailEnabled`，默认开启；后续仅保留真机性能抛光。

**补充状态（2026-06-06）：** `R-13` Seek 缩略图性能抛光已完成：抽帧结果按 240x135 上限等比缩放后再进入内存缓存和 UI 展示，避免拖动预览缓存原始大帧；已补策略与 loader 源码回归测试。

---

## 1. 成功定义

1. 文件移动、重命名、MediaStore id 变化后，历史、收藏、播放列表尽量不断链。
2. 常见剧集命名能自动识别季/集/剧名，并生成剧集详情页。
3. 首页具备「继续观看」「最近添加」「剧集」「文件夹」「智能列表」的信息架构。
4. 支持同目录本地海报，优先离线，不强制联网刮削。
5. 智能列表能帮助用户快速找到未看完、已看完、4K/HDR、大文件、带字幕等内容。

---

## 2. 非目标

| 不做 | 原因 |
|------|------|
| 不默认联网拉 TMDb | 隐私和 API 配置成本，先做本地识别与海报 |
| 不做账号同步 | 跨设备进度等 Phase 之后再考虑 |
| 不做复杂媒体服务器 | Jellyfin/Plex/Emby 归 Phase 3+ 调研 |
| 不强行识别所有文件名 | 命名识别以置信度和可编辑为原则 |
| 不移除现有文件夹浏览 | 新影视库与文件夹浏览并存，避免破坏老习惯 |

---

## 3. 数据与架构设计

### 3.1 新模块建议

```text
core/mediaid/
  MediaIdentity.kt
  MediaIdentityMatcher.kt
  MediaFingerprintPolicy.kt
  MediaPathNormalizer.kt

core/metadata/
  EpisodeNameParser.kt
  EpisodeMatch.kt
  LocalArtworkFinder.kt
  MediaSmartListPolicy.kt

data/local/
  MediaIdentityEntity.kt
  SeriesEntity.kt
  EpisodeEntity.kt
  MediaArtworkEntity.kt
```

### 3.2 新表建议

| 表 | 字段重点 | 说明 |
|----|----------|------|
| `media_identity` | identityId、currentVideoId、path、normalizedPath、size、duration、width、height、modifiedTime、firstSeen、lastSeen | 稳定媒体身份 |
| `media_path_history` | identityId、path、seenAt、fileExists | 支持路径迁移与回溯 |
| `series` | id、title、normalizedTitle、folderPath、posterPath、createdAt、updatedAt | 剧集聚合 |
| `episodes` | id、seriesId、identityId、season、episode、episodeTitle、confidence | 单集映射 |
| `media_artwork` | ownerType、ownerId、artworkPath、source、updatedAt | 本地海报缓存 |

### 3.3 匹配策略优先级

1. **强匹配：** 原 MediaStore id + path 未变。
2. **路径迁移匹配：** 文件名相同 + size/duration 相同 + modifiedTime 接近。
3. **重命名匹配：** 同目录 + size/duration 相同 + 分辨率相同。
4. **弱匹配：** duration 接近 + size 接近 + 标题相似。
5. **人工确认：** 低置信度时不自动合并，后续可做「可能是同一文件」提示。

---

## 4. Sprint 节奏

### Sprint 2.1：稳定媒体身份模型（5～7 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 2.1.1 | 设计 Room 表和迁移 | `MediaIdentityEntity`、迁移测试 | 旧数据库升级不丢历史 |
| 2.1.2 | 路径标准化 | 统一 `/`、大小写策略、末尾分隔符、content URI | 单元测试覆盖 Windows-like/Android 路径 |
| 2.1.3 | 指纹策略 | size/duration/width/height/modifiedTime/title 组合 | 测试覆盖 id 变更、路径变更、重命名 |
| 2.1.4 | 扫描后合并 | `VideoScanner` 输出后由 repository 关联 identity | 不阻塞扫描 UI |
| 2.1.5 | 历史迁移 | history/favorite/playlist 能通过 identity 找回 | 文件移动后继续观看仍可用 |
| 2.1.6 | 冲突处理 | 多个候选 identity 时保守不合并，记录诊断 | 不误合并不同视频 |

### Sprint 2.2：剧集命名识别（4～6 天）

| 切片 | 任务 | 示例 | 验收 |
|------|------|------|------|
| 2.2.1 | 英文 SxxEyy | `Show.Name.S01E02.1080p.mkv` | 识别 title=Show Name, season=1, episode=2 |
| 2.2.2 | EP/第几集 | `某剧 第02集.mp4`、`EP12` | 中文/英文常见规则通过测试 |
| 2.2.3 | 多集文件 | `S01E01-E02`、`01-02` | 至少能标记 episodeStart/end 或保守识别 |
| 2.2.4 | 清理噪声 token | 1080p、WEB-DL、x264、字幕组、年份 | title 不包含噪声 |
| 2.2.5 | 置信度模型 | high/medium/low | 低置信度不自动聚合或标记待确认 |
| 2.2.6 | 文件夹辅助 | 同目录标题补全剧名 | 单集文件名过短时仍可聚合 |

**测试样本建议：** 中英日韩命名、动漫字幕组命名、电影带年份、纪录片多集、无集号文件。

### Sprint 2.3：剧集聚合与详情页 MVP（5～8 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 2.3.1 | Series/Episode 写入 | 扫描后生成 series 和 episodes | 重复扫描不重复插入 |
| 2.3.2 | 首页剧集入口 | 新增「剧集」区或实验入口 | 不破坏现有 Local 页 |
| 2.3.3 | 剧集详情页 | 展示季、集、观看进度、继续播放 | 点击单集进入播放器队列 |
| 2.3.4 | 已看/未看状态 | 基于 history progress 和 completion policy | 已看完显示明确状态 |
| 2.3.5 | 剧集排序 | season、episode、自然排序 | S01E10 不排在 S01E02 前 |
| 2.3.6 | 缺失文件提示 | 历史存在但文件缺失时展示 | 不崩溃，可清理 |

### Sprint 2.4：本地海报与智能列表（4～7 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 2.4.1 | 本地海报发现 | `poster.jpg`、`folder.jpg`、`cover.png`、同名图片 | 单元测试覆盖优先级 |
| 2.4.2 | 海报缓存 | Room 记录 + Glide 加载 | 文件变更可刷新 |
| 2.4.3 | 智能列表策略 | 最近添加、未看完、已看完、大文件、4K/HDR、带字幕 | 每个列表有独立 policy 测试 |
| 2.4.4 | 首页信息架构 | 继续观看、剧集、最近添加、文件夹/智能列表 | 空状态友好 |
| 2.4.5 | 筛选联动 | 高级筛选与智能列表不互相打架 | 状态可重置 |

### Sprint 2.5：播放列表与数据整理增强（3～5 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 2.5.1 | 播放列表 identity 化 | PlaylistVideo 关联稳定 identity | MediaStore id 变化后列表不丢 |
| 2.5.2 | 拖拽排序 | 列表详情页支持手动排序 | position 持久化 |
| 2.5.3 | 导入导出 | M3U/JSON MVP | 导入无效路径有提示 |
| 2.5.4 | 批量清理 | 缺失文件、重复项清理 | 有确认和撤销/保守策略 |

---

## 5. 详细任务清单

| ID | 优先级 | 任务 | 依赖 | 验证 |
|----|--------|------|------|------|
| P2-ID-001 | P0 | ✅ MediaIdentity 数据模型 | `P2-ID-FOUNDATION` | `media_identity` / `media_path_history`、`MIGRATION_5_6`、版本 6、`MediaIdentityDao` 基础 upsert/query 已完成；索引名对齐 Room schema，identity upsert 避免 `REPLACE`；`testDebugUnitTest` / `assembleDebug` 通过 |
| P2-ID-002 | P0 | ✅ 多因子匹配策略 | P2-ID-001 | `MediaIdentityMatcher` 已完成同 id、同路径、路径迁移、重命名候选、多候选冲突保守拒绝与指标不同不匹配；`testDebugUnitTest` / `assembleDebug` 通过；未接扫描/仓库 |
| P2-ID-003 | P0 | ✅ History/Favorite/Playlist identity 接入 | P2-ID-002 | 扫描 identity sync、`mediaIdentityId` 记录、identity-aware history prune、history lookup fallback、favorite fallback 与 playlist fallback 已完成；历史、收藏、播放列表可通过 identity 尽量恢复；`testDebugUnitTest` / `assembleDebug` 通过 |
| P2-ID-FOUNDATION | P0 | ✅ MediaPathNormalizer + MediaFingerprintPolicy 纯策略地基 | 无 | 路径归一化、指纹创建、强匹配、重命名候选匹配已覆盖；不含 Room 迁移 |
| P2-EP-001 | P0 | ✅ EpisodeNameParser 纯解析器切片 | 无 | `EpisodeNameParserTest` 覆盖 SxxEyy、1x02、多集范围、中文集数、括号动漫、父目录兜底和拒绝规则 |
| P2-EP-002 | P1 | ✅ Series/Episode 表 | P2-ID-001 | v8、`MIGRATION_7_8`、`SeriesEntity` / `EpisodeEntity`、`SeriesEpisodeDao` 与 Hilt DAO provider 已完成；不接扫描写入或 UI；`testDebugUnitTest` / `assembleDebug` 通过 |
| P2-EP-003 | P1 | ✅ 剧集详情页 MVP | P2-EP-002 | 前置扫描生成 series/episode rows、repository playable flow、Series 详情/列表状态层、详情页与列表 Fragment/Adapter/XML、Local 页剧集入口、点击单集启动播放器并带同剧集队列已完成；聚焦 `ui.series.*`、`LocalSeriesEntrySourceTest`、播放队列 source tests、完整 `testDebugUnitTest`、`assembleDebug`、`lintDebug`、`git diff --check` 已按切片验证 |
| P2-EP-004 | P1 | ✅ 剧集观看状态 | P2-EP-003 | 观看状态微切片 + 缺失文件降级已完成；history progress、完成判定、详情页 meta 标签、缺失文件标签、不可播放项置灰且不进入播放队列均已落盘 |
| P2-EP-005 | P1 | ✅ 低置信度剧集聚合保护 | P2-EP-003 | repository 遇到 `EpisodeMatchConfidence.LOW` 不自动写入 `series` / `episodes`，保留给后续人工确认；HIGH/MEDIUM 聚合路径不变；剧集解析、repository source 与 series 聚焦测试通过 |
| P2-ART-001 | P1 | ✅ LocalArtworkFinder | P2-EP-002 | `core/metadata/LocalArtworkFinder` 纯策略已完成；同目录 poster/folder/cover/同名图片优先级、jpg/jpeg/png/webp、路径大小写与分隔符归一化已覆盖；未接 Room、扫描、Glide 或 UI |
| P2-ART-002 | P1 | ✅ 剧集本地海报缓存/UI | P2-ART-001 | 扫描生成 series 时枚举同目录图片候选并写入/刷新 `SeriesEntity.posterPath`；Series 列表使用 Glide 加载本地海报，缺失时降级默认图标；聚焦海报候选、repository source 与 adapter source 测试通过 |
| P2-LIST-001 | P1 | ✅ SmartListPolicy | P2-ID-003 | `core/metadata/MediaSmartListPolicy` 纯策略已完成；最近添加、未看完、已看完、大文件、UHD、HDR、带字幕过滤/排序/limit 规则已覆盖；未接首页、Room、扫描或 UI |
| P2-LIST-002 | P1 | ✅ Home 智能列表状态出口 | P2-LIST-001 | `HomeSmartListBuilder` 从扫描视频与历史进度生成非空 smart list sections，`HomeViewModel.smartLists` 暴露给后续首页 UI 消费；暂不改变现有 All/Recent/Favorites 页面；聚焦 builder/source 测试通过 |
| P2-LIST-003 | P1 | ✅ Home 智能列表 Chip 过滤 UI | P2-LIST-002 | Home All 分类新增智能列表 chip 行，支持按最近添加、未看完、已看完、大文件、UHD/HDR、带字幕过滤，并继续叠加文件夹/搜索/高级筛选/排序；不改变 Recent/Favorites 结构 |
| P2-LIST-004 | P1 | ✅ Home 智能列表筛选联动 | P2-LIST-003 | 切出 All 分类时重置智能列表选择，空态只在 All 分类把智能列表计为主动筛选；避免 Recent/Favorites 与隐藏 smart chip 状态互相打架；Home 聚焦测试通过 |
| P2-PL-001 | P2 | ✅ 播放列表重排 | P2-ID-003 | 播放列表详情页支持长按拖拽重排；`PlaylistReorderPolicy` 归一化 position，`PlaylistDao.updatePositions` 持久化；播放列表聚焦测试、`assembleDebug`、`lintDebug`、adb 安装启动验证通过 |
| P2-PL-002 | P2 | ✅ M3U/JSON 导入导出 | P2-PL-001 | 播放列表详情页支持 SAF 导出 JSON、导入 JSON/M3U；格式层、导入去重/临时 id 分配、UI wiring 与 Toast 降级已覆盖；播放列表聚焦测试、`assembleDebug`、`lintDebug`、adb 安装启动验证通过 |
| P2-PL-003 | P2 | ✅ 播放列表批量清理策略入口 | P2-PL-002 | `PlaylistCleanupPolicy` 统一规划缺失文件和重复项清理，重复项按 `mediaIdentityId` 优先、再按归一化路径识别并保留最早 position；`PlaylistViewModel.cleanupPlaylistVideos` 提供入口；确认/撤销 UI 未接入，留给后续切片 |
| P2-PL-004 | P2 | ✅ 播放列表确认式清理 UI | P2-PL-003 | 播放列表详情页新增清理图标按钮，点击后弹出确认框，确认后才移除不可用和重复条目；撤销未接入，留给后续抛光 |
| P2-PL-005 | P2 | ✅ 播放列表清理撤销 | P2-PL-004 | 清理确认后返回被移除 rows，详情页显示 Snackbar Undo；点击撤销通过 `restorePlaylistVideos` 恢复条目，补齐 2.5.4 的撤销验收；播放列表聚焦测试通过 |

---

## 6. QA 样本库建议

| 类型 | 样本 |
|------|------|
| 电影 | `Movie.Name.2024.1080p.BluRay.x264.mkv` |
| 美剧 | `Show.Name.S01E02.2160p.WEB-DL.mkv` |
| 中文剧 | `剧名 第12集 1080P.mp4` |
| 动漫 | `[字幕组][动画名][03][1080P][简日双语].mkv` |
| 多集 | `Show.S01E01-E02.mkv` |
| 无集号 | `Special.Bonus.Video.mp4` |
| 移动文件 | 同一文件从 A 目录移动到 B 目录 |
| 重命名 | 同一文件改名但 size/duration 不变 |

---

## 7. 验收清单

- [ ] 文件移动/重命名后，历史、收藏、播放列表尽量恢复。
- [ ] 常见中英文剧集命名识别准确，低置信度不会误聚合。
- [ ] 剧集详情页可从继续观看进入下一集队列。
- [x] 本地海报可发现、缓存、刷新、缺失降级。
- [x] 智能列表有独立策略测试。
- [ ] 数据库迁移测试覆盖旧版本升级。

---

## 8. Phase 结束输出

1. v0.3.0 Release Notes 草稿。
2. MediaIdentity 设计说明与迁移记录。
3. EpisodeNameParser 规则和样本集。
4. 首页信息架构更新说明。
5. Phase 3 网络来源需要复用的 source/identity 接口清单。
