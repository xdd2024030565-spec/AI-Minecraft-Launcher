package com.tungsten.fcl.mod;

import com.tungsten.fcl.FCLRepository;

import java.io.File;
import java.util.List;

/**
 * Mod 下载管理器 — 统一管理 Modrinth / CurseForge 多源下载
 *
 * 仿 FCL 的多源聚合策略，提供统一的搜索和下载接口。
 */
public class ModDownloadManager {

    public enum ModSource {
        MODRINTH("Modrinth"),
        CURSEFORGE("CurseForge"),
        AUTO("自动");

        private final String displayName;
        ModSource(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum AddonType {
        MOD,
        MODPACK,
        RESOURCE_PACK,
        SHADER_PACK
    }

    private final ModrinthApi modrinthApi = new ModrinthApi();
    private final CurseForgeApi curseForgeApi = new CurseForgeApi();
    private final FCLRepository repository;

    public ModDownloadManager(FCLRepository repository) {
        this.repository = repository;
    }

    public ModrinthApi getModrinthApi() { return modrinthApi; }
    public CurseForgeApi getCurseForgeApi() { return curseForgeApi; }

    /**
     * 搜索 Mod (多源聚合)
     */
    public void searchMods(AddonType type, String gameVersion, String searchFilter,
                           int page, int pageSize,
                           ModSource source,
                           SearchCallback callback) {
        new Thread(() -> {
            try {
                if (source == ModSource.MODRINTH || source == ModSource.AUTO) {
                    ModrinthApi.ProjectType pt = toModrinthType(type);
                    List<ModrinthApi.ModSearchResult> results = modrinthApi.search(
                            pt, gameVersion, searchFilter, page, pageSize,
                            ModrinthApi.SortType.RELEVANCE
                    );
                    for (ModrinthApi.ModSearchResult r : results) {
                        UnifiedModResult unified = new UnifiedModResult();
                        unified.source = ModSource.MODRINTH;
                        unified.id = r.projectId;
                        unified.title = r.title;
                        unified.description = r.description;
                        unified.author = r.author;
                        unified.downloadCount = r.downloadCount;
                        unified.iconUrl = r.iconUrl;
                        unified.pageUrl = r.pageUrl;
                        unified.categories = r.categories;
                        unified.gameVersions = r.gameVersions;
                        callback.onResult(unified);
                    }
                }
                if (source == ModSource.CURSEFORGE || source == ModSource.AUTO) {
                    CurseForgeApi.Section section = toCurseForgeSection(type);
                    List<CurseForgeApi.ModSearchResult> results = curseForgeApi.search(
                            section, gameVersion, searchFilter, page, pageSize,
                            CurseForgeApi.SortField.POPULARITY
                    );
                    for (CurseForgeApi.ModSearchResult r : results) {
                        UnifiedModResult unified = new UnifiedModResult();
                        unified.source = ModSource.CURSEFORGE;
                        unified.id = String.valueOf(r.id);
                        unified.title = r.name;
                        unified.description = r.summary;
                        unified.downloadCount = r.downloadCount;
                        unified.iconUrl = r.iconUrl;
                        unified.pageUrl = r.websiteUrl;
                        unified.gameVersions = r.gameVersions;
                        callback.onResult(unified);
                    }
                }
                callback.onComplete();
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    /**
     * 下载 Mod 到指定版本目录
     */
    public void downloadMod(UnifiedModResult mod, String downloadUrl, String fileName,
                           String versionId, DownloadCallback callback) {
        new Thread(() -> {
            try {
                File modsDir = repository.getVersionModsDir(versionId);
                modsDir.mkdirs();
                File destFile = new File(modsDir, fileName);

                if (mod.source == ModSource.MODRINTH) {
                    modrinthApi.downloadFile(downloadUrl, destFile);
                } else {
                    curseForgeApi.downloadFile(downloadUrl, destFile);
                }

                callback.onSuccess(destFile);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    /**
     * 下载整合包并安装
     */
    public void downloadModpack(UnifiedModResult modpack, String downloadUrl, String fileName,
                                DownloadCallback callback) {
        new Thread(() -> {
            try {
                File modpacksDir = repository.getModpacksDir();
                modpacksDir.mkdirs();
                File destFile = new File(modpacksDir, fileName);

                if (modpack.source == ModSource.MODRINTH) {
                    modrinthApi.downloadFile(downloadUrl, destFile);
                } else {
                    curseForgeApi.downloadFile(downloadUrl, destFile);
                }

                callback.onSuccess(destFile);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    /**
     * 下载光影包
     */
    public void downloadShaderPack(UnifiedModResult shader, String downloadUrl, String fileName,
                                   String versionId, DownloadCallback callback) {
        new Thread(() -> {
            try {
                File shaderDir = repository.getShaderPacksDir();
                shaderDir.mkdirs();
                File destFile = new File(shaderDir, fileName);

                if (shader.source == ModSource.MODRINTH) {
                    modrinthApi.downloadFile(downloadUrl, destFile);
                } else {
                    curseForgeApi.downloadFile(downloadUrl, destFile);
                }

                callback.onSuccess(destFile);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    /**
     * 下载资源包
     */
    public void downloadResourcePack(UnifiedModResult pack, String downloadUrl, String fileName,
                                     DownloadCallback callback) {
        new Thread(() -> {
            try {
                File packsDir = repository.getResourcePacksDir();
                packsDir.mkdirs();
                File destFile = new File(packsDir, fileName);

                if (pack.source == ModSource.MODRINTH) {
                    modrinthApi.downloadFile(downloadUrl, destFile);
                } else {
                    curseForgeApi.downloadFile(downloadUrl, destFile);
                }

                callback.onSuccess(destFile);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    // === 类型转换 ===

    private ModrinthApi.ProjectType toModrinthType(AddonType type) {
        switch (type) {
            case MOD: return ModrinthApi.ProjectType.MOD;
            case MODPACK: return ModrinthApi.ProjectType.MODPACK;
            case RESOURCE_PACK: return ModrinthApi.ProjectType.RESOURCE_PACK;
            case SHADER_PACK: return ModrinthApi.ProjectType.SHADER_PACK;
            default: return ModrinthApi.ProjectType.MOD;
        }
    }

    private CurseForgeApi.Section toCurseForgeSection(AddonType type) {
        switch (type) {
            case MOD: return CurseForgeApi.Section.MOD;
            case MODPACK: return CurseForgeApi.Section.MODPACK;
            case RESOURCE_PACK: return CurseForgeApi.Section.RESOURCE_PACK;
            case SHADER_PACK: return CurseForgeApi.Section.SHADER_PACK;
            default: return CurseForgeApi.Section.MOD;
        }
    }

    // === 回调接口 ===

    public interface SearchCallback {
        void onResult(UnifiedModResult result);
        void onComplete();
        void onError(String message, Exception e);
    }

    public interface DownloadCallback {
        void onSuccess(File file);
        void onError(String message, Exception e);
    }

    // === 统一数据类 ===

    public static class UnifiedModResult {
        public ModSource source;
        public String id;
        public String title;
        public String description;
        public String author;
        public int downloadCount;
        public String iconUrl;
        public String pageUrl;
        public List<String> categories;
        public List<String> gameVersions;
    }
}
