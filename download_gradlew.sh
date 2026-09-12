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

# 从 gradle-wrapper.properties 获取 Gradle 版本
GRADLE_VERSION=$(grep "distributionUrl" "${WRAPPER_PROPS}" | sed 's/.*gradle-\([^/]*\)-.*/\1/')
WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"

echo "检测到 Gradle 版本: ${GRADLE_VERSION}"
echo "下载 URL: ${WRAPPER_URL}"
echo ""

# 创建目录
mkdir -p "${WRAPPER_DIR}"

# 检查是否已存在
if [ -f "${WRAPPER_JAR}" ]; then
    echo "✓ gradle-wrapper.jar 已存在，验证文件..."
    if [ -s "${WRAPPER_JAR}" ]; then
        echo "✓ gradle-wrapper.jar 文件大小正常"
        echo "✓ 跳过下载"
        exit 0
    else
        echo "⚠ gradle-wrapper.jar 文件为空，重新下载..."
        rm -f "${WRAPPER_JAR}"
    fi
fi

echo "正在下载 gradle-wrapper.jar..."

# 尝试下载
if command -v curl &> /dev/null; then
    curl -L -f -s -S -o "${WRAPPER_JAR}" "${WRAPPER_URL}"
    DOWNLOAD_SUCCESS=$?
elif command -v wget &> /dev/null; then
    wget -q -O "${WRAPPER_JAR}" "${WRAPPER_URL}"
    DOWNLOAD_SUCCESS=$?
else
    echo "错误: 未找到 curl 或 wget"
    echo "请安装 curl 或 wget"
    exit 1
fi

if [ $DOWNLOAD_SUCCESS -ne 0 ]; then
    echo ""
    echo "=========================================="
    echo "  下载失败，尝试备用方案"
    echo "=========================================="
    echo ""
    
    # 备用URL
    BACKUP_URL="https://github.com/gradle/gradle/raw/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"
    echo "尝试备用URL: ${BACKUP_URL}"
    
    if command -v curl &> /dev/null; then
        curl -L -f -s -S -o "${WRAPPER_JAR}" "${BACKUP_URL}"
        DOWNLOAD_SUCCESS=$?
    elif command -v wget &> /dev/null; then
        wget -q -O "${WRAPPER_JAR}" "${BACKUP_URL}"
        DOWNLOAD_SUCCESS=$?
    fi
    
    if [ $DOWNLOAD_SUCCESS -ne 0 ]; then
        echo ""
        echo "=========================================="
        echo "  所有下载方案均失败"
        echo "=========================================="
        echo ""
        echo "请手动下载 gradle-wrapper.jar:"
        echo "1. 访问: ${WRAPPER_URL}"
        echo "2. 保存文件到: ${WRAPPER_JAR}"
        echo "3. 运行: chmod +x gradlew"
        echo ""
        echo "或者，如果你已安装 Gradle，请运行:"
        echo "  gradle wrapper --gradle-version ${GRADLE_VERSION}"
        echo ""
        exit 1
    fi
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
echo "  路径: ${WRAPPER_JAR}"
echo ""
echo "✓ 现在可以运行: ./gradlew tasks"
echo ""
# 验证 gradlew 脚本
if [ -f "gradlew" ]; then
    echo "✓ gradlew 脚本存在"
    if [ -x "gradlew" ]; then
        echo "✓ gradlew 脚本可执行"
    else
        echo "⚠ gradlew 脚本不可执行，正在设置权限..."
        chmod +x gradlew
    fi
else
    echo "⚠ gradlew 脚本不存在"
    echo "请确保 gradlew 文件存在"
fi

echo ""
echo "完成!"
