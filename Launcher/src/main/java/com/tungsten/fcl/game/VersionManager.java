package com.tungsten.fcl.game;

import com.tungsten.fcl.FCLRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏版本管理器
 *
 * 负责扫描已安装的版本、检查版本安装状态、删除版本等。
 */
public class VersionManager {

    private static VersionManager instance;
    private final FCLRepository repository;

    private VersionManager(FCLRepository repository) {
        this.repository = repository;
    }

    public static VersionManager getInstance(FCLRepository repository) {
        if (instance == null) {
            synchronized (VersionManager.class) {
                if (instance == null) {
                    instance = new VersionManager(repository);
                }
            }
        }
        return instance;
    }

    /**
     * 获取已安装的版本列表
     */
    public List<String> getInstalledVersions() {
        List<String> versions = new ArrayList<>();
        File versionsDir = repository.getVersionsDir();
        if (versionsDir.exists() && versionsDir.isDirectory()) {
            File[] dirs = versionsDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    // 版本目录必须包含对应的 .json 文件才算已安装
                    File json = new File(dir, dir.getName() + ".json");
                    if (json.exists()) {
                        versions.add(dir.getName());
                    }
                }
            }
        }
        return versions;
    }

    /**
     * 检查版本是否已安装
     */
    public boolean isVersionInstalled(String versionId) {
        return getVersionJson(versionId).exists();
    }

    /**
     * 获取版本的 JSON 描述文件
     */
    public File getVersionJson(String versionId) {
        return new File(repository.getVersionDir(versionId), versionId + ".json");
    }

    /**
     * 获取版本的 client JAR
     */
    public File getVersionJar(String versionId) {
        return repository.getVersionJar(versionId);
    }

    /**
     * 删除版本
     */
    public void removeVersion(String versionId) {
        File versionDir = repository.getVersionDir(versionId);
        if (versionDir.exists()) {
            deleteRecursive(versionDir);
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
