# OpenVideo 设置页对齐 Spec（修正版）

## 背景

之前文档把范围扩展到“全 App 完整设计定义”，与现有设计输入不一致。  
当前设计输入为四张图：

- `design/全量界面.png`
- `design/竖屏播放界面.png`
- `design/横屏播放界面.png`
- `design/播放的设置页面.png`

本 spec 修正为：**按四张图分别对齐主列表、竖屏播放、横屏播放和设置页**，并把未定义区域标注为“待设计图补充”。

## 目标

1. 主列表页、竖屏播放页、横屏播放页、设置页结构与参考图一致。
2. 把设置项状态保存到本地，保证再次打开时状态可恢复。
3. 删除与参考图不一致的过度扩展配置。

## 非目标

- 不在本 spec 中定义首页、播放器控制层、底部导航的像素级视觉细节。
- 不在没有设计图依据时定义其他分类的完整业务规则。

## 强约束

### 设置页布局结构

- 左导航：播放器设置 + 6 个分类 + 恢复默认设置
- 右内容：仅显示当前分类内容，采用分组卡片 + 行设置

### 播放分类内容

- 播放速度：0.5x/0.75x/1.0x/1.25x/1.5x/2.0x
- 循环播放：关闭/单集循环/列表循环
- 跳过片头片尾：开关
- 快进快退间隔：5s/10s/15s
- 记住播放进度：开关
- 播放时启用硬件加速：开关
- 退出时自动暂停：开关
- 自动连播下一集：开关
- 背景音（后台播放）：开关

### 状态持久化

上述播放分类项全部写入 `SharedPreferences`。

## 代码范围

- `app/src/main/res/layout/dialog_player_settings.xml`
- `app/src/main/java/com/example/openvideo/ui/player/PlayerSettingsDialog.kt`
- `app/src/main/res/values/strings.xml`
- `docs/design-system.md`
- `docs/superpowers/specs/2026-05-05-openvideo-dark-design-system.md`
- `docs/superpowers/plans/2026-05-05-openvideo-dark-design-system.md`

## 验收标准

- 设置页视觉结构与参考图一致（左导航 + 右分组）
- 播放分类项齐全且可操作
- 关键项状态可持久化
- 文档不再声明超出设计图范围的已定义规则
