@rem
@echo off
setlocal

set DEFAULT_JVM_OPTS=-Xmx4096m -Dfile.encoding=UTF-8
set GRADLE_OPTS=-Dorg.gradle.parallel=true -Dorg.gradle.caching=true

set APP_HOME=%~dp0..
set GRADLE_USER_HOME=%GRADLE_USER_HOME%%HOMEDRIVE%%HOMEPATH%.gradle
set GRADLE_WRAPPER_DIR=%APP_HOME%\gradle\wrapper
set GRADLE_WRAPPER_JAR=%GRADLE_WRAPPER_DIR%\gradle-wrapper.jar

if not exist "%GRADLE_WRAPPER_JAR%" (
    echo 错误: gradle-wrapper.jar 不存在
    echo 请运行: download_gradlew.bat
    exit /b 1
)

if "%JAVA_HOME%" == "" (
    set JAVA=java
) else (
    set JAVA=%JAVA_HOME%\bin\java
)

%JAVA% %DEFAULT_JVM_OPTS% %GRADLE_OPTS% -classpath "%GRADLE_WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal