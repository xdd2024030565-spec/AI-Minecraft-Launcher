# 🎮 AI Minecraft Launcher

> 一个基于 [FCL (Fold Craft Launcher)](https://github.com/FCL-Team/FoldCraftLauncher) 的 Android Minecraft: Java Edition 启动器，内置 **AI 接入功能**，让 AI 可以玩 Minecraft。

## ✨ 特性

### 启动器功能 (基于 FCL)
- 📦 下载和安装 Minecraft Java 版
- 🔐 微软账户登录认证
- 🎮 支持 Forge / Fabric / NeoForge 等 Mod 加载器
- 🖥️ LWJGL Android 适配 (触摸操控)
- ☕ 内置 ARM64 OpenJDK 运行时

### AI 接入功能 (新增)
- 🤖 **AI Bridge Mod** — Fabric Mod，在游戏内运行 HTTP API 服务器
- 🌐 **HTTP API** — 暴露游戏状态查询和动作执行接口
- 🧠 **AI Controller** — 连接 LLM (GPT-4 / Claude / DeepSeek)，实现 AI 决策循环
- 📸 **视觉能力** — 截图 + 多模态 LLM
- 🔄 **RikkaHub MCP 集成** — 可通过 MCP 协议控制

## 📁 项目结构

```
AI-Minecraft-Launcher/
├── Launcher/                    # 启动器主应用 (Android, Kotlin/Java)
│   └── src/main/java/
│       ├── com/aimc/launcher/      # 启动器 UI 和逻辑
│       ├── com/aimc/launcher/ai/    # AI 控制器服务
│       └── com/aimc/launcher/mod/  # Mod 自动注入
├── AiBridgeMod/                # Fabric Mod (Java)
│   └── src/main/java/
│       └── com/aimc/ai_bridge/     # HTTP API + 游戏控制
│           ├── AiBridgeServer.java     # HTTP 服务器
│           ├── GameStateCollector.java # 游戏状态收集
│           ├── ActionExecutor.java     # 动作执行器
│           ├── InventoryInspector.java  # 背包检查
│           └── BlockScanner.java        # 方块扫描
├── AiController/               # AI 控制器 (Android 库)
│   └── src/main/java/
│       └── com/aimc/controller/    # LLM 客户端 + 决策循环
├── build.gradle.kts            # 根构建文件
└── settings.gradle.kts         # 模块配置
```

## 🔌 AI Bridge API

游戏内 HTTP API (默认端口 25580):

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/state` | GET | 获取玩家状态 (位置/生命/饥饿/经验) |
| `/api/action` | POST | 执行动作 (移动/挖矿/放置/攻击等) |
| `/api/inventory` | GET | 获取背包内容 |
| `/api/blocks` | GET | 扫描附近方块和实体 |
| `/api/screenshot` | GET | 获取游戏截图 (PNG) |
| `/api/chat` | POST | 发送聊天消息 |
| `/api/recipe` | GET | 查询合成配方 |
| `/api/events` | WS | WebSocket 实时事件流 |

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

- [x] **阶段 0**: 项目初始化
- [ ] **阶段 1**: 基础启动器 (FCL Fork)
- [ ] **阶段 2**: AI Bridge Mod 开发
- [ ] **阶段 3**: 启动器 + Mod 集成
- [ ] **阶段 4**: AI 控制器 (LLM 决策循环)
- [ ] **阶段 5**: 高级功能 (视觉/记忆/多 AI)

## 📄 许可证

GPL-3.0 (与 FCL 保持一致)

## 🙏 致谢

- [FCL-Team/FoldCraftLauncher](https://github.com/FCL-Team/FoldCraftLauncher) — Fold Craft Launcher
- [PrismarineJS/mineflayer](https://github.com/PrismarineJS/mineflayer) — Minecraft bot framework
- [FCL-Team/EnchantNet](https://github.com/FCL-Team/EnchantNet) — Remote connection tool
