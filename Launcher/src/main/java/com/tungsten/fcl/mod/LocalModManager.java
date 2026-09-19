package com.tungsten.fcl.mod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 本地 Mod 管理器 — 对齐 FCL ModManager
 *
 * 扫描版本 mods 目录、启用/禁用 Mod、删除、扩展名处理。
 * FCL 的禁用方式：把 xxx.jar 重命名为 xxx.jar.disabled
 */
public class LocalModManager {

    /** FCL 禁用后缀 */
    public static final String DISABLED_EXTENSION = ".disabled";

    private final File modsDir;

    public LocalModManager(File modsDir) {
        this.modsDir = modsDir;
    }

    public File getModsDir() { return modsDir; }

    /**
     * 扫描目录下所有 Mod 文件
     */
    public List<File> getModFiles() {
        List<File> result = new ArrayList<>();
        if (!modsDir.exists() || !modsDir.isDirectory()) return result;
        File[] files = modsDir.listFiles();
        if (files == null) return result;
        for (File f : files) {
            if (f.isFile() && isModFile(f.getName())) {
                result.add(f);
            }
        }
        Collections.sort(result, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    /**
     * 是否为 Mod 文件 (.jar / .jar.disabled / .zip 等)
     */
    public static boolean isModFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".jar") || lower.endsWith(".jar" + DISABLED_EXTENSION)
                || lower.endsWith(".zip") || lower.endsWith(".zip" + DISABLED_EXTENSION)
                || lower.endsWith(".litemod") || lower.endsWith(".litemod" + DISABLED_EXTENSION);
    }

    /**
     * 是否已禁用 — 对齐 FCL isDisabled()
     */
    public static boolean isDisabled(File file) {
        return file.getName().endsWith(DISABLED_EXTENSION);
    }

    /**
     * 获取真实文件名 (去掉 .disabled)
     */
    public static String getRealName(File file) {
        String name = file.getName();
        if (name.endsWith(DISABLED_EXTENSION)) {
            return name.substring(0, name.length() - DISABLED_EXTENSION.length());
        }
        return name;
    }

    /**
     * 启用 Mod — 对齐 FCL enableMod()
     *
     * @return 启用后的文件
     */
    public File enableMod(File file) throws IOException {
        if (!isDisabled(file)) return file;
        String realName = getRealName(file);
        File target = new File(file.getParentFile(), realName);
        moveFile(file, target);
        return target;
    }

    /**
     * 禁用 Mod — 对齐 FCL disableMod()
     *
     * @return 禁用后的文件
     */
    public File disableMod(File file) throws IOException {
        if (isDisabled(file)) return file;
        File target = new File(file.getParentFile(), file.getName() + DISABLED_EXTENSION);
        moveFile(file, target);
        return target;
    }

    /**
     * 切换启用状态
     */
    public File toggleMod(File file) throws IOException {
        return isDisabled(file) ? enableMod(file) : disableMod(file);
    }

    /**
     * 删除 Mod (同时删除 disabled 版本)
     */
    public boolean deleteMod(File file) {
        boolean ok = file.delete();
        // 清理可能存在的对立状态文件
        String realName = getRealName(file);
        File counterpart = isDisabled(file)
                ? new File(file.getParentFile(), realName)
                : new File(file.getParentFile(), realName + DISABLED_EXTENSION);
        if (counterpart.exists()) counterpart.delete();
        return ok;
    }

    /**
     * 复制 Mod 文件到 mods 目录
     */
    public File copyMod(File source) throws IOException {
        modsDir.mkdirs();
        File target = new File(modsDir, source.getName());
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    /**
     * 检查是否已安装同名 Mod
     */
    public boolean hasMod(String fileName) {
        File direct = new File(modsDir, fileName);
        File disabled = new File(modsDir, fileName + DISABLED_EXTENSION);
        return direct.exists() || disabled.exists();
    }

    /**
     * 获取 Mod 统计信息
     */
    public ModStats getStats() {
        ModStats stats = new ModStats();
        for (File f : getModFiles()) {
            if (isDisabled(f)) stats.disabled++;
            else stats.enabled++;
            stats.totalSize += f.length();
        }
        stats.total = stats.enabled + stats.disabled;
        return stats;
    }

    private void moveFile(File from, File to) throws IOException {
        Path source = from.toPath();
        Path target = to.toPath();
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // 跨文件系统 fallback
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(source);
        }
    }

    /**
     * Mod 统计
     */
    public static class ModStats {
        public int total;
        public int enabled;
        public int disabled;
        public long totalSize;

        public String formatSize() {
            if (totalSize < 1024) return totalSize + " B";
            if (totalSize < 1024 * 1024) return String.format("%.1f KB", totalSize / 1024.0);
            if (totalSize < 1024L * 1024 * 1024) return String.format("%.1f MB", totalSize / (1024.0 * 1024));
            return String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
        }
    }
}
