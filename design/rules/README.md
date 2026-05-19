# OpenVideo 设计规则（Design Rules）

本目录存放**不可随意改动的行为契约**，供在改代码前对照。
每条规则对应一类用户可见行为；修改实现前必须先读规则，改完后跑规则中列出的测试。

| 规则 | 说明 |
|------|------|
| [player-video-orientation.md](./player-video-orientation.md) | 播放器进入时的横/竖屏判定与切换（禁止「先横后竖」闪屏） |
| [settings-backup-deferred.md](./settings-backup-deferred.md) | 设置备份：底层已实现，UI 暂隐藏，后续 webdev Web 备份 |

Cursor 侧镜像：`.cursor/rules/*.mdc`（带 `globs`，编辑相关文件时自动加载）。
