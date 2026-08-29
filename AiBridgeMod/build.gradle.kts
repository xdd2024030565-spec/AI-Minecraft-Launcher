plugins {
    id("fabric-loom")
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
