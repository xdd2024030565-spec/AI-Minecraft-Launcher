# 📤 发布指南

本指南说明如何将AI Minecraft Launcher发布到GitHub Release。

## 🚀 发布流程

### 1. 准备工作

#### 环境要求
- Java 17+
- Android SDK Build-Tools 34.0.0+
- Android SDK API 34
- Gradle 8.7+
- Git
- GitHub CLI

#### 安装GitHub CLI
```bash
# macOS
brew install gh

# Ubuntu/Debian
sudo apt install gh

# Windows
winget install GitHub.cli
```

### 2. 构建APK

#### 自动构建
```bash
# 给脚本执行权限
chmod +x build_apk.sh

# 运行构建脚本
./build_apk.sh
```

#### 手动构建
```bash
# 清理项目
./gradlew clean

# 构建Debug版本
./gradlew :Launcher:assembleDebug

# 构建Release版本
./gradlew :Launcher:assembleRelease

# 签名APK
jarsigner -verbose \
  -sigalg SHA256withRSA \
  -digestalg SHA-256 \
  -keystore keystore/aimc-keystore.jks \
  -storepass aimc123456 \
  -keypass aimc123456 \
  Launcher/build/outputs/apk/release/Launcher-release-unsigned.apk \
  aimc-key

# 对齐APK
zipalign -v 4 \
  Launcher/build/outputs/apk/release/Launcher-release-unsigned.apk \
  Launcher/build/outputs/apk/release/Launcher-release-aligned.apk
```

### 3. 创建Git标签

#### 使用脚本创建标签
```bash
# 给脚本执行权限
chmod +x create_tag.sh

# 运行标签创建脚本
./create_tag.sh
```

#### 手动创建标签
```bash
# 检查当前状态
git status

# 添加所有更改
git add .

# 提交更改
git commit -m "Release v1.0.0: AI Minecraft Launcher First Stable Version"

# 创建标签
git tag -a v1.0.0 -m "AI Minecraft Launcher v1.0.0 Release"

# 推送标签到GitHub
git push origin v1.0.0
```

### 4. 发布到GitHub

#### 使用发布脚本
```bash
# 给脚本执行权限
chmod +x release.sh

# 运行发布脚本
./release.sh
```

#### 手动发布
```bash
# 创建GitHub Release
gh release create v1.0.0 \
  --title "AI Minecraft Launcher v1.0.0" \
  --notes-file release_notes.md \
  "AI-Minecraft-Launcher-v1.0.0.apk"
```

### 5. 验证发布

#### 检查Release
```bash
# 查看Release信息
gh release view v1.0.0

# 下载Release文件
gh release download v1.0.0
```

#### 访问Release页面
- 访问: https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher/releases
- 查看v1.0.0 Release

## 📋 发布检查清单

### 构建前检查
- [ ] 所有代码已提交
- [ ] 版本号已更新 (build.gradle.kts)
- [ ] README.md已更新
- [ ] 构建脚本已测试
- [ ] 签名密钥已准备

### 构建中检查
- [ ] Gradle构建成功
- [ ] APK签名成功
- [ ] APK对齐成功
- [ ] APK文件大小合理

### 发布前检查
- [ ] Git标签已创建
- [ ] 标签已推送到GitHub
- [ ] Release说明已准备
- [ ] APK文件已上传
- [ ] Release页面显示正确

## 🔧 故障排除

### 常见问题

#### 1. 签名失败
```bash
# 检查密钥文件
ls -la keystore/

# 重新生成密钥
keytool -genkeypair \
  -keystore keystore/aimc-keystore.jks \
  -alias aimc-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass aimc123456 \
  -keypass aimc123456 \
  -dname "CN=AI Minecraft Launcher, OU=Development, O=AIMC, L=Shanghai, ST=Shanghai, C=CN"
```

#### 2. GitHub CLI认证失败
```bash
# 登录GitHub
gh auth login

# 检查认证状态
gh auth status
```

#### 3. 构建失败
```bash
# 清理Gradle缓存
./gradlew cleanBuildCache

# 重新构建
./gradlew clean build
```

### 调试技巧

#### 查看详细构建信息
```bash
# 详细构建
./gradlew :Launcher:assembleDebug --info

# 查看APK信息
aapt dump badging AI-Minecraft-Launcher-v1.0.0.apk
```

#### 检查签名
```bash
# 验证APK签名
jarsigner -verify -verbose AI-Minecraft-Launcher-v1.0.0.apk

# 查看APK信息
keytool -printcert -jarfile AI-Minecraft-Launcher-v1.0.0.apk
```

## 📊 发布统计

### 版本历史
- **v1.0.0** (2026-09-12) - 首个稳定版本
  - 完整的AI控制功能
  - 多智能体协作系统
  - 视觉决策能力
  - 记忆系统
  - 用户友好的设置界面

### 文件大小
- **APK大小**: ~15-20MB (取决于优化程度)
- **最小安装**: ~50MB
- **推荐存储**: 100MB+

### 下载统计
可以通过GitHub Release页面查看下载统计：
- 访问: https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher/releases
- 查看下载次数和统计数据

## 🔄 版本管理

### 版本号规范
- **版本代码**: 递增整数 (1, 2, 3...)
- **版本名称**: 语义化版本 (v1.0.0, v1.1.0, v2.0.0)

### 更新流程
1. 更新版本号 (build.gradle.kts)
2. 更新更新日志 (v1.0.0, v1.1.0等)
3. 创建新标签
4. 发布新版本

---

**🎉 恭喜！您的应用已成功发布！**

**📞 支持**: 如遇到问题，请查看故障排除部分或提交Issue。