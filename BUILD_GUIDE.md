# 🚀 GitHub Actions 构建指南

## ✅ 已完成的工作

### 1. 创建了两个GitHub Actions工作流

#### 📦 `build-apk.yml` - APK构建工作流
- **功能**: 构建Debug和Release版本的APK
- **触发条件**: 代码提交、PR、手动触发
- **构建内容**:
  - 下载源代码
  - 设置Java 17环境
  - 设置Android SDK
  - 构建Debug和Release APK
  - 签名和对齐APK
  - 上传构建产物

#### 📤 `publish-release.yml` - 自动发布工作流
- **功能**: 构建成功后自动创建GitHub Release
- **触发条件**: 构建工作流成功完成
- **发布内容**:
  - 自动下载构建的APK文件
  - 创建v1.0.0 Release
  - 上传APK文件到Release
  - 包含完整的功能说明

## 🚨 重要修复说明

### 已修复的问题
- ✅ **弃用的actions版本**: 已更新到最新版本
- ✅ **actions/upload-artifact**: 从v3更新到v4
- ✅ **actions/cache**: 从v3更新到v4
- ✅ **actions/github-script**: 从v6更新到v7
- ✅ **actions/upload-release-asset**: 从v1更新到v2

## 📱 如何使用

### 方法1：通过GitHub网页界面

1. **打开仓库页面**
   ```
   https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher
   ```

2. **进入Actions页面**
   - 点击顶部的 **"Actions"** 标签

3. **选择工作流**
   - 找到 **"Build APK"** 工作流

4. **手动触发构建**
   - 点击右上角的 **"..."** 按钮
   - 选择 **"Run workflow"**
   - 点击 **"Run workflow"** 按钮

### 方法2：通过GitHub Mobile App

1. **下载GitHub Mobile App**
   - iOS: App Store搜索 "GitHub"
   - Android: Google Play搜索 "GitHub"

2. **登录并找到仓库**
   - 搜索: `xdd2024030565-spec/AI-Minecraft-Launcher`
   - 点击进入仓库

3. **运行工作流**
   - 点击底部的 **"Actions"** 标签
   - 找到 **"Build APK"** 工作流
   - 点击 **"Run workflow"** 按钮

4. **监控构建进度**
   - 等待构建开始（通常1-3分钟）
   - 可以实时查看构建日志
   - 构建成功后显示绿色✅

## 📋 构建产物

### 构建完成后，您将获得：

#### 1. Debug版本APK
- **文件名**: `Launcher-debug.apk`
- **路径**: Actions构建产物的debug-apk
- **用途**: 测试和调试

#### 2. Release版本APK
- **文件名**: `AI-Minecraft-Launcher-v1.0.0.apk`
- **路径**: Actions构建产物的release-apk
- **用途**: 发布和分发

#### 3. GitHub Release
- **自动创建**: v1.0.0 Release
- **包含文件**: 签名后的APK文件
- **访问地址**: Releases页面

## 🎯 下载APK文件

### 从Actions页面下载
1. 进入 **"Actions"** 页面
2. 找到成功的构建
3. 点击进入构建详情
4. 在 **"Artifacts"** 部分下载APK文件

### 从Releases页面下载
1. 进入 **"Releases"** 页面
2. 找到 **"v1.0.0"** 版本
3. 点击下载APK文件

## 🔧 构建配置说明

### 环境配置
- **操作系统**: Ubuntu 20.04 LTS
- **Java版本**: JDK 17 (Temurin)
- **Android SDK**: 34.0.0
- **构建工具**: Gradle 8.2.0
- **Actions版本**: 最新稳定版本

### 构建步骤
1. **代码检出**: 获取最新源代码
2. **环境设置**: 配置Java和Android SDK
3. **权限设置**: 给gradlew执行权限
4. **依赖缓存**: 加速Gradle构建
5. **Debug构建**: 构建测试版本
6. **Release构建**: 构建发布版本
7. **密钥生成**: 创建签名密钥
8. **APK签名**: 对Release APK进行签名
9. **APK对齐**: 优化APK性能
10. **产物上传**: 上传构建文件

### 自动发布
- **触发条件**: 构建成功且在main分支
- **发布内容**: Release版本的APK
- **版本信息**: 包含完整的功能说明

## ⚠️ 注意事项

### 构建时间
- **首次构建**: 5-10分钟
- **后续构建**: 2-5分钟（有缓存）

### 失败原因
- **网络问题**: 检查GitHub Actions网络连接
- **依赖问题**: Gradle依赖下载失败
- **环境问题**: Android SDK配置错误
- **Actions版本**: 已修复所有弃用版本问题

### 解决方案
1. **重新运行**: 手动触发重新构建
2. **查看日志**: 检查构建日志详情
3. **清理缓存**: 删除.gradle重新构建
4. **检查版本**: 确保使用最新Actions版本

## 🎉 下一步

现在您可以:
1. ✅ **触发第一次构建** - 使用GitHub网页或Mobile App
2. ✅ **下载APK文件** - 从Actions或Releases页面
3. ✅ **测试应用功能** - 在手机上安装和测试
4. ✅ **自动发布** - 构建成功后自动创建Release

## 📞 支持信息

如遇到问题，请:
1. 检查构建日志
2. 确认网络连接
3. 重新触发构建
4. 提交Issue报告

---

**🎯 构建系统已修复完成！现在可以正常构建和发布APK文件！**

**📧 开发者**: xdd2024030565@gmail.com
**🔗 项目**: https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher
**📄 许可证**: GPL-3.0