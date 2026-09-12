#!/bin/bash

# AI Minecraft Launcher APK 构建脚本
# 用于构建和签名APK文件

set -e

echo "🚀 开始构建 AI Minecraft Launcher APK..."

# 检查环境
check_environment() {
    echo "🔍 检查构建环境..."
    
    # 检查Java版本
    if ! command -v java &> /dev/null; then
        echo "❌ Java 未安装，请安装 Java 17+"
        exit 1
    fi
    
    java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        echo "❌ 需要 Java 17+，当前版本: $java_version"
        exit 1
    fi
    
    # 检查Android SDK
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        echo "❌ ANDROID_HOME 或 ANDROID_SDK_ROOT 未设置"
        echo "请设置Android SDK路径: export ANDROID_HOME=/path/to/android-sdk"
        exit 1
    fi
    
    echo "✅ 环境检查通过"
}

# 创建密钥目录
create_keystore() {
    echo "🔐 创建密钥目录..."
    mkdir -p keystore
    
    if [ ! -f "keystore/aimc-keystore.jks" ]; then
        echo "📝 生成签名密钥..."
        keytool -genkeypair \
          -v \
          -keystore keystore/aimc-keystore.jks \
          -alias aimc-key \
          -keyalg RSA \
          -keysize 2048 \
          -validity 10000 \
          -storepass aimc123456 \
          -keypass aimc123456 \
          -dname "CN=AI Minecraft Launcher, OU=Development, O=AIMC, L=Shanghai, ST=Shanghai, C=CN"
        echo "✅ 密钥生成完成"
    else
        echo "✅ 密钥文件已存在"
    fi
}

# 构建APK
build_apk() {
    echo "🔨 开始构建APK..."
    
    # 清理之前的构建
    ./gradlew clean
    
    # 构建Debug版本
    echo "📦 构建Debug版本..."
    ./gradlew :Launcher:assembleDebug
    
    # 构建Release版本
    echo "📦 构建Release版本..."
    ./gradlew :Launcher:assembleRelease
    
    echo "✅ APK构建完成"
}

# 签名APK
sign_apk() {
    echo "🔐 签名Release APK..."
    
    UNSIGNED_APK="Launcher/build/outputs/apk/release/Launcher-release-unsigned.apk"
    SIGNED_APK="Launcher/build/outputs/apk/release/Launcher-release-signed.apk"
    ALIGNED_APK="AI-Minecraft-Launcher-v1.0.0.apk"
    
    if [ ! -f "$UNSIGNED_APK" ]; then
        echo "❌ 未找到未签名的APK文件: $UNSIGNED_APK"
        exit 1
    fi
    
    # 签名APK
    jarsigner -verbose \
      -sigalg SHA256withRSA \
      -digestalg SHA-256 \
      -keystore keystore/aimc-keystore.jks \
      -storepass aimc123456 \
      -keypass aimc123456 \
      "$UNSIGNED_APK" \
      aimc-key
    
    # 对齐APK
    zipalign -v 4 "$UNSIGNED_APK" "$ALIGNED_APK"
    
    echo "✅ APK签名和对齐完成"
    echo "📱 最终APK文件: $ALIGNED_APK"
}

# 创建发布说明
create_release_notes() {
    echo "📝 创建发布说明..."
    
    cat > release_notes.md << EOF
# AI Minecraft Launcher v1.0.0 发布说明

## 🎉 新版本发布

这是AI Minecraft Launcher的第一个正式版本，包含完整的AI控制功能！

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

## 📱 用户界面

### 主界面
- 启动 Minecraft 按钮
- 启动/停止 AI 控制器按钮
- AI 状态显示
- 设置按钮

### 设置界面
- **LLM 配置**: API Key、模型选择、Base URL
- **AI Bridge**: 端口设置
- **任务描述**: 自定义AI任务
- **决策循环**: 间隔时间设置
- **高级功能**: 视觉模式、记忆系统开关
- **多智能体**: 协作模式开关

## 🔌 AI Bridge API

游戏内 HTTP API (默认端口 25580):

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/health` | GET | 健康检查 |
| `/api/state` | GET | 获取玩家状态 (位置/生命/饥饿/经验) |
| `/api/action` | POST | 执行动作 (移动/挖矿/放置/攻击等) |
| `/api/inventory` | GET | 获取背包内容 |
| `/api/blocks` | GET | 扫描附近方块和实体 |
| `/api/screenshot` | GET | 获取游戏截图 (PNG) |
| `/api/chat` | POST | 发送聊天消息 |
| `/api/recipe` | GET | 查询合成配方 |
| `/api/events` | GET | 获取游戏事件队列 |

## 🚀 使用说明

### 1. 安装APK
- 下载并安装 AI-Minecraft-Launcher-v1.0.0.apk
- 首次启动需要授予必要权限

### 2. 配置AI设置
- 打开设置界面
- 输入您的LLM API Key (OpenAI/Claude/DeepSeek)
- 选择模型和API Base URL
- 设置AI任务描述
- 根据需要启用视觉模式和记忆系统

### 3. 启动Minecraft
- 点击"启动Minecraft"按钮
- 等待游戏启动并登录账户

### 4. 启动AI控制器
- 点击"启动AI控制器"按钮
- AI将开始控制游戏角色
- 可以通过设置界面调整AI行为

## 📋 系统要求

- **Android**: 8.0+ (API 26+)
- **内存**: 至少2GB RAM
- **存储**: 至少500MB可用空间
- **网络**: 需要连接互联网用于AI API调用

## ⚠️ 注意事项

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

**版本**: 1.0.0  
**发布日期**: 2026-09-12  
**兼容性**: Android 8.0+  
**许可证**: GPL-3.0
EOF

    echo "✅ 发布说明已创建: release_notes.md"
}

# 主函数
main() {
    echo "🎮 AI Minecraft Launcher APK 构建脚本"
    echo "========================================"
    
    check_environment
    create_keystore
    build_apk
    sign_apk
    create_release_notes
    
    echo ""
    echo "🎉 构建完成！"
    echo "📱 最终APK文件: AI-Minecraft-Launcher-v1.0.0.apk"
    echo "📝 发布说明: release_notes.md"
    echo ""
    echo "🚀 下一步:"
    echo "1. 检查APK文件"
    echo "2. 测试APK功能"
    echo "3. 创建GitHub Release"
    echo "4. 上传APK文件"
}

# 运行主函数
main "$@"