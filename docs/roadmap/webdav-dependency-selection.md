# P3-WD-001 WebDAV 依赖选型

## 决策

WebDAV MVP 使用 OkHttp 5.3.2 直接发起 RFC 4918 `PROPFIND` 请求，不引入专门 WebDAV 客户端库。

## 理由

- 项目已有网络播放、错误分类和来源管理基础，WebDAV 第一阶段只需要认证、`PROPFIND Depth: 0/1`、目录 XML 解析和文件 URL 交给播放器。
- OkHttp 是成熟 HTTP 客户端，许可证为 Apache-2.0，适合 Android；后续目录浏览测试可使用 MockWebServer。
- 不引入 Sardine 等完整 WebDAV 库，避免额外传递依赖、Android 兼容性和长期维护风险。

## 维护风险

- 需要自行维护 WebDAV XML 解析、路径编码、重定向、认证失败和服务器差异处理。
- `PROPFIND Depth: 1` 大目录可能响应很大，后续 `P3-WD-003` 必须做 IO 线程、超时、取消和 UI 分页/懒加载。
- 当前切片只实现连接测试和安全保存，目录浏览、播放和字幕匹配继续留给后续 WebDAV 切片。

## 验证策略

- 纯策略测试覆盖 WebDAV base URL 校验、凭据必填、`PROPFIND` 请求头和 HTTP 状态分类。
- 后续连接与目录浏览使用 MockWebServer 固定 207 Multi-Status 响应，真机 adb 只验证 UI 入口、输入校验和无崩溃。
