# AI Minecraft Launcher v1.0.0 发布说明

## 🎉 新版本发布

这是AI Minecraft Launcher的第一个正式版本，包含完整的AI控制功能！

## 📱 下载安装

### 直接下载APK
📥 **[AI-Minecraft-Launcher-v1.0.0.apk](https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher/releases/download/v1.0.0/AI-Minecraft-Launcher-v1.0.0.apk)**

### 安装步骤
1. 下载APK文件
2. 在Android设备上安装
3. 首次启动需要授予必要权限
4. 配置AI设置
5. 启动Minecraft并开始AI控制

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

## 🚀 使用说明

### 1. 配置AI设置
打开应用 → 点击"设置"按钮
- **API Key**: 输入您的LLM API Key (OpenAI/Claude/DeepSeek)
- **模型**: 选择模型 (gpt-4o-mini/claude-3-5-sonnet/deepseek-chat)
- **Base URL**: API基础URL (默认: https://api.openai.com/v1/)
- **任务描述**: 自定义AI任务目标
- **视觉模式**: 启用截图+多模态LLM分析
- **记忆系统**: 启用AI记忆功能
- **多智能体**: 启用多智能体协作模式

### 2. 启动Minecraft
- 点击"启动Minecraft"按钮
- 等待游戏启动并登录微软账户

### 3. 启动AI控制器
- 点击"启动AI控制器"按钮
- AI将开始控制游戏角色

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
4. 提交Issue报告: [GitHub Issues](https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher/issues)

## 📝 更新日志

### v1.0.0 (2026-09-12)
- ✅ 完整的AI Minecraft启动器功能
- ✅ 多智能体协作系统
- ✅ 视觉决策能力
- ✅ 记忆系统
- ✅ 用户友好的设置界面
- ✅ 完整的HTTP API
- ✅ APK签名和发布

---

**🎉 感谢使用AI Minecraft Launcher！**

**📧 联系**: xdd2024030565@gmail.com  
**🔗 项目**: https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher  
**📄 许可证**: GPL-3.0