# 📦 AI Minecraft Launcher 构建指南

## 🚀 构建APK

### 环境要求
- Java 17+
- Android SDK Build-Tools 34.0.0+
- Android SDK API 34
- Gradle 8.7+

### 构建步骤

#### 1. 生成签名密钥（如果还没有）

```bash
# 创建密钥目录
mkdir -p keystore

# 生成签名密钥
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
```

#### 2. 构建APK

```bash
# 构建Debug版本
./gradlew :Launcher:assembleDebug

# 构建Release版本
./gradlew :Launcher:assembleRelease

# 构建并安装到设备
./gradlew :Launcher:installDebug
```

#### 3. 签名Release版本

```bash
# 使用签名密钥签名APK
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

### 环境变量配置

在构建环境中设置以下环境变量：

```bash
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"
```

### 构建产物

- **Debug APK**: `Launcher/build/outputs/apk/debug/Launcher-debug.apk`
- **Release APK**: `Launcher/build/outputs/apk/release/Launcher-release.apk`
- **对齐后的Release APK**: `Launcher/build/outputs/apk/release/Launcher-release-aligned.apk`

### 版本管理

- 版本代码: `1`
- 版本名称: `1.0.0`
- 最小SDK: 26
- 目标SDK: 34

### 发布到GitHub

1. 创建Git标签:
```bash
git tag -a v1.0.0 -m "AI Minecraft Launcher 1.0.0 Release"
git push origin v1.0.0
```

2. 创建GitHub Release:
- 访问仓库的 "Releases" 页面
- 点击 "Create a new release"
- 标签选择 "v1.0.0"
- 标题: "AI Minecraft Launcher v1.0.0"
- 描述: 详细的发布说明
- 上传APK文件

### 故障排除

#### 签名错误
检查密钥文件是否存在，密码是否正确。

#### 构建失败
确保所有依赖项都已正确下载，Android SDK配置正确。

#### 资源未找到
确保所有XML布局文件和资源文件都存在且路径正确。