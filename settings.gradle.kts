pluginManagement {
    repositories {
        // fabric-loom 的插件标记只发布在 Fabric 官方仓库，必须在此声明
        maven("https://maven.fabricmc.net") {
            name = "Fabric"
        }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.fabricmc.net") {
            name = "Fabric"
        }
    }
}

rootProject.name = "AI Minecraft Launcher"

// 启动器主应用（APK 由它产出）
include(":Launcher")

// AI 控制器库
include(":AiController")

// AI Bridge Fabric Mod
//
// 说明：fabric-loom 1.17.x 要求 Gradle 9.5+ 与 JDK 21，
// 而 APK 构建链（AGP 8.2 / Gradle 8.5 / JDK 17）与之不兼容。
// 为避免 Mod 模块拖垮 APK 构建，默认不包含该模块。
// 需要单独构建 Mod 时（请使用 Gradle 9.5+ / JDK 21 环境）：
//   ./gradlew -PbuildFabricMod=true :AiBridgeMod:build
if (providers.gradleProperty("buildFabricMod").orNull == "true") {
    include(":AiBridgeMod")
}
