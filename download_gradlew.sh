#!/bin/bash
# 自动下载 Gradle Wrapper JAR 文件
# 运行此脚本: bash download_gradlew.sh

set -e

WRAPPER_DIR="gradle/wrapper"
WRAPPER_JAR="${WRAPPER_DIR}/gradle-wrapper.jar"
WRAPPER_PROPS="${WRAPPER_DIR}/gradle-wrapper.properties"

if [ ! -f "${WRAPPER_PROPS}" ]; then
    echo "错误: gradle-wrapper.properties 不存在"
    echo "请确保 gradle/wrapper/gradle-wrapper.properties 文件存在"
    exit 1
fi

echo "=========================================="
echo "  Gradle Wrapper 自动下载脚本"
echo "=========================================="
echo ""

# 从 gradle-wrapper.properties 获取 Gradle 版本（例如 8.5）
GRADLE_VERSION=$(grep "distributionUrl" "${WRAPPER_PROPS}" | sed 's/.*gradle-\([^/]*\)-.*/\1/')
if [ -z "${GRADLE_VERSION}" ]; then
    echo "错误: 无法从 gradle-wrapper.properties 解析 Gradle 版本"
    exit 1
fi

echo "检测到 Gradle 版本: ${GRADLE_VERSION}"
echo ""

# 创建目录
mkdir -p "${WRAPPER_DIR}"

# 检查是否已存在
if [ -f "${WRAPPER_JAR}" ] && [ -s "${WRAPPER_JAR}" ]; then
    echo "✓ gradle-wrapper.jar 已存在，跳过下载"
    exit 0
fi

# Gradle 官方仓库的 tag 有两种形式：v8.5.0（补丁版，常见）与 v8.5（部分无补丁版本）
# 依次尝试，避免因 tag 名称不一致导致 404
download_wrapper_jar() {
    local url="$1"
    echo "尝试下载: ${url}"
    if command -v curl &> /dev/null; then
        curl -L -f -s -S -o "${WRAPPER_JAR}" "${url}" && return 0
    elif command -v wget &> /dev/null; then
        wget -q -O "${WRAPPER_JAR}" "${url}" && return 0
    else
        echo "错误: 未找到 curl 或 wget"
        return 1
    fi
    return 1
}

DOWNLOAD_OK=0
for TAG in "v${GRADLE_VERSION}.0" "v${GRADLE_VERSION}"; do
    URL="https://raw.githubusercontent.com/gradle/gradle/${TAG}/gradle/wrapper/gradle-wrapper.jar"
    if download_wrapper_jar "${URL}"; then
        DOWNLOAD_OK=1
        break
    fi
done

if [ ${DOWNLOAD_OK} -ne 1 ]; then
    echo ""
    echo "=========================================="
    echo "  所有下载方案均失败"
    echo "=========================================="
    echo ""
    echo "请手动下载 gradle-wrapper.jar 并保存到: ${WRAPPER_JAR}"
    echo "或者，如果你已安装 Gradle，请运行:"
    echo "  gradle wrapper --gradle-version ${GRADLE_VERSION}"
    echo ""
    exit 1
fi

if [ ! -s "${WRAPPER_JAR}" ]; then
    echo "错误: 下载的 gradle-wrapper.jar 为空或不存在"
    exit 1
fi

if ! unzip -t "${WRAPPER_JAR}" > /dev/null 2>&1; then
    echo "错误: 文件不是有效的 JAR 文件"
    exit 1
fi

echo "✓ gradle-wrapper.jar 下载成功"
echo "  大小: $(du -h "${WRAPPER_JAR}" | cut -f1)"
echo ""

if [ -f "gradlew" ]; then
    [ -x "gradlew" ] || chmod +x gradlew
    echo "✓ gradlew 已就绪"
fi

echo ""
echo "完成! 现在可以运行: ./gradlew tasks"
