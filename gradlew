#!/usr/bin/env bash
#
# Gradle Wrapper 脚本
# 用法: ./gradlew [options]
#

# 设置 JVM 参数
DEFAULT_JVM_OPTS="-Xmx4096m -Dfile.encoding=UTF-8"

# 设置 Gradle 参数
GRADLE_OPTS="-Dorg.gradle.parallel=true -Dorg.gradle.caching=true"

# 路径
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="${APP_HOME%/*}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-${HOME}/.gradle}"
GRADLE_WRAPPER_DIR="${APP_HOME}/gradle/wrapper"
GRADLE_WRAPPER_JAR="${GRADLE_WRAPPER_DIR}/gradle-wrapper.jar"
GRADLE_WRAPPER_PROPS="${GRADLE_WRAPPER_DIR}/gradle-wrapper.properties"

# 检查 wrapper JAR
if [ ! -f "${GRADLE_WRAPPER_JAR}" ]; then
    echo "错误: gradle-wrapper.jar 不存在"
    echo "请运行: bash download_gradlew.sh"
    exit 1
fi

# 检查 Java
if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA="java"
fi

# 执行 Gradle Wrapper
exec "${JAVA}" \
    ${DEFAULT_JVM_OPTS} \
    ${GRADLE_OPTS} \
    -classpath "${GRADLE_WRAPPER_JAR}" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"