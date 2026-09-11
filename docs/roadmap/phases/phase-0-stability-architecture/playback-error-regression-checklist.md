# 播放失败 HUD 真机回归清单

**日期：** 2026-05-17  
**范围：** Phase 0.1 播放错误诊断闭环  
**适用分支：** `codex/phase0-stability-architecture`

---

## 1. 测试前准备

| 项目 | 要求 |
|------|------|
| 构建包 | 使用 `:app:assembleDebug` 生成的 Debug APK |
| 设备 | 至少 1 台 Android 13+ 手机；如可用，再补 1 台 Android 10～12 设备 |
| 权限 | 分别验证「已授权媒体权限」和「权限被撤销」两种状态 |
| 样本 | 正常 MP4、损坏视频、不支持/疑似不支持编码视频、被移动或删除的视频、网络错误预留样本 |
| 记录 | 每条记录设备型号、Android 版本、样本类型、实际结果、是否通过 |

---

## 2. 基础成功路径

| ID | 场景 | 步骤 | 期望 |
|----|------|------|------|
| EHUD-OK-001 | 正常视频播放 | 打开一个可播放 MP4 | 不显示错误 HUD；播放、暂停、seek 正常 |
| EHUD-OK-002 | 错误后恢复正常 | 触发错误 HUD 后返回列表，再打开正常 MP4 | 错误 HUD 不残留；正常视频可播放 |
| EHUD-OK-003 | 旋转后状态 | 错误 HUD 展示时旋转横/竖屏 | HUD 仍居中可读，按钮可点，不崩溃 |

---

## 3. 错误分类触发

| ID | 场景 | 触发方式 | 期望 |
|----|------|----------|------|
| EHUD-ERR-001 | 损坏文件 | 打开截断/伪造的视频文件 | 显示错误 HUD；文案不是空白；有重试/复制诊断/返回入口 |
| EHUD-ERR-002 | 不支持编码或解码失败 | 打开设备无法硬解的样本 | 显示「解码失败」类文案；出现「软件解码」入口 |
| EHUD-ERR-003 | 权限丢失 | 扫描后从系统设置撤销媒体权限，再尝试打开历史视频 | 显示「权限已失效」类文案；不出现软件解码入口 |
| EHUD-ERR-004 | 文件被移动/删除 | 扫描后移动或删除文件，再从历史/列表打开 | 显示「媒体来源不可用」或通用失败文案；不崩溃 |
| EHUD-ERR-005 | 网络预留错误 | 后续 URL 功能接入后，用断网或无效地址触发 | 显示「网络播放失败」或「播放超时」类文案 |

---

## 4. HUD 按钮行为

| ID | 按钮 | 步骤 | 期望 |
|----|------|------|------|
| EHUD-ACT-001 | 重试 | 错误 HUD 出现后连续快速点击「重试」3 次 | 第一次点击后按钮禁用或 HUD 消失；不重复触发多次恢复动作；不崩溃 |
| EHUD-ACT-002 | 软件解码 | 解码错误 HUD 出现后连续快速点击「软件解码」3 次 | 第一次点击后按钮禁用或 HUD 消失；仅触发一次重试；不崩溃 |
| EHUD-ACT-003 | 复制诊断 | 点击「复制诊断」 | Toast 显示诊断已复制；剪贴板有诊断文本；按钮可再次使用 |
| EHUD-ACT-004 | 返回列表 | 点击「返回列表」 | 退出播放页并释放播放器；返回列表后无黑屏 |
| EHUD-ACT-005 | READY 后清理 | 错误后点击重试并成功进入 READY | HUD 自动隐藏；播放控制恢复正常 |

---

## 5. 诊断文本脱敏检查

复制诊断后，检查剪贴板内容：

| ID | 字段 | 期望 |
|----|------|------|
| EHUD-DIAG-001 | `category` | 存在，值为 Network / Permission / Decoder / Source / Timeout / Unknown 之一 |
| EHUD-DIAG-002 | `errorCode` | 存在，保留 ExoPlayer 错误码名称 |
| EHUD-DIAG-003 | `causes` | 存在，包含异常类名链路，不包含完整本地路径 |
| EHUD-DIAG-004 | `positionMs` / `durationMs` | 存在，便于定位播放位置 |
| EHUD-DIAG-005 | `videoId` | 存在，可用于本地排查 |
| EHUD-DIAG-006 | `videoTitlePresent` | 只记录标题是否存在，不输出真实标题 |
| EHUD-DIAG-007 | `videoUri` / `videoPath` | 不包含 `/sdcard/`、`/storage/emulated/`、真实文件名、content provider 私密路径 |
| EHUD-DIAG-008 | URL token | `token`、`key`、`auth`、`signature` 等 query 值应显示为 `<redacted>` |
| EHUD-DIAG-009 | 设备字段 | `androidSdk`、`device` 存在，用于兼容性排查 |

---

## 6. 结果记录模板

| 日期 | 设备 | Android | 样本 | 用例 ID | 结果 | 备注 |
|------|------|---------|------|---------|------|------|
| 2026-05-17 |  |  |  |  | Pass / Fail |  |

---

## 7. 当前自动化覆盖

| 覆盖项 | 自动化测试 |
|--------|------------|
| 错误分类和动作 | `PlayerPlaybackErrorPolicyTest` |
| 诊断字段与脱敏 | `PlayerPlaybackErrorDiagnosticsPolicyTest` |
| Activity 接入和防连点源码约束 | `PlayerPlaybackErrorSourceTest` |
