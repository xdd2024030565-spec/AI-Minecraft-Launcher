#!/bin/bash
# 自动下载 Gradle Wrapper JAR 文件
# 运行此脚本: bash download_gradlew.sh

set -e

WRAPPER_DIR="gradle/wrapper"
WRAPPER_JAR="${WRAPPER_DIR}/gradle-wrapper.jar"
WRAPPER_PROPS="${WRAPPER_DIR}/gradle-wrapper.properties"

echo "=========================================="
echo "  Gradle Wrapper 自动下载脚本"
echo "=========================================="

# 创建目录
mkdir -p "${WRAPPER_DIR}"

# 检查是否已存在
if [ -f "${WRAPPER_JAR}" ]; then
    echo "✓ gradle-wrapper.jar 已存在，跳过下载"
    exit 0
fi

# 方法 1: 从 Gradle 官方下载
echo "正在从 Gradle 官方下载 gradle-wrapper.jar..."
WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar"

if command -v curl &> /dev/null; then
    curl -L -o "${WRAPPER_JAR}" "${WRAPPER_URL}"
elif command -v wget &> /dev/null; then
    wget -O "${WRAPPER_JAR}" "${WRAPPER_URL}"
else
    echo "错误: 未找到 curl 或 wget"
    exit 1
fi

# 验证文件
if [ ! -f "${WRAPPER_JAR}" ] || [ ! -s "${WRAPPER_JAR}" ]; then
    echo "错误: 下载的 gradle-wrapper.jar 为空或不存在"
    exit 1
fi

# 验证 JAR 文件格式
if ! unzip -t "${WRAPPER_JAR}" > /dev/null 2>&1; then
    echo "错误: 文件不是有效的 JAR 文件"
    exit 1
fi

echo "✓ gradle-wrapper.jar 下载成功"
echo "  大小: $(du -h "${WRAPPER_JAR}" | cut -f1)"

# 方法 2: 如果上述方法失败，使用 gradle 命令生成
if ! command -v gradle &> /dev/null; then
    echo ""
    echo "=========================================="
    echo "  备选方案: 使用 gradle wrapper 命令"
    echo "=========================================="
    echo ""
    echo "如果你已安装 Gradle，请运行:"
    echo "  cd /path/to/AI-Minecraft-Launcher"
    echo "  gradle wrapper --gradle-version 8.5"
    echo ""
    echo "然后提交 gradle/wrapper/ 下的文件到仓库"
fi

echo ""
echo "完成! 现在可以运行: ./gradlew tasks"