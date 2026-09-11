# Phase 3：网络与多来源播放

**建议周期：** 4～8 周  
**目标版本：** v0.4.0 网络来源版  
**阶段目标：** 从纯本地播放器扩展到 URL、HLS/DASH/RTSP、WebDAV/NAS 等多来源，但仍保持离线优先、隐私透明、错误可解释。

**补充状态（2026-06-06）：** `P3-URL-002` URL 校验规范化纯策略已完成：新增 `core/network/NetworkUrlPolicy`，支持 `http` / `https` / `rtsp` 播放 URL 的复制粘贴清理、scheme/host 校验、协议与非法字符拒绝，并统一小写 scheme/host；暂不接 UI、播放器或最近记录。

**补充状态（2026-06-06）：** `P3-NET-001` NetworkErrorClassifier 纯策略已完成：新增 `core/network/NetworkErrorClassifier`，按 Media3 playback errorCode 与 Java 网络异常区分连接失败、DNS、超时、HTTP 状态、明文 HTTP 禁止、未知网络和非网络错误，并输出 retryable 标记；暂不改 Phase 0 HUD 或播放器接入。

**补充状态（2026-06-06）：** `P3-NET-002` 网络缓冲策略已完成：`PlayerBufferingPolicy` 现覆盖本地、HTTP progressive、HLS、DASH 与 RTSP；HLS/DASH 识别会忽略 query/fragment，RTSP 使用独立低启动缓冲 profile；暂不改播放器 UI 或网络状态提示。

**补充状态（2026-06-06）：** `P3-NET-003` Header/UA 配置已完成：新增 `NetworkPlaybackHeaderPolicy`，统一生成 OpenVideo 播放 User-Agent、校验可选 HTTP(S) Referer，并提供敏感 header 脱敏输出；`PlayerManager` 使用 `DefaultHttpDataSource.Factory` + `DefaultMediaSourceFactory` 接入播放器网络请求链路，默认不保存或注入 Authorization/Cookie/token 类敏感 header。adb 已验证 HTTPS URL 仍进入 `PlayerActivity` 且无 OpenVideo 崩溃；本地 HTTP header probe 被系统 cleartext policy 拦截，符合现有明文 HTTP 禁止策略。

**补充状态（2026-06-06）：** `P3-NET-004` 失败重试策略已完成：新增 `NetworkPlaybackRetryPolicy`，对可重试网络错误执行 1s / 2s / 4s 三次有界指数退避；`NetworkErrorClassifier` 读取 Media3 HTTP 状态异常并复用 `classifyHttpStatus`，404/401/403 不自动重试，503/429/超时/连接失败可重试。`PlayerViewModel` 持有自动重试预算，`PlayerEventController` 在自动重试已安排时暂不弹错误 HUD，预算耗尽后回到既有手动重试 HUD。adb 已验证 404 URL 不重复自动重试且无崩溃；不存在域名触发首次错误 + 3 次自动重试，随后不再快速请求且无 OpenVideo `FATAL EXCEPTION`。

**补充状态（2026-06-06）：** `P3-NET-005` 网络状态提示已完成：新增 `PlayerNetworkStatusPolicy`，网络来源在 `STATE_BUFFERING` 显示“缓冲中”，自动重试等待期间显示“正在重连”，READY 且 Media3 标记 live 或 duration unknown 时显示“直播”；本地文件不显示网络状态。竖屏/横屏播放器控制层新增同名 `tv_network_status` 标签，由 `PlayerEventController` 统一更新，不改变现有错误 HUD 与手动重试入口。adb 已验证 DNS 错误 + 自动重试路径仍停留在 `PlayerActivity`，无 OpenVideo `FATAL EXCEPTION` 或资源异常。

**补充状态（2026-06-06）：** `P3-NET-006` 直播/点播差异已完成：新增 `PlaybackTimelinePolicy` 统一判断点播可 seek duration；`PlayerManager.seekForward/seekBackward` 在直播或 unknown duration 下不再提交相对 seek；播放器总时长标签改为 `PlayerTimeline.durationText`，unknown/live duration 显示 `--:--`，避免显示 `00:00` 或异常超大总时长。聚焦测试 `PlaybackTimelinePolicyTest` 与 `PlayerTimelineTest` 已通过；adb 验证 live HLS 失效样例进入网络错误 HUD 无崩溃，Apple HTTPS HLS 示例进入 `PlayerActivity`，日志无 OpenVideo `FATAL EXCEPTION`。

**补充状态（2026-06-06）：** `P3-SRC-001` 来源管理数据模型已完成：数据库升到 v9，新增 `media_sources` 非敏感来源元数据表、`MediaSourceEntity` / `MediaSourceDao`、`MIGRATION_8_9` 与 Hilt provider；表不保存用户名、密码、Header 或 token，凭据安全存储留给 WebDAV 添加切片。

**补充状态（2026-06-06）：** `P3-SRC-002` 来源信息架构已完成：底部导航新增“来源”页，集中展示本地媒体库、打开 URL、WebDAV 与未来 SMB/Jellyfin/Plex 占位；Home 与来源页共用 `NetworkOpenUrlDialog`，复用 `NetworkUrlPolicy`、`NetworkRecentUrlPolicy` 与 `PlayerActivityIntents`，避免重复打开链接逻辑；WebDAV 行仅显示规划状态，不保存凭据也不发起连接，后续实现仍留给 `P3-WD-002`。adb 已验证“来源”页可打开，来源页“打开链接”弹出共享 URL 对话框，日志无 OpenVideo `FATAL EXCEPTION`。

**补充状态（2026-06-06）：** `P3-SRC-003` 最近播放聚合已完成：来源页新增“最近播放”区，`SourceRecentPlaybackPolicy` 合并本地继续观看历史与网络最近 URL，并按最近播放时间排序；本地行显示 `Local` 标签、文件夹图标和继续观看进度，网络行显示 `URL` 标签、流媒体图标和脱敏 URL。策略会过滤 `http` / `https` / `rtsp` 播放历史，避免同一网络 URL 同时显示为“本地缺失”和“URL”；本地缺失文件保留但不可点击播放，网络最近项点击复用 `PlayerActivityIntents.networkPlayback(...)` 并刷新最近记录。adb 已验证来源页显示 `URL` 最近播放且点击进入 `PlayerActivity`，日志无 OpenVideo `FATAL EXCEPTION`。

**补充状态（2026-06-06）：** `P3-SRC-004` 来源详情页已完成：网络 URL 最近记录会同步维护非敏感 `url` 类型 `media_sources` 元数据，来源页新增“已保存来源”列表，点击可进入 `SourceDetailFragment` 查看名称、脱敏地址、最后使用时间，并提供测试连接和删除入口。测试连接当前只做 URL 规范校验，不发起 WebDAV 或凭据连接；删除来源确认已统一复用“我的/设置页清除播放历史”同款 `SettingsConfirmationActionSheet` 底部确认样式，`docs/design-system.md` 已补充所有删除/清除破坏性操作后续统一规则。adb 已验证打开 URL 后生成已保存来源、进入详情页、删除来源底部确认 Sheet 和取消操作均无 OpenVideo `FATAL EXCEPTION`。

**补充状态（2026-06-06）：** `P3-SRC-005` 凭据安全提示已完成：新增 `SourceCredentialPrivacyPolicy`，来源页和来源详情页会展示凭据保存位置、导出策略和诊断脱敏策略；新增 `docs/roadmap/network-source-privacy.md` 作为网络来源隐私说明。设置导出敏感 marker 补齐 `cookie` / `authorization` / `header`，与既有 `password` / `token` 一起防止敏感字段进入设置导出。adb 已验证来源页与来源详情页隐私提示可见且无 OpenVideo `FATAL EXCEPTION`。

**补充状态（2026-06-06）：** `P3-WD-001` WebDAV 依赖选型已完成：选择 OkHttp 5.3.2 直接发起 RFC 4918 `PROPFIND` 请求，后续用 MockWebServer 做连接/目录浏览回归；不引入完整 WebDAV 客户端库，降低 Android 兼容和依赖维护风险。详见 `docs/roadmap/webdav-dependency-selection.md`。

**补充状态（2026-06-06）：** `P3-WD-002` 添加 WebDAV 来源基础已完成：来源页 WebDAV 行打开添加对话框，输入 base URL、用户名、密码后先发起 `PROPFIND Depth: 0` 测试连接；测试成功才保存 `webdav` 类型来源元数据，凭据写入 AndroidX Security `EncryptedSharedPreferences`，Room 与导出策略只保留非敏感名称/地址。`WebDavConnectionPolicyTest` 与 `WebDavSourceSelectionSourceTest` 通过；adb 已安装并确认 OpenVideo 进程启动且无 `AndroidRuntime:E` 崩溃输出，但当前设备停在系统锁屏/通知层，WebDAV 对话框点击验证待设备解锁后补跑。目录浏览、播放和远程字幕留给 `P3-WD-003` 之后。

**补充状态（2026-06-06）：** `P3-WD-003` WebDAV 浏览与播放入口已完成基础闭环：`WebDavDirectoryParser` 解析 RFC 4918 `207 Multi-Status`，`WebDavConnectionClient.listDirectory(...)` 使用 `PROPFIND Depth: 1` 拉取目录；来源详情页对 `webdav` 来源显示“浏览文件”，进入 `WebDavBrowserFragment` 后可展示文件夹、可播放视频和普通文件，点击文件夹继续浏览。点击 WebDAV 视频时不把用户名/密码拼进 URL，而是从 `EncryptedSharedPreferences` 读取凭据，生成本次播放专用 `Authorization: Basic ...` header，经 `PlayerActivityIntents` / `PlayerViewModel` 传给 `PlayerManager` 的 Media3 HTTP data source；重试会复用当前 header，本地/队列切换会清空 header。`PlayerActivityIntentsSourceTest`、`PlayerRequestHeadersSourceTest`、`PlayerNetworkHeaderSourceTest`、`WebDavDirectoryParserTest` 与 `WebDavBrowserSourceTest` 通过；全量 `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` 与 `git diff --check` 通过。adb 已安装并确认 OpenVideo 进程启动且无 `AndroidRuntime:E` 崩溃输出，但当前设备焦点仍是系统 `NotificationShade`，WebDAV 浏览页点击验证待设备解锁后补跑。远程字幕匹配继续留给 `P3-WD-004`。

**补充状态（2026-06-06）：** `P3-WD-004` 远程同目录字幕匹配已完成基础切片：新增 `WebDavSubtitleMatcher`，在当前 WebDAV 目录中按同 basename 自动匹配 `.srt` / `.vtt`，支持 `movie.srt` 与 `movie.en.srt` 这类语言后缀并排除目录、无关文件和暂未纳入 MVP 的 `.ass`；`WebDavBrowserFragment` 点击视频时把匹配字幕 URL 放进 `PlayerActivityIntents` 的 `external_subtitle_uri`，播放器初始化优先使用用户手动字幕，其次使用 WebDAV 自动字幕，最后回退视频 URI；`SubtitleLoader.loadFromNetworkUrl(...)` 使用 OkHttp 加载网络字幕并复用当前播放 request headers，因此受认证保护的 WebDAV 字幕不会要求把凭据写进 URL。`WebDavSubtitleMatcherTest`、`WebDavBrowserSourceTest`、`PlayerActivityIntentsSourceTest`、`PlayerRequestHeadersSourceTest` 与 `SubtitleLoaderWiringTest` 聚焦测试通过；全量 `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` 与 `git diff --check` 通过。adb 已安装并确认 OpenVideo 进程启动且无 `AndroidRuntime:E` 崩溃输出，但当前设备焦点仍是系统 `NotificationShade`，WebDAV 字幕实际点击验证待设备解锁后补跑。缓存和离线清理继续留给 `P3-WD-005`。

**补充状态（2026-06-06）：** `P3-WD-005` WebDAV 缓存策略基础已完成：新增进程内 `WebDavMemoryCache`，目录列表缓存 60 秒，远程字幕解析结果缓存 5 分钟；缓存 key 使用 URL + request headers 的 SHA-256 摘要，不保存 Authorization、密码或其它 header 明文。`WebDavConnectionClient.listDirectory(...)` 成功后缓存目录 metadata，`SubtitleLoader.loadFromNetworkUrl(...)` 成功后缓存解析后的字幕条目；视频流不进入该缓存，也不默认下载。设置页既有“清除缓存”确认入口会同步调用 `webDavMemoryCache.clear()`，清掉 WebDAV 目录/字幕内存缓存。`WebDavMemoryCacheTest`、`WebDavCacheSourceTest` 与 `SubtitleLoaderWiringTest` 聚焦测试通过；全量 `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` 与 `git diff --check` 通过。adb 已安装并确认 OpenVideo 进程启动且无 `AndroidRuntime:E` 崩溃输出，但当前设备焦点仍是系统 `NotificationShade`，WebDAV 缓存相关 UI 点击验证待设备解锁后补跑。

**补充状态（2026-06-06）：** `P3-WD-006` WebDAV 错误处理基础已完成：`WebDavConnectionPolicy` 现在区分 401 认证失败、403 权限不足、404 文件/目录不存在、超时、TLS/证书错误、其它网络错误与其它 HTTP 状态；`WebDavConnectionClient` 的连接测试和目录浏览异常路径统一走 `classifyFailure(...)`，来源页添加 WebDAV 和 WebDAV 浏览页会显示对应文案。`WebDavConnectionPolicyTest` 与 `WebDavErrorHandlingSourceTest` 聚焦测试通过；全量 `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` 与 `git diff --check` 通过。adb 已安装并确认 OpenVideo 进程启动、当前焦点在 `MainActivity` 且无 `AndroidRuntime:E` 崩溃输出；由于当前没有真实 WebDAV 测试账号/服务端，401/403/404/超时/证书错误的真机端到端触发待后续样本补跑。本切片暂未新增“编辑凭据并重试”的专门 UI，用户仍需删除/重建来源或后续来源编辑入口补齐。

**补充状态（2026-06-06）：** `P3-SMB-001` SMB 依赖调研已完成：新增 `docs/roadmap/smb-nas-dlna-research.md`，记录 SMBJ、jcifs-ng/Codelibs jcifs、smb-kotlin 与 libsmb2 的许可、Android 兼容、体积/性能和流式播放风险。结论是本切片不把 SMB 依赖加入生产 APK，优先用 SMBJ 做隔离原型；jcifs-ng 因 LGPL/历史 API 风险暂不优先，smb-kotlin 因商业授权暂作备选，libsmb2 因 JNI/NDK 和 LGPL 合规成本延后。`SmbResearchDecisionSourceTest` 聚焦测试通过；全量 `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` 与 `git diff --check` 通过。adb 已安装并确认 OpenVideo 进程启动且无 `AndroidRuntime:E` 崩溃输出，但当前设备焦点是系统 `NotificationShade`；本切片未新增运行时代码路径或 SMB APK 依赖。

**补充状态（2026-06-06）：** `P3-SMB-002` NAS 字幕匹配可行性已完成：新增协议无关 `RemoteSidecarSubtitleMatcher`，固化远程来源同目录 `.srt` / `.vtt` 匹配规则、精确 basename 优先、语言后缀次之、目录/ASS/无关文件排除；`WebDavSubtitleMatcher` 已改为委托该通用 matcher，证明 WebDAV 与未来 SMB 可复用同一套规则而不重复开发。`docs/roadmap/smb-nas-dlna-research.md` 补充路径编码、权限模型、same directory only 和缓存 key 作用域要求。`RemoteSidecarSubtitleMatcherTest`、`NasSubtitleFeasibilitySourceTest` 与 `WebDavSubtitleMatcherTest` 聚焦测试通过；全量 `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` 与 `git diff --check` 通过。adb 已安装并确认 OpenVideo 进程启动且无 `AndroidRuntime:E` 崩溃输出，但当前设备焦点是系统 `NotificationShade`；本切片不新增 SMB 运行时代码或 APK 依赖。

**补充状态（2026-06-06）：** `P3-DLNA-001` DLNA/UPnP 调研已完成：`docs/roadmap/smb-nas-dlna-research.md` 新增 DLNA/UPnP 决策段，明确 `ContentDirectory` 浏览型 MediaServer 来源与 `MediaRenderer` 投屏控制是两条不同产品线；当前 Phase 3 不向生产 APK 添加 DLNA/UPnP runtime 依赖，也不把投屏混入 WebDAV/NAS 来源抽象。结论是 DLNA 延后到后续 Phase，如重开需拆成 browse-only MediaServer source 与 casting control point 两个独立切片；Jellyfin/Plex 后续优先评估原生 API，而不是把 DLNA 作为默认集成路径。

**补充状态（2026-06-06）：** `P3-MATRIX-001` URL/HLS/DASH/RTSP 支持矩阵已完成：新增 `docs/roadmap/network-protocol-support-matrix.md`，按当前代码能力记录 HTTP/HTTPS progressive、HLS、DASH、RTSP 的 URL 形态、Media3 module、buffering profile、直播/unknown duration、cleartext HTTP 和已知限制。该切片只补阶段结束文档，不新增播放协议或依赖；`NetworkProtocolSupportMatrixSourceTest` 固化文档内容与 `media3-exoplayer-hls` / `media3-exoplayer-dash` / `media3-exoplayer-rtsp`、`NETWORK_PROGRESSIVE` / `ADAPTIVE_STREAM` / `RTSP_STREAM` 的代码对应关系。

**补充状态（2026-06-06）：** `P3-WD-DOC-001` WebDAV 使用说明和已知限制已完成：新增 `docs/roadmap/webdav-usage-and-limits.md`，按当前实现记录添加 WebDAV 来源、`EncryptedSharedPreferences` 凭据存储、`PROPFIND Depth: 0/1`、目录浏览、播放时 `Authorization` header 注入、同目录 `.srt` / `.vtt` 字幕、`WebDavMemoryCache`、错误分类和已知限制。文档明确当前没有 edit credentials 屏幕、大目录分页/懒加载、离线缓存、写操作、递归扫描和真实 WebDAV 样本账号。

**补充状态（2026-06-06）：** `P3-MEDIA-SERVER-001` SMB/DLNA/Jellyfin/Plex 后续决策记录已完成：`docs/roadmap/smb-nas-dlna-research.md` 新增 media server follow-up decision，明确当前 Phase 3 不添加 Jellyfin/Plex runtime 依赖、不把 Jellyfin/Plex 默认走 DLNA；后续如进入新 Phase，应先做 Jellyfin native API 只读原型，再单独评估 Plex API 的 token、server discovery、direct play/transcoding 和隐私边界。

**补充状态（2026-06-06）：** `P3-REL-001` v0.4.0 Release Notes 草稿已完成：新增 `docs/roadmap/release-notes-v0.4.0-draft.md`，面向用户总结 URL/HLS/DASH/RTSP、来源页、WebDAV、隐私、安全、已知问题、未包含范围、回滚点和验证要求。该文档是 Phase 3 roadmap 草稿，不改变当前 `gradle.properties` 的实际版本号。

**补充状态（2026-06-06）：** `P3-URL-001` 打开 URL UI 已完成：Home header 新增“打开链接”图标入口，弹出 URL 输入对话框，提交时复用 `NetworkUrlPolicy` 校验 `http` / `https` / `rtsp`；当前切片只完成输入入口和校验反馈，不启动播放器，播放接入留给 `P3-URL-003`。

**补充状态（2026-06-06）：** `P3-URL-003` Media3 URL 播放接入已完成：Home 打开链接对话框校验通过后启动 `PlayerActivity`，传入规范化 URL、稳定 URL id、标题与 `video_path`；`LocalMediaUriPolicy` 补齐 `rtsp://` 网络 URI，避免 RTSP 被误当本地文件；adb HLS URL 回归发现并补齐 Media3 HLS/DASH/RTSP 可选模块，避免协议 factory 缺类崩溃。

**补充状态（2026-06-06）：** `P3-URL-004` 最近 URL 数据能力已完成：数据库升到 v10，新增 `network_recent_items` 最近网络播放表、`NetworkRecentItemDao`、`MIGRATION_9_10` 与 Hilt provider；Home 打开 URL 成功时记录最近项，Repository/ViewModel 提供读取与清空入口；展示地址由 `NetworkRecentUrlPolicy` 脱敏 query 中 `token` / `key` / `sign` / `auth` 类参数值。列表聚合 UI 留给 3.3.2。

**补充状态（2026-06-06）：** `P3-URL-005` 网络错误 HUD 联动已完成：播放器错误 HUD 接入 `NetworkErrorClassifier`，网络连接失败、DNS、超时、HTTP 状态、明文 HTTP 禁止和未知网络错误显示独立网络标题与具体描述；非网络本地文件/解码错误路径保持原 Phase 0 体验。

**补充状态（2026-06-06）：** `P3-URL-006` 分享 Intent 已完成：Manifest 为 `MainActivity` 增加 `SEND text/plain` 与 `VIEW http/https/rtsp` 入口；`NetworkSharedUrlPolicy` 从分享文本或浏览器 data URL 中提取并规范化首个可播放 URL，MainActivity 复用 `PlayerActivityIntents.networkPlayback(...)` 启动播放器并写入最近 URL。adb 已验证 SEND 标题+URL 文本与 VIEW data URL 均进入 `PlayerActivity`，示例 404 URL 走播放器错误路径且无 OpenVideo `FATAL EXCEPTION`。

---

## 1. 成功定义

1. 用户可以从应用内打开网络 URL，并使用同一套播放器手势、字幕、音轨、历史和错误诊断。
2. HLS/DASH/RTSP/普通 HTTP 视频地址具备基础播放、最近记录、失败重试和缓冲策略。
3. WebDAV 来源 MVP 能完成添加账号、浏览目录、播放视频、加载同目录字幕。
4. 网络凭据安全存储，不进入崩溃日志、诊断复制、设置导出或明文数据库。
5. 本地来源、URL 来源、WebDAV 来源在 UI 中有统一入口和清晰状态。

---

## 2. 非目标

| 不做 | 原因 |
|------|------|
| 不同时铺开 SMB/DLNA/Jellyfin/Plex | 协议复杂，先用 URL + WebDAV 打通来源抽象 |
| 不做在线播放平台聚合 | 避免版权和内容运营风险 |
| 不保存敏感 Header 到普通 Prefs | 凭据必须走安全存储或加密封装 |
| 不承诺所有直播源稳定 | 网络源差异大，先做清晰错误和重试 |
| 不做云同步账号 | 来源配置可导出但默认不含密码 |

---

## 3. 架构设计

### 3.1 来源抽象

```text
data/source/
  MediaSourceDescriptor.kt
  MediaSourceRepository.kt
  LocalMediaSource.kt
  UrlMediaSource.kt
  WebDavMediaSource.kt

core/network/
  NetworkPlaybackProbe.kt
  NetworkErrorClassifier.kt
  RequestHeaderPolicy.kt
  SecureCredentialStore.kt
```

### 3.2 数据模型

| 表/存储 | 字段重点 | 说明 |
|---------|----------|------|
| `media_sources` | id、type、name、baseUri、createdAt、lastUsedAt、enabled | 来源基础信息 |
| `network_recent_items` | sourceId、uri、title、duration、lastPlayedAt、identityId | 最近 URL/网络文件 |
| `source_credentials` | sourceId、credentialAlias | 只保存安全存储引用，不保存明文 |
| EncryptedSharedPreferences / Keystore | username/password/token | 凭据安全存储 |

### 3.3 错误分类

| 类型 | 用户文案方向 | 操作 |
|------|--------------|------|
| DNS/连接失败 | 无法连接到服务器 | 重试、检查地址 |
| 401/403 | 需要登录或权限不足 | 编辑凭据、重试 |
| 404 | 文件不存在 | 返回来源目录 |
| 超时 | 网络响应过慢 | 重试、调整缓冲 |
| 不支持协议 | 当前版本暂不支持此链接 | 复制链接、反馈 |
| 解码失败 | 视频编码暂不支持 | 切软解、复制诊断 |

---

## 4. Sprint 节奏

### Sprint 3.1：打开 URL MVP（4～6 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 3.1.1 | ✅ URL 输入入口 | Home header「打开链接」对话框，复用 `NetworkUrlPolicy` 校验 | 输入 http/https/rtsp 可提交并得到校验反馈 |
| 3.1.2 | ✅ URL 校验与规范化 | `NetworkUrlPolicy` 纯策略：scheme、空格/控制字符、host、协议、复制粘贴清理 | `NetworkUrlPolicyTest` 覆盖常见坏链接 |
| 3.1.3 | ✅ Media3 播放接入 | URL 进入 `PlayerActivity`，复用播放器；HLS/DASH/RTSP 模块已随 APK 打包 | HLS/MP4 示例可播 |
| 3.1.4 | ✅ 最近 URL | 记录标题/地址/时间，可清除；展示 URL 默认脱敏敏感 query | 不记录带敏感 token 的完整展示 |
| 3.1.5 | ✅ 错误 HUD 联动 | 网络错误走 Phase 0 错误体验，按连接/DNS/超时/HTTP/明文限制细分文案 | 404/超时/协议错误文案不同 |
| 3.1.6 | ✅ 分享 Intent | 从浏览器/文件管理器分享 URL 到 OpenVideo；分享文本和 VIEW data URL 均复用 URL 校验、播放器 Intent helper 与最近 URL 记录 | `NetworkSharedUrlPolicyTest` / `MainActivitySharedUrlSourceTest` / adb 分享入口 |

### Sprint 3.2：网络播放策略与缓冲（4～6 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 3.2.1 | ✅ 网络缓冲 profile | `PlayerBufferingPolicy` 覆盖 URL/HLS/DASH/RTSP，不同 buffer policy | `PlayerBufferingPolicyTest` 覆盖 scheme/type |
| 3.2.2 | ✅ Header/UA 配置 | 基础 User-Agent、Referer 可选；播放器 HTTP data source 统一走 `NetworkPlaybackHeaderPolicy` | `NetworkPlaybackHeaderPolicyTest` / `PlayerNetworkHeaderSourceTest`，敏感 header 诊断脱敏 |
| 3.2.3 | ✅ 失败重试策略 | 可重试网络错误自动 1s/2s/4s 退避，最多 3 次；耗尽后回到既有手动错误 HUD | `NetworkPlaybackRetryPolicyTest` / `PlayerNetworkRetrySourceTest`，弱网不无限请求 |
| 3.2.4 | ✅ 网络状态提示 | buffering、reconnecting、live/unknown duration 轻量状态标签；本地播放不显示网络状态 | `PlayerNetworkStatusPolicyTest` / `PlayerNetworkStatusSourceTest` |
| 3.2.5 | ✅ 直播/点播差异 | duration unknown 时隐藏不适合的 seek 行为；相对 seek 按钮在不可 seek duration 下短路 | `PlaybackTimelinePolicyTest` / `PlayerTimelineTest`；直播源总时长显示 `--:--` |

### Sprint 3.3：来源管理页（3～5 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 3.3.1 | ✅ 来源信息架构 | 本地、打开 URL、WebDAV、未来 SMB/Jellyfin 占位；Home/来源页共用打开 URL 对话框 | `SourcesInformationArchitectureSourceTest` / `NetworkOpenUrlDialogSourceTest`，导航清楚，不新增 WebDAV 连接逻辑 |
| 3.3.2 | ✅ 最近播放聚合 | 本地继续观看 + 网络最近播放按时间聚合，来源页用 `Local` / `URL` 标签和图标区分来源，并过滤网络播放历史避免重复显示 | `SourceRecentPlaybackPolicyTest` / `SourcesRecentPlaybackSourceTest`，缺失本地文件不直接播放，网络最近项复用播放器 Intent |
| 3.3.3 | ✅ 来源详情页 | 已保存 URL 来源可查看名称、脱敏地址、最后使用，可做 URL 规范校验并删除；删除确认复用 `SettingsConfirmationActionSheet` 底部样式 | `SourceDetailPresentationPolicyTest` / `SourceDetailSourceTest`，删除前统一底部确认 |
| 3.3.4 | ✅ 凭据安全提示 | 来源页/详情页展示凭据保存位置、导出排除和诊断脱敏策略，补充网络来源隐私说明 | `SourceCredentialPrivacyPolicyTest` / `SourceCredentialPrivacySourceTest`，设置导出敏感 marker 补强 |

### Sprint 3.4：WebDAV MVP（7～12 天）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 3.4.1 | ✅ 技术选型 | OkHttp 5.3.2 + WebDAV PROPFIND，后续 MockWebServer 回归；不引入完整 WebDAV 客户端库 | `docs/roadmap/webdav-dependency-selection.md` 记录依赖许可和维护风险 |
| 3.4.2 | ✅ 添加 WebDAV 来源 | baseUrl、用户名、密码、测试连接；成功后保存 `webdav` 来源元数据 | 凭据写入 AndroidX Security 加密存储，Room 不保存明文 |
| 3.4.3 | ✅ 目录浏览（基础） | `PROPFIND Depth: 1` 解析文件夹、可播放视频和普通文件；来源详情可进入 WebDAV 浏览页 | `WebDavDirectoryParserTest` / `WebDavBrowserSourceTest`；分页/懒加载仍待后续大目录优化 |
| 3.4.4 | ✅ 视频播放入口 | 点击远程视频进入播放器 URL 入口，不把凭据拼进 URL；认证播放通过本次播放 request header 注入 | `PlayerRequestHeadersSourceTest` / `PlayerNetworkHeaderSourceTest` / `WebDavBrowserSourceTest` |
| 3.4.5 | ✅ 同目录字幕发现 | 远程视频旁同名 `.srt` / `.vtt` 自动匹配，认证字幕加载复用本次播放 header | `WebDavSubtitleMatcherTest` / `SubtitleLoaderWiringTest` / WebDAV source tests |
| 3.4.6 | ✅ 缓存策略 | 字幕/目录元数据短缓存，视频不默认下载 | `WebDavMemoryCacheTest` / `WebDavCacheSourceTest`；设置页清缓存会清 WebDAV 内存缓存 |
| 3.4.7 | ✅ 错误处理（基础） | 认证失败、证书、超时、404 分别分类和展示 | `WebDavConnectionPolicyTest` / `WebDavErrorHandlingSourceTest`；编辑凭据重试入口待后续来源编辑 |

### Sprint 3.5：SMB/NAS 与 DLNA 调研原型（3～5 天，可选）

| 切片 | 任务 | 交付物 | 验收 |
|------|------|--------|------|
| 3.5.1 | ✅ SMB 依赖调研 | jcifs-ng/SMBJ/Android 兼容性；不向生产 APK 添加 SMB 依赖 | `docs/roadmap/smb-nas-dlna-research.md` / `SmbResearchDecisionSourceTest` |
| 3.5.2 | ✅ NAS 字幕匹配可行性 | 同目录字幕、权限、路径编码；WebDAV 字幕规则抽成远程来源通用 matcher | `RemoteSidecarSubtitleMatcherTest` / `NasSubtitleFeasibilitySourceTest` |
| 3.5.3 | ✅ DLNA/UPnP 调研 | 浏览 vs 投屏边界；`ContentDirectory` MediaServer 来源与 `MediaRenderer` 投屏控制拆开 | 决定延后到后续 Phase，不向生产 APK 添加 DLNA/UPnP runtime 依赖 |

---

## 5. 安全与隐私规则

1. URL 展示时默认隐藏 query 中疑似 `token`、`key`、`sign`、`auth` 字段。
2. 设置导出默认不包含网络凭据；如未来支持导出来源，只导出来源名称和地址。
3. 崩溃日志、复制诊断、Release 反馈不包含完整 URL、Header、用户名、密码。
4. WebDAV 密码使用 Android Keystore/EncryptedSharedPreferences 保存。
5. 删除来源时同步删除凭据引用和缓存。

---

## 6. 详细任务清单

| ID | 优先级 | 任务 | 依赖 | 验证 |
|----|--------|------|------|------|
| P3-URL-001 | P0 | ✅ 打开 URL UI | Phase 0 错误 HUD | `HomeOpenUrlSourceTest` / Home 聚焦测试 |
| P3-URL-002 | P0 | ✅ URL 校验规范化 | 无 | `NetworkUrlPolicyTest` |
| P3-URL-003 | P0 | ✅ Media3 URL 播放接入 | P3-URL-001 | `HomeOpenUrlSourceTest` / `LocalMediaUriPolicySourceTest` |
| P3-URL-004 | P0 | ✅ 最近 URL 数据能力 | P3-URL-003 | `NetworkRecentUrlPolicyTest` / `NetworkRecentItemSchemaSourceTest` / Home source tests |
| P3-URL-005 | P0 | ✅ 网络错误 HUD 联动 | P3-URL-003 / P3-NET-001 | `PlayerErrorPresentationPolicyTest` / adb HLS 错误 HUD |
| P3-URL-006 | P0 | ✅ 分享 Intent | P3-URL-003 / P3-URL-004 | `NetworkSharedUrlPolicyTest` / `MainActivitySharedUrlSourceTest` / adb SEND & VIEW |
| P3-NET-001 | P1 | ✅ NetworkErrorClassifier | Phase 0 错误模型 | `NetworkErrorClassifierTest` |
| P3-NET-002 | P1 | ✅ 网络缓冲策略 | PlayerBufferingPolicy | `PlayerBufferingPolicyTest` |
| P3-NET-003 | P1 | ✅ Header/UA 配置 | P3-NET-002 | `NetworkPlaybackHeaderPolicyTest` / `PlayerNetworkHeaderSourceTest` / adb HTTPS 入口与 cleartext 拦截 |
| P3-NET-004 | P1 | ✅ 失败重试策略 | P3-NET-001 / P3-NET-003 | `NetworkPlaybackRetryPolicyTest` / `PlayerNetworkRetrySourceTest` / adb 404 与 DNS 错误 |
| P3-NET-005 | P1 | ✅ 网络状态提示 | P3-NET-004 | `PlayerNetworkStatusPolicyTest` / `PlayerNetworkStatusSourceTest` / adb DNS 重试状态 |
| P3-NET-006 | P1 | ✅ 直播/点播差异 | P3-NET-005 | `PlaybackTimelinePolicyTest` / `PlayerTimelineTest` / adb live/VOD HLS 网络入口无崩溃 |
| P3-SRC-001 | P1 | ✅ 来源管理数据模型 | 无 | `MediaSourceSchemaSourceTest` / `data.local.*` |
| P3-SRC-002 | P1 | ✅ 来源信息架构 | P3-SRC-001 / P3-URL-003 | `SourcesInformationArchitectureSourceTest` / `NetworkOpenUrlDialogSourceTest` / adb 来源页导航和打开 URL 对话框 |
| P3-SRC-003 | P1 | ✅ 最近播放聚合 | P3-SRC-002 / P3-URL-004 | `SourceRecentPlaybackPolicyTest` / `SourcesRecentPlaybackSourceTest` / adb 来源页最近播放展示 |
| P3-SRC-004 | P1 | ✅ 来源详情页 | P3-SRC-001 / P3-SRC-003 | `SourceDetailPresentationPolicyTest` / `SourceDetailSourceTest` / adb 来源详情与统一底部删除确认 |
| P3-SRC-005 | P1 | ✅ 凭据安全提示 | P3-SRC-004 | `SourceCredentialPrivacyPolicyTest` / `SourceCredentialPrivacySourceTest` / adb 来源页与详情页隐私提示 |
| P3-WD-001 | P1 | ✅ WebDAV 依赖选型 | P3-SRC-001 | `docs/roadmap/webdav-dependency-selection.md` |
| P3-WD-002 | P1 | ✅ WebDAV 添加与测试连接 | P3-WD-001 | `WebDavConnectionPolicyTest` / `WebDavSourceSelectionSourceTest` / adb 安装启动无崩溃；对话框点击待设备解锁后补验 |
| P3-WD-003 | P1 | ✅ WebDAV 浏览与播放入口 | P3-WD-002 | `WebDavDirectoryParserTest` / `WebDavBrowserSourceTest` / `PlayerRequestHeadersSourceTest` / `PlayerNetworkHeaderSourceTest`；adb UI 点击待设备解锁 |
| P3-WD-004 | P1 | ✅ 远程字幕匹配 | P3-WD-003 | `WebDavSubtitleMatcherTest` / `SubtitleLoaderWiringTest` / `WebDavBrowserSourceTest` |
| P3-WD-005 | P1 | ✅ 缓存策略 | P3-WD-004 | `WebDavMemoryCacheTest` / `WebDavCacheSourceTest` / `SubtitleLoaderWiringTest` |
| P3-WD-006 | P1 | ✅ 错误处理（基础） | P3-WD-003 | `WebDavConnectionPolicyTest` / `WebDavErrorHandlingSourceTest`；编辑凭据重试入口待补 |
| P3-SMB-001 | P2 | ✅ SMB 调研原型 | WebDAV MVP | `docs/roadmap/smb-nas-dlna-research.md` / `SmbResearchDecisionSourceTest` |
| P3-SMB-002 | P2 | ✅ NAS 字幕匹配可行性 | P3-SMB-001 / P3-WD-004 | `RemoteSidecarSubtitleMatcherTest` / `NasSubtitleFeasibilitySourceTest` |
| P3-DLNA-001 | P2 | ✅ DLNA/UPnP 调研 | P3-SMB-001 | `docs/roadmap/smb-nas-dlna-research.md` / `DlnaUpnpResearchDecisionSourceTest` |
| P3-MATRIX-001 | P2 | ✅ URL/HLS/DASH/RTSP 支持矩阵 | P3-URL-003 / P3-NET-006 | `docs/roadmap/network-protocol-support-matrix.md` / `NetworkProtocolSupportMatrixSourceTest` |
| P3-WD-DOC-001 | P2 | ✅ WebDAV 使用说明和已知限制 | P3-WD-006 | `docs/roadmap/webdav-usage-and-limits.md` / `WebDavUsageGuideSourceTest` |
| P3-MEDIA-SERVER-001 | P2 | ✅ SMB/DLNA/Jellyfin/Plex 后续决策记录 | P3-SMB-001 / P3-DLNA-001 | `docs/roadmap/smb-nas-dlna-research.md` / `MediaServerFollowUpDecisionSourceTest` |
| P3-REL-001 | P2 | ✅ v0.4.0 Release Notes 草稿 | Phase 3 docs | `docs/roadmap/release-notes-v0.4.0-draft.md` / `Phase3ReleaseNotesSourceTest` |

---

## 7. 验收清单

- [ ] 普通 MP4 URL、HLS、DASH、RTSP 至少各有一个样本验证或明确记录不支持原因。（支持矩阵已记录样本策略；DASH/RTSP 真实端到端样本待可用地址补跑）
- [ ] 网络错误分类清晰，复制诊断脱敏。
- [ ] 最近 URL 可清理，敏感 query 不直接暴露。
- [ ] WebDAV 可添加、测试连接、浏览目录、播放视频、自动匹配字幕。
- [ ] WebDAV 密码不进入设置导出、日志、崩溃上报。
- [ ] 来源管理页可删除来源并清理凭据/缓存。

---

## 8. Phase 结束输出

1. ✅ v0.4.0 Release Notes 草稿：`docs/roadmap/release-notes-v0.4.0-draft.md`。
2. ✅ 网络来源隐私说明：`docs/roadmap/network-source-privacy.md`。
3. ✅ URL/HLS/DASH/RTSP 支持矩阵：`docs/roadmap/network-protocol-support-matrix.md`。
4. ✅ WebDAV 使用说明和已知限制：`docs/roadmap/webdav-usage-and-limits.md`。
5. ✅ SMB/DLNA/Jellyfin/Plex 后续决策记录：`docs/roadmap/smb-nas-dlna-research.md`。
