package com.tungsten.fcl.game;

import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.download.GameDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 游戏版本管理器 — 仿 FCL DefaultGameRepository
 *
 * 负责扫描已安装版本、检查安装状态、删除版本、获取远程版本列表等。
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
                    File json = new File(dir, dir.getName() + ".json");
                    if (json.exists()) {
                        versions.add(dir.getName());
                    }
                }
            }
        }
        Collections.sort(versions);
        return versions;
    }

    /**
     * 获取已安装版本的详细信息
     */
    public List<InstalledVersionInfo> getInstalledVersionInfos() {
        List<InstalledVersionInfo> infos = new ArrayList<>();
        for (String id : getInstalledVersions()) {
            File jar = repository.getVersionJar(id);
            File json = repository.getVersionJson(id);
            File modsDir = repository.getVersionModsDir(id);
            int modCount = (modsDir.exists() && modsDir.isDirectory()) ?
                    modsDir.listFiles((d, n) -> n.endsWith(".jar") || n.endsWith(".disabled")).length : 0;
            infos.add(new InstalledVersionInfo(
                    id,
                    json.length(),
                    jar.exists() ? jar.length() : 0,
                    modCount
            ));
        }
        return infos;
    }

    public boolean isVersionInstalled(String versionId) {
        return repository.getVersionJson(versionId).exists();
    }

    public File getVersionJson(String versionId) {
        return repository.getVersionJson(versionId);
    }

    public File getVersionJar(String versionId) {
        return repository.getVersionJar(versionId);
    }

    public File getVersionModsDir(String versionId) {
        return repository.getVersionModsDir(versionId);
    }

    /**
     * 删除版本
     */
    public void removeVersion(String versionId) {
        File versionDir = repository.getVersionDir(versionId);
        deleteRecursive(versionDir);
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

    /**
     * 已安装版本信息
     */
    public static class InstalledVersionInfo {
        public final String id;
        public final long jsonSize;
        public final long jarSize;
        public final int modCount;

        public InstalledVersionInfo(String id, long jsonSize, long jarSize, int modCount) {
            this.id = id;
            this.jsonSize = jsonSize;
            this.jarSize = jarSize;
            this.modCount = modCount;
        }

        public boolean isComplete() {
            return jsonSize > 0 && jarSize > 0;
        }
    }
}
