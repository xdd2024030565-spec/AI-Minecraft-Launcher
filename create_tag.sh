#!/bin/bash

# 创建Git标签脚本
# 用于创建v1.0.0标签

echo "🏷️ 创建Git标签 v1.0.0..."

# 检查是否在正确的仓库
if [ ! -d ".git" ] || [ ! -f "build.gradle.kts" ]; then
    echo "❌ 请在项目根目录运行此脚本"
    exit 1
fi

# 检查标签是否已存在
if git tag -l | grep -q "v1.0.0"; then
    echo "⚠️ 标签 v1.0.0 已存在"
    echo "是否删除旧标签并重新创建? (y/N)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        git tag -d v1.0.0
        echo "✅ 旧标签已删除"
    else
        echo "❌ 取消创建标签"
        exit 0
    fi
fi

# 创建新标签
git tag -a v1.0.0 -m "AI Minecraft Launcher v1.0.0 Release"

echo "✅ Git标签创建完成"
echo "📝 标签信息:"
git tag -v v1.0.0

echo ""
echo "📤 推送标签到GitHub:"
echo "   git push origin v1.0.0"
echo ""
echo "🚀 下一步:"
echo "   1. 推送标签: git push origin v1.0.0"
echo "   2. 运行发布脚本: ./release.sh"
echo "   3. 创建GitHub Release"