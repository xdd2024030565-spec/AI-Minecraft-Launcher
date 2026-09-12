# 🎮 AI Minecraft Launcher

> 一个基于 [FCL (Fold Craft Launcher)](https://github.com/FCL-Team/FoldCraftLauncher) 的 Android Minecraft: Java Edition 启动器，内置 **AI 接入功能**，让 AI 可以玩 Minecraft。

## ✨ 特性

### 启动器功能 (基于 FCL)
- 📦 下载和安装 Minecraft Java 版
- 🔐 微软账户登录认证 (离线模式可用)
- 🎮 支持 Fabric Mod 加载器
- 🖥️ LWJGL Android 适配 (触摸操控)
- ☕ 内置 ARM64 OpenJDK 运行时

### AI 接入功能
- 🤖 **AI Bridge Mod** — Fabric Mod，在游戏内运行 HTTP API 服务器
- 🌐 **HTTP API** — 暴露游戏状态查询和动作执行接口
- 🧠 **AI Controller** — 连接 LLM (GPT-4 / Claude / DeepSeek)，实现 AI 决策循环
- 📸 **视觉能力** — 截图 + 多模态 LLM
- 🔄 **事件流** — 游戏事件实时推送
- 🔧 **配置管理** — 通过 SharedPreferences 管理模型/密钥/任务

## 📁 项目结构

```
AI-Minecraft-Launcher/
├── Launcher/                    # 启动器主应用 (Android, Kotlin/Java)
│   └── src/main/java/
│       ├── com/aimc/launcher/
│       │   ├── MainActivity.kt        # 启动器主界面
│       │   ├── AiConfig.kt            # AI 配置管理
│       │   ├── service/
│       │   │   └── AiControllerService.kt  # AI 控制器前台服务
│       │   └── mod/
│       │       └── ModInjector.kt    # Mod 自动注入
│       └── com/tungsten/fcl/
│           ├── FCLApplication.java     # FCL Application
│           ├── FCLRepository.java      # 文件管理
│           ├── activity/MainActivity.java  # FCL 游戏启动界面
│           ├── auth/AccountManager.java     # 账户管理
│           ├── download/GameDownloader.java # 游戏下载器
│           ├── game/
│           │   ├── GameVersion.java    # 版本信息
│           │   └── VersionManager.java # 版本管理
│           └── launch/GameLauncher.java # 游戏启动器
├── AiBridgeMod/                # Fabric Mod (Java)
│   └── src/main/java/com/aimc/ai_bridge/
│       ├── AiBridgeMod.java        # Mod 入口
│       ├── AiBridgeServer.java     # HTTP 服务器
│       ├── ActionExecutor.java     # 动作执行 (12种)
│       ├── GameStateCollector.java # 状态收集
│       ├── BlockScanner.java        # 方块扫描
│       ├── InventoryInspector.java  # 背包检查
│       ├── ScreenshotCapture.java   # 截图捕获
│       ├── GameEventCollector.java  # 事件收集
│       └── RecipeLookup.java        # 配方查询
├── AiController/               # AI 控制器 (Android 库)
│   └── src/main/java/com/aimc/controller/
│       ├── DecisionEngine.java  # 决策循环
│       ├── GameApiClient.java   # 游戏 API 客户端
│       └── LlmClient.java       # LLM 客户端
├── build.gradle.kts            # 根构建文件
└── settings.gradle.kts         # 模块配置
```

## 🔌 AI Bridge API

游戏内 HTTP API (默认端口 25580):

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/health` | GET | 健康检查 |
| `/api/state` | GET | 获取玩家状态 (位置/生命/饥饿/经验) |
| `/api/action` | POST | 执行动作 (移动/挖矿/放置/攻击等) |
| `/api/inventory` | GET | 获取背包内容 |
| `/api/blocks` | GET | 扫描附近方块和实体 (支持 radius 参数) |
| `/api/screenshot` | GET | 获取游戏截图 (PNG) |
| `/api/chat` | POST | 发送聊天消息 |
| `/api/recipe` | GET | 查询合成配方 (支持 item 参数搜索) |
| `/api/events` | GET | 获取游戏事件队列 |

### 动作类型

```json
{"action": "move", "direction": "forward", "duration": 10}
{"action": "look", "yaw": 90.0, "pitch": 0.0}
{"action": "jump"}
{"action": "mine", "duration": 20}
{"action": "place"}
{"action": "attack"}
{"action": "inventory_click", "slot": 0, "button": "left"}
{"action": "craft", "item": "stone_pickaxe"}
{"action": "drop_item", "slot": 0}
{"action": "toggle_sprint"}
{"action": "chat", "message": "Hello!"}
```

## 🏗️ 开发路线图

- [x] **阶段 0**: 项目初始化 — Gradle 多模块、仓库结构
- [x] **阶段 1**: 基础启动器 — FCL 核心框架 (Activity/Repository/版本管理/下载器/启动器/认证)
- [x] **阶段 2**: AI Bridge Mod 开发 — HTTP API、动作执行、状态收集、方块扫描、背包检查、截图、事件收集、配方查询
- [x] **阶段 3**: 启动器 + Mod 集成 — Mod 自动注入器、AI 控制器服务、配置管理
- [x] **阶段 4**: AI 控制器 (LLM 决策循环) — DecisionEngine、GameApiClient、LlmClient、前台服务
- [ ] **阶段 5**: 高级功能 — 视觉决策、记忆系统、多 AI 协作

## 📄 许可证

GPL-3.0 (与 FCL 保持一致)

## 🙏 致谢

- [FCL-Team/FoldCraftLauncher](https://github.com/FCL-Team/FoldCraftLauncher) — Fold Craft Launcher
- [PrismarineJS/mineflayer](https://github.com/PrismarineJS/mineflayer) — Minecraft bot framework
- [FCL-Team/EnchantNet](https://github.com/FCL-Team/EnchantNet) — Remote connection tool
