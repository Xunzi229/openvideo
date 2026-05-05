# OpenVideo 设置页对齐执行计划（修正版）

**目标：** 严格按四张设计图对齐主列表、竖屏播放、横屏播放和设置页，并修正文档中不一致描述。

---

## Task 1：重构设置页布局（按图）

**文件：**
- `app/src/main/res/layout/dialog_player_settings.xml`

**动作：**
- 重建为左导航 + 右内容结构
- 左侧含：播放器设置标题、6 分类、恢复默认设置
- 右侧默认展示“播放”分类，两组卡片：
  - 播放控制
  - 播放选项

## Task 2：重构设置页逻辑（按图）

**文件：**
- `app/src/main/java/com/example/openvideo/ui/player/PlayerSettingsDialog.kt`

**动作：**
- 分类切换：点击左侧导航切换右侧 section
- 播放分类项实现：
  - 速度
  - 循环模式
  - 跳过片头片尾
  - 快进/快退间隔
  - 记住播放进度
  - 硬件加速
  - 退出自动暂停
  - 自动连播下一集
  - 背景音
- 全部状态写入 `SharedPreferences`
- 恢复默认设置可回滚到默认值

## Task 3：文案资源化

**文件：**
- `app/src/main/res/values/strings.xml`

**动作：**
- 把设置页新增文案全部改成资源字符串
- 不保留新增硬编码文案

## Task 4：修正文档（仅保留设计图支持范围）

**文件：**
- `docs/design-system.md`
- `docs/superpowers/specs/2026-05-05-openvideo-dark-design-system.md`
- `docs/superpowers/plans/2026-05-05-openvideo-dark-design-system.md`

**动作：**
- 删除“全 App 已完整定义”的表述
- 明确当前四张图对应的页面范围和优先级
- 其他页面标注为“待设计图补充”

## Task 6：播放页像素微调（新增）

**文件：**
- `app/src/main/res/layout/activity_player.xml`
- `app/src/main/res/layout/player_controls.xml`
- `app/src/main/res/layout-land/activity_player.xml`

**动作：**
- 按 `design/竖屏播放界面.png` 微调竖屏播放层级、控件间距和按钮尺寸
- 按 `design/横屏播放界面.png` 微调横屏控件布局和信息密度
- 不改动已有播放核心逻辑

## Task 5：验证

**动作：**
- XML 解析通过
- `ReadLints` 无新增错误
- 在可用 Gradle 环境执行 `:app:assembleDebug`

> 提交时遵循仓库规则：使用 `git-helper`，不直接执行 `git add/git commit/git push`。
