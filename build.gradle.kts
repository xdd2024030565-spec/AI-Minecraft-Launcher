// Top-level build file
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    // fabric-loom 已移至 AiBridgeMod/build.gradle.kts 内声明：
    // 它要求 Gradle 9.5+ / JDK 21，且默认不参与 APK 构建（见 settings.gradle.kts）
}
