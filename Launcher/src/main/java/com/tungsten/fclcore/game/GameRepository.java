package com.tungsten.fclcore.game;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏版本仓库 — 直接取自 FCL GameRepository 核心能力
 *
 * 管理已安装版本的扫描、加载、路径获取。
 */
public class GameRepository implements VersionProvider {

    private final java.io.File baseDirectory;
    private final Map<String, Version> versions = new HashMap<>();
    private volatile boolean loaded = false;

    public GameRepository(java.io.File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public java.io.File getBaseDirectory() { return baseDirectory; }
    public boolean isLoaded() { return loaded; }

    @Override
    public boolean hasVersion(String id) {
        return id != null && versions.containsKey(id);
    }

    @Override
    public Version getVersion(String id) throws VersionNotFoundException {
        if (!hasVersion(id))
            throw new VersionNotFoundException("Version '" + id + "' does not exist in " + versions.keySet());
        return versions.get(id);
    }

    public int getVersionCount() { return versions.size(); }
    public java.util.Collection<Version> getVersions() { return versions.values(); }

    /**
     * 扫描并加载所有版本 — 仿 FCL DefaultGameRepository.refreshVersions
     */
    public void refreshVersions() {
        versions.clear();
        java.io.File versionsDir = new java.io.File(baseDirectory, "versions");
        if (!versionsDir.exists() || !versionsDir.isDirectory()) {
            loaded = true;
            return;
        }
        java.io.File[] dirs = versionsDir.listFiles(java.io.File::isDirectory);
        if (dirs == null) { loaded = true; return; }

        for (java.io.File dir : dirs) {
            java.io.File jsonFile = new java.io.File(dir, dir.getName() + ".json");
            if (!jsonFile.exists()) continue;
            try {
                String content = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath()), "UTF-8");
                Version version = new com.google.gson.Gson().fromJson(content, Version.class);
                if (version != null) {
                    if (version.getId() == null || version.getId().isEmpty()) {
                        version = new Version(dir.getName());
                    }
                    versions.put(dir.getName(), version);
                }
            } catch (Exception e) {
                // 跳过损坏的版本 JSON
            }
        }
        loaded = true;
    }

    public java.io.File getVersionRoot(String id) {
        return new java.io.File(baseDirectory, "versions/" + id);
    }

    public java.io.File getLibrariesDirectory(Version version) {
        return new java.io.File(baseDirectory, "libraries");
    }

    /**
     * 获取库文件路径 — 仿 FCL DefaultGameRepository.getLibraryFile
     */
    public java.io.File getLibraryFile(Version version, Library lib) {
        if ("local".equals(lib.getHint())) {
            if (lib.getFileName() != null)
                return new java.io.File(getVersionRoot(version.getId()), "libraries/" + lib.getFileName());
            return new java.io.File(getVersionRoot(version.getId()), "libraries/" + lib.getArtifact().getFileName());
        } else {
            return new java.io.File(getLibrariesDirectory(version), lib.getPath());
        }
    }

    /**
     * 获取版本 jar
     */
    public java.io.File getVersionJar(Version version) {
        Version v = version.isResolved() ? version : version;
        String id = v.getJar() != null ? v.getJar() : v.getId();
        return new java.io.File(getVersionRoot(id), id + ".jar");
    }

    public java.io.File getModsDirectory(String id) {
        return new java.io.File(getVersionRoot(id), "mods");
    }

    /**
     * 获取资源索引文件
     */
    public java.io.File getIndexFile(String versionId, String assetIndexId) {
        return new java.io.File(baseDirectory, "assets/indexes/" + assetIndexId + ".json");
    }

    /**
     * 获取资源对象文件
     */
    public java.nio.file.Path getAssetObject(String versionId, String assetIndexId, AssetObject object) {
        return new java.io.File(baseDirectory, "assets/objects/" + object.getLocation()).toPath();
    }
}
