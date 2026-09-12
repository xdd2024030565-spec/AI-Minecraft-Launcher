#!/bin/bash
# 生成 Gradle Wrapper 文件
# 运行此脚本: bash generate_gradle_wrapper.sh

set -e

echo "=========================================="
echo "  Gradle Wrapper 生成脚本"
echo "=========================================="
echo ""

# 从 gradle-wrapper.properties 获取 Gradle 版本
WRAPPER_DIR="gradle/wrapper"
WRAPPER_PROPS="${WRAPPER_DIR}/gradle-wrapper.properties"

if [ ! -f "${WRAPPER_PROPS}" ]; then
    echo "错误: gradle-wrapper.properties 不存在"
    echo "请确保 gradle/wrapper/gradle-wrapper.properties 文件存在"
    exit 1
fi

# 获取 Gradle 版本
GRADLE_VERSION=$(grep "distributionUrl" "${WRAPPER_PROPS}" | sed 's/.*gradle-\([^/]*\)-.*/\1/')
echo "检测到 Gradle 版本: ${GRADLE_VERSION}"
echo ""

# 下载 Gradle 发行包
TMP=$(mktemp -d)
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

echo "正在下载 Gradle 发行包..."
echo "URL: ${GRADLE_URL}"

curl -fL --retry 3 -o "${TMP}/gradle.zip" "${GRADLE_URL}"
echo "✓ 下载完成"
echo ""

# 解压 Gradle 发行包
echo "正在解压 Gradle 发行包..."
unzip -q "${TMP}/gradle.zip" -d "${TMP}"
echo "✓ 解压完成"
echo ""

# 生成 Gradle Wrapper
echo "正在生成 Gradle Wrapper..."
"${TMP}/gradle-${GRADLE_VERSION}/bin/gradle" wrapper --gradle-version "${GRADLE_VERSION}"
echo "✓ Gradle Wrapper 生成完成"
echo ""

# 验证生成的文件
if [ -f "gradlew" ] && [ -f "${WRAPPER_DIR}/gradle-wrapper.jar" ] && [ -f "${WRAPPER_DIR}/gradle-wrapper.properties" ]; then
    echo "✓ 所有必要文件已生成:"
    echo "  - gradlew"
    echo "  - ${WRAPPER_DIR}/gradle-wrapper.jar"
    echo "  - ${WRAPPER_DIR}/gradle-wrapper.properties"
    echo ""
    echo "✓ 设置执行权限..."
    chmod +x gradlew
    echo "✓ 权限设置完成"
else
    echo "错误: Gradle Wrapper 生成失败"
    echo "请检查生成的文件"
    exit 1
fi

# 清理临时文件
rm -rf "${TMP}"
echo "✓ 临时文件已清理"
echo ""
echo "=========================================="
echo "  完成生成！"
echo "=========================================="
echo ""
echo "现在可以运行以下命令提交到 Git:"
echo ""
echo "git add gradlew gradlew.bat gradle/wrapper/"
echo "git commit -m "chore: add gradle wrapper"
echo "git push"
