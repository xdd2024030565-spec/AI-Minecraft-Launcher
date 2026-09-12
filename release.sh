#!/bin/bash

# AI Minecraft Launcher 发布脚本
# 用于构建APK并发布到GitHub Release

set -e

echo "🚀 开始发布 AI Minecraft Launcher v1.0.0..."

# 检查环境
check_environment() {
    echo "🔍 检查发布环境..."
    
    # 检查Git
    if ! command -v git &> /dev/null; then
        echo "❌ Git 未安装"
        exit 1
    fi
    
    # 检查GitHub CLI
    if ! command -v gh &> /dev/null; then
        echo "❌ GitHub CLI 未安装"
        echo "请安装 GitHub CLI: https://cli.github.com/"
        exit 1
    fi
    
    # 检查是否在正确的仓库
    if [ ! -d ".git" ] || [ ! -f "build.gradle.kts" ]; then
        echo "❌ 请在项目根目录运行此脚本"
        exit 1
    fi
    
    echo "✅ 环境检查通过"
}

# 构建APK
build_apk() {
    echo "🔨 构建APK..."
    
    # 清理之前的构建
    ./gradlew clean
    
    # 构建Debug版本
    echo "📦 构建Debug版本..."
    ./gradlew :Launcher:assembleDebug
    
    # 构建Release版本
    echo "📦 构建Release版本..."
    ./gradlew :Launcher:assembleRelease
    
    # 签名APK
    echo "🔐 签名APK..."
    
    UNSIGNED_APK="Launcher/build/outputs/apk/release/Launcher-release-unsigned.apk"
    SIGNED_APK="AI-Minecraft-Launcher-v1.0.0.apk"
    
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
    zipalign -v 4 "$UNSIGNED_APK" "$SIGNED_APK"
    
    # 验证APK
    if [ -f "$SIGNED_APK" ]; then
        echo "✅ APK构建和签名完成"
        echo "📱 APK文件: $SIGNED_APK"
        ls -lh "$SIGNED_APK"
    else
        echo "❌ APK签名失败"
        exit 1
    fi
}

# 创建Git标签
create_git_tag() {
    echo "🏷️ 创建Git标签..."
    
    # 检查标签是否已存在
    if git tag -l | grep -q "v1.0.0"; then
        echo "⚠️ 标签 v1.0.0 已存在，删除旧标签..."
        git tag -d v1.0.0
    fi
    
    # 创建新标签
    git tag -a v1.0.0 -m "AI Minecraft Launcher v1.0.0 Release"
    
    echo "✅ Git标签创建完成"
}

# 推送标签
push_tag() {
    echo "📤 推送标签到GitHub..."
    
    git push origin v1.0.0
    
    echo "✅ 标签推送完成"
}

# 创建GitHub Release
create_release() {
    echo "🎉 创建GitHub Release..."
    
    # 检查Release是否已存在
    if gh release list | grep -q "v1.0.0"; then
        echo "⚠️ Release v1.0.0 已存在，删除旧Release..."
        gh release delete v1.0.0 --yes
    fi
    
    # 创建Release
    gh release create v1.0.0 \
        --title "AI Minecraft Launcher v1.0.0" \
        --notes-file release_notes.md \
        "AI-Minecraft-Launcher-v1.0.0.apk"
    
    echo "✅ GitHub Release创建完成"
}

# 上传APK到Release
upload_apk() {
    echo "📤 上传APK到GitHub Release..."
    
    # 上传APK文件
    gh release upload v1.0.0 "AI-Minecraft-Launcher-v1.0.0.apk"
    
    echo "✅ APK文件上传完成"
}

# 验证Release
verify_release() {
    echo "🔍 验证Release..."
    
    # 检查Release是否创建成功
    if gh release view v1.0.0 > /dev/null; then
        echo "✅ Release验证成功"
        echo "🔗 Release链接: https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher/releases/tag/v1.0.0"
    else
        echo "❌ Release验证失败"
        exit 1
    fi
}

# 清理临时文件
cleanup() {
    echo "🧹 清理临时文件..."
    
    # 删除未签名的APK
    if [ -f "Launcher/build/outputs/apk/release/Launcher-release-unsigned.apk" ]; then
        rm "Launcher/build/outputs/apk/release/Launcher-release-unsigned.apk"
    fi
    
    echo "✅ 清理完成"
}

# 主函数
main() {
    echo "🎮 AI Minecraft Launcher 发布脚本"
    echo "=================================="
    
    check_environment
    build_apk
    create_git_tag
    push_tag
    create_release
    upload_apk
    verify_release
    cleanup
    
    echo ""
    echo "🎉 发布完成！"
    echo "📱 APK文件: AI-Minecraft-Launcher-v1.0.0.apk"
    echo "🔗 Release链接: https://github.com/xdd2024030565-spec/AI-Minecraft-Launcher/releases/tag/v1.0.0"
    echo ""
    echo "🚀 用户现在可以下载并安装您的应用了！"
}

# 运行主函数
main "$@"