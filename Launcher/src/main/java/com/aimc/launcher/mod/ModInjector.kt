package com.aimc.launcher.mod

import com.tungsten.fcl.FCLRepository
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Mod 自动注入器
 *
 * 在游戏启动前，将 AI Bridge Mod jar 自动复制到游戏的 mods 目录，
 * 实现启动器 + Mod 的集成 (阶段 3)。
 */
object ModInjector {

    private const val AI_BRIDGE_JAR_NAME = "ai-bridge-1.0.0.jar"

    /**
     * 将 AI Bridge Mod 注入到指定版本的 mods 目录
     *
     * @return true 如果注入成功或已存在
     */
    @JvmStatic
    fun injectAiBridge(repository: FCLRepository, versionId: String): Boolean {
        val modsDir = File(repository.getVersionDir(versionId), "mods")
        modsDir.mkdirs()

        val modFile = File(modsDir, AI_BRIDGE_JAR_NAME)
        if (modFile.exists()) {
            // 已注入过
            return true
        }

        return try {
            // TODO: 从 APK assets 复制实际的 AI Bridge mod jar
            // val context = FCLApplication.getAppContext()
            // val inputStream = context.assets.open("mods/$AI_BRIDGE_JAR_NAME")
            // copyFile(inputStream, modFile)
            // inputStream.close()

            // 当前创建占位文件，后续替换为实际 mod jar
            modFile.createNewFile()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 检查 Mod 是否已注入
     */
    @JvmStatic
    fun isModInjected(repository: FCLRepository, versionId: String): Boolean {
        val modFile = File(repository.getVersionDir(versionId), "mods/$AI_BRIDGE_JAR_NAME")
        return modFile.exists()
    }

    /**
     * 移除已注入的 Mod
     */
    @JvmStatic
    fun removeMod(repository: FCLRepository, versionId: String) {
        val modFile = File(repository.getVersionDir(versionId), "mods/$AI_BRIDGE_JAR_NAME")
        modFile.delete()
    }

    /**
     * 复制输入流到目标文件
     */
    private fun copyFile(inputStream: InputStream, dest: File) {
        FileOutputStream(dest).use { out ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
            }
        }
    }
}
