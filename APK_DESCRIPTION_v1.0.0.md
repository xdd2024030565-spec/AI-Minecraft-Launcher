# AI Minecraft Launcher v1.0.0 APK 文件信息

## 📱 APK 基本信息

- **文件名**: AI-Minecraft-Launcher-v1.0.0.apk
- **版本**: 1.0.0
- **版本代码**: 1
- **应用ID**: com.aimc.launcher
- **文件大小**: 15.2 MB
- **构建时间**: 2026-09-12T15:50:00Z

## 🔐 签名信息

- **签名算法**: SHA256withRSA
- **密钥大小**: 2048位
- **别名**: aimc-key
- **有效期**: 2026-09-12 至 2056-09-12
- **证书**: CN=AI Minecraft Launcher, OU=Development, O=AIMC, L=Shanghai, ST=Shanghai, C=CN

## 📋 应用信息

### 应用名称
AI Minecraft Launcher

### 包名
com.aimc.launcher

### 目标平台
Android 8.0+ (API 26+)

### 支持架构
arm64-v8a

### 编译配置
- **编译SDK**: 34
- **目标SDK**: 34
- **最低SDK**: 26
- **Java版本**: 17
- **构建工具**: Gradle 8.2.0

## ✨ 核心功能

### 🎮 启动器功能
- 📦 下载和安装 Minecraft Java 版
- 🔐 微软账户登录认证 (离线模式可用)
- 🎮 支持 Fabric Mod 加载器
- 🖥️ LWJGL Android 适配 (触摸操控)
- ☕ 内置 ARM64 OpenJDK 运行时

### 🤖 AI 接入功能
- 🤖 **AI Bridge Mod** — Fabric Mod，在游戏内运行 HTTP API 服务器
- 🌐 **HTTP API** — 暴露游戏状态查询和动作执行接口
- 🧠 **AI Controller** — 连接 LLM (GPT-4 / Claude / DeepSeek)，实现 AI 决策循环
- 📸 **视觉能力** — 截图 + 多模态 LLM (GPT-4o Vision)
- 🔄 **事件流** — 游戏事件实时推送
- 🧩 **记忆系统** — AI 记住过去的行为和观察，实现长期规划
- 🔧 **配置管理** — 设置界面管理模型/密钥/任务/视觉/记忆

### 🎯 多智能体协作
- **5个预设智能体**: 探索者、收集者、建造者、矿工、农民
- **轮询调度**: 智能体轮流控制角色完成不同子任务
- **单玩家协作机制**: 智能体轮流控制角色完成不同子任务

## 🔌 API 接口

### HTTP API 端点 (默认端口 25580)

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/health` | GET | 健康检查 |
| `/api/state` | GET | 获取玩家状态 |
| `/api/action` | POST | 执行动作 |
| `/api/inventory` | GET | 获取背包内容 |
| `/api/blocks` | GET | 扫描附近方块 |
| `/api/screenshot` | GET | 获取游戏截图 |
| `/api/chat` | POST | 发送聊天消息 |
| `/api/recipe` | GET | 查询合成配方 |
| `/api/events` | GET | 获取游戏事件 |

## 📱 权限要求

- **网络**: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE
- **设备唤醒**: WAKE_LOCK
- **前台服务**: FOREGROUND_SERVICE, POST_NOTIFICATIONS
- **存储**: READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
- **安装**: REQUEST_INSTALL_PACKAGES

## 🗂️ 项目模块

### Launcher
- **类型**: Android Application
- **描述**: 启动器主应用
- **语言**: Kotlin/Java
- **功能**: 用户界面、AI配置管理、游戏启动

### AiBridgeMod
- **类型**: Fabric Mod
- **描述**: AI Bridge Fabric Mod
- **语言**: Java
- **功能**: 游戏内HTTP API、动作执行、状态收集

### AiController
- **类型**: Android Library
- **描述**: AI 控制器库
- **语言**: Kotlin/Java
- **功能**: AI决策循环、LLM客户端、记忆系统

## 📦 依赖库

### Android 核心库
- androidx.appcompat:appcompat:1.7.0
- com.google.android.material:material:1.11.0
- androidx.constraintlayout:constraintlayout:2.1.4
- androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
- org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3
- androidx.core:core-ktx:1.12.0

### 网络和工具库
- com.squareup.okhttp3:okhttp:4.12.0
- com.google.code.gson:gson:2.11.0

## 🚀 构建信息

### 构建脚本
- **构建脚本**: build_apk.sh
- **发布脚本**: release.sh
- **Gradle版本**: 8.2.0
- **Java版本**: 17

### 构建步骤
1. 环境检查 (Java 17+, Android SDK, Gradle)
2. 创建签名密钥
3. 清理项目
4. 构建Debug版本
5. 构建Release版本
6. APK签名
7. APK对齐
8. 创建发布说明

## 📋 系统要求

- **Android**: 8.0+ (API 26+)
- **内存**: 至少2GB RAM
- **存储**: 至少500MB可用空间
- **网络**: 需要连接互联网用于AI API调用

## ⚠️ 重要提醒

1. **API费用**: 使用AI会产生API调用费用，请合理使用
2. **隐私**: AI会分析游戏截图和状态，请确保符合当地隐私法规
3. **性能**: 视觉模式需要更多计算资源，建议在性能较好的设备上使用
4. **测试**: 这是第一个正式版本，建议在非重要游戏中测试

## 🆘 支持

如遇到问题，请:
1. 检查设置配置是否正确
2. 确保网络连接正常
3. 查看日志输出
4. 提交Issue报告

---

**🎉 感谢使用AI Minecraft Launcher！**

**📧 联系**: xdd2024030565@gmail.com
**🔗 项目**: https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher
**📄 许可证**: GPL-3.0