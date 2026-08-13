# 🎵 Lyricraft

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.02.00-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![ExoPlayer](https://img.shields.io/badge/ExoPlayer-2.19.1-34A853.svg?logo=android)](https://exoplayer.dev/)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?logo=android)](https://developer.android.com/studio/releases/platforms)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

[**English**](README.md) | **中文**

---

## 📖 项目概述

**Lyricraft** 是一款基于 **Android Jetpack Compose + ExoPlayer** 构建的纯原生安卓轻量歌词制作工具。用户可导入纯文本歌词、加载本地音频，通过手动打点匹配时间轴，最终导出标准 **`.lrc`** 格式歌词文件。

本项目为**纯离线、本地优先**应用 — 无需联网、无需注册，所有数据均在本地处理，无隐私泄露风险。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| 📝 歌词导入 | 粘贴多行纯文本，自动按换行分割为歌词列表 |
| 🎵 音频加载 | 读取本地音频文件，支持播放/暂停/进度拖拽 |
| ⏱️ 时间打点 | 播放时一键记录当前进度，自动切换至下一行 |
| ✏️ 实时编辑 | 支持修改每行歌词文本、删除单行 |
| 💾 导出 LRC | 自定义保存路径与文件名，生成标准 LRC 格式 |
| 📂 文件浏览器 | 内置简易文件夹选择器，支持层级跳转 |
| 🔐 权限适配 | 适配 Android 11+ 全局文件访问权限 |
| 🌓 深色模式 | 跟随系统自动切换 Material 3 深浅配色 |
| 📱 底部导航 | 文本导入 / 时间打点 / 设置 三页面切换 |

## 🛠️ 技术栈

| 技术 | 说明 |
|------|------|
| **Kotlin** | 100% Kotlin 编写 |
| **Jetpack Compose** | 声明式 UI，Material 3 设计规范 |
| **ExoPlayer** | Google 官方音频播放引擎 |
| **Activity Result API** | 现代化文件选择回调 |
| **Kotlin Coroutines** | 协程处理 IO 与 UI 刷新 |

## 🏗️ 架构设计

- **单 Activity + 纯 Compose**：无 Fragment，无传统 XML 布局
- **状态驱动 UI**：所有状态由 `remember` 托管，数据变更自动刷新视图
- **轻量简洁**：无 MVVM 过度设计，代码清晰易维护

## 📂 项目结构

```
app/src/main/java/com/storm/lyricraft/
├── MainActivity.kt                 # 应用入口 & 全局状态管理
├── data/
│   └── LyricLine.kt                # 歌词数据模型
├── ui/
│   ├── theme/
│   │   ├── Color.kt               # 配色定义
│   │   ├── Type.kt                # 字体排版
│   │   └── Theme.kt               # Material 3 主题配置
│   ├── navigation/
│   │   └── TabPage.kt             # 底部导航枚举
│   ├── components/
│   │   ├── LyricItem.kt           # 单行歌词组件
│   │   └── Dialogs.kt             # 导出/文件夹/权限弹窗
│   └── screens/
│       ├── InputTextScreen.kt     # 文本导入页
│       ├── RecordTimeScreen.kt    # 时间打点页（核心）
│       └── SettingScreen.kt       # 设置页
└── utils/
    └── TimeUtils.kt               # LRC 时间格式化工具
```

## 🚀 快速开始

### 📋 环境要求

| 工具 | 版本 |
|------|------|
| Android Studio | Ladybug \| 2024.2.1 或更高 |
| Gradle | 8.2+ |
| Min SDK | API 21 (Android 5.0) |
| Target SDK | API 34 (Android 14) |

### 🔨 克隆与构建

```bash
# 克隆仓库
git clone https://github.com/BlazeDu/Lyricraft.git

# 使用 Android Studio 打开项目，等待 Gradle 同步完成

# 运行应用
./gradlew installDebug
```

## 📱 使用指南

1. **导入歌词** — 切换到 **「文本」** 页，粘贴完整纯文本歌词（每行一句），点击 **「导入歌词并进入打点界面」**
2. **加载音频** — 进入 **「时间」** 页，点击 **「选择音频」**，从本地选取音频文件
3. **时间打点** — 点击 **「播放」** 开始播放，在每句歌词对应时刻点击 **「记录当前歌曲时间」**，应用自动记录并切换至下一行
4. **导出 LRC** — 点击 **「导出 LRC」**，选择保存文件夹与文件名，确认后生成标准 LRC 文件

## ⏱️ 时间格式

时间戳采用标准 LRC 格式：`[分钟:秒.厘秒]`，例如：

```
[00:12.34]第一句歌词
[00:25.67]第二句歌词
```

## 🔐 权限适配

- **Android 10 及以下**：使用传统文件读写权限
- **Android 11+ (API 30+)**：适配 `MANAGE_EXTERNAL_STORAGE` 全局文件访问权限
- 应用内提供一键跳转系统设置页面，引导用户授权

## 🧹 资源管理

- `DisposableEffect` 监听生命周期，页面销毁时自动释放 ExoPlayer 实例
- 协程自动终止，无内存泄漏风险

## 🧩 后续迭代方向

- [ ] 支持导入已有 LRC 文件并编辑
- [ ] 批量微调时间 / 整体偏移
- [ ] 主题切换与字体大小自定义
- [ ] 歌词预览与播放同步高亮
- [ ] 撤销 / 重做功能
- [ ] 批量删除歌词

## 🤝 贡献指南

欢迎提交 Issue 或 Pull Request！

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

## 📄 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose) — 现代声明式 UI 工具包
- [ExoPlayer](https://exoplayer.dev/) — 强大的 Android 音视频播放引擎
- [Material 3](https://m3.material.io/) — Google 最新设计语言

---

**用心制作 ❤️ by BlazeDu**