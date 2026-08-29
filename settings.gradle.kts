pluginManagement {
    repositories {
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

// 启动器主应用
include(":Launcher")

// AI Bridge Fabric Mod
include(":AiBridgeMod")

// AI 控制器库
include(":AiController")
