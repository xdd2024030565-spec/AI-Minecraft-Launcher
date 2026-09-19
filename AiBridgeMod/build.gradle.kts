plugins {
    // 构建要求: JDK 21 + Gradle 9.7.1+ (fabric-loom 1.17.x)
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

// Fabric Mod 配置：展开 fabric.mod.json 中的 ${version}
// 修复：改用显式 tasks.withType<ProcessResources>() 形式（不依赖 Kotlin DSL 访问器生成）
//       且 expand() 参数必须为 Pair（其签名是 vararg Pair<String, Any>）
tasks.withType<ProcessResources>().configureEach {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

// 版本（独立构建时 jar 名 = 项目名 "ai-bridge" + 版本号）
version = "1.0.0"
group = "com.aimc"
