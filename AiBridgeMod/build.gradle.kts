plugins {
    // 注意：构建本模块需要 Gradle 9.5+ 与 JDK 21（fabric-loom 1.17.x 的要求）。
    // 该模块默认不参与构建，仅在传入 -PbuildFabricMod=true 时被包含（详见 settings.gradle.kts）
    id("fabric-loom") version "1.17.20"
}

dependencies {
    // Fabric API
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // Gson (Minecraft 自带, 但明确声明依赖)
    implementation("com.google.code.gson:gson:2.10.1")
}

// Fabric Mod 配置
processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version": project.version)
    }
}

// Mod 输出 jar 名称
base {
    archivesName = "ai-bridge"
}

// 版本
version = "1.0.0"
group = "com.aimc"
