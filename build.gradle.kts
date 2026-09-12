// Top-level build file
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    // Fabric Loom (Mod 编译)
    id("fabric-loom") version "1.17" apply false  // 修复：使用稳定版本
}