package com.tungsten.fcl.mod;

import android.content.Context;

import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.setting.LauncherSettings;

import java.io.File;
import java.util.List;

/**
 * Mod 下载管理器 — 统一管理 Modrinth / CurseForge 多源下载
 *
 * 从 LauncherSettings 读取 API Key 和源偏好。
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
    private CurseForgeApi curseForgeApi;
    private final FCLRepository repository;
    private final LauncherSettings settings;

    public ModDownloadManager(FCLRepository repository, Context context) {
        this.repository = repository;
        this.settings = LauncherSettings.getInstance(context);
        this.curseForgeApi = new CurseForgeApi(settings.getCurseForgeApiKey());
    }

    public ModrinthApi getModrinthApi() { return modrinthApi; }
    public CurseForgeApi getCurseForgeApi() { return curseForgeApi; }

    /**
     * 从设置刷新 CurseForge API Key
     */
    public void refreshApiKey() {
        this.curseForgeApi = new CurseForgeApi(settings.getCurseForgeApiKey());
    }

    public void searchMods(AddonType type, String gameVersion, String searchFilter,
                           int page, int pageSize, ModSource source, SearchCallback callback) {
        new Thread(() -> {
            try {
                boolean hasCfKey = settings.hasCurseForgeApiKey();

                if (source == ModSource.MODRINTH || source == ModSource.AUTO) {
                    searchModrinth(type, gameVersion, searchFilter, page, pageSize, callback);
                }
                if (source == ModSource.CURSEFORGE || (source == ModSource.AUTO && hasCfKey)) {
                    try {
                        searchCurseForge(type, gameVersion, searchFilter, page, pageSize, callback);
                    } catch (Exception e) {
                        // CurseForge 失败不影响 Modrinth 结果
                        if (source == ModSource.CURSEFORGE) throw e;
                    }
                }
                callback.onComplete();
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    private void searchModrinth(AddonType type, String gameVersion, String searchFilter,
                                int page, int pageSize, SearchCallback callback) throws Exception {
        ModrinthApi.ProjectType pt = toModrinthType(type);
        List<ModrinthApi.ModSearchResult> results = modrinthApi.search(
                pt, gameVersion, searchFilter, page, pageSize, ModrinthApi.SortType.RELEVANCE);
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

    private void searchCurseForge(AddonType type, String gameVersion, String searchFilter,
                                  int page, int pageSize, SearchCallback callback) throws Exception {
        refreshApiKey();
        CurseForgeApi.Section section = toCurseForgeSection(type);
        List<CurseForgeApi.ModSearchResult> results = curseForgeApi.search(
                section, gameVersion, searchFilter, page, pageSize, CurseForgeApi.SortField.POPULARITY);
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

    public void downloadMod(UnifiedModResult mod, String downloadUrl, String fileName,
                           String versionId, DownloadCallback callback) {
        new Thread(() -> {
            try {
                File modsDir = repository.getVersionModsDir(versionId);
                modsDir.mkdirs();
                File destFile = new File(modsDir, fileName);
                doDownload(mod.source, downloadUrl, destFile);
                callback.onSuccess(destFile);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    public void downloadModpack(UnifiedModResult modpack, String downloadUrl, String fileName,
                                DownloadCallback callback) {
        new Thread(() -> {
            try {
                File dir = repository.getModpacksDir();
                dir.mkdirs();
                File destFile = new File(dir, fileName);
                doDownload(modpack.source, downloadUrl, destFile);
                callback.onSuccess(destFile);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    public void downloadShaderPack(UnifiedModResult shader, String downloadUrl, String fileName,
                                   DownloadCallback callback) {
        new Thread(() -> {
            try {
                File dir = repository.getShaderPacksDir();
                dir.mkdirs();
                File destFile = new File(dir, fileName);
                doDownload(shader.source, downloadUrl, destFile);
                callback.onSuccess(destFile);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    public void downloadResourcePack(UnifiedModResult pack, String downloadUrl, String fileName,
                                     DownloadCallback callback) {
        new Thread(() -> {
            try {
                File dir = repository.getResourcePacksDir();
                dir.mkdirs();
                File destFile = new File(dir, fileName);
                doDownload(pack.source, downloadUrl, destFile);
                callback.onSuccess(destFile);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    private void doDownload(ModSource source, String url, File dest) throws Exception {
        if (source == ModSource.MODRINTH) {
            modrinthApi.downloadFile(url, dest);
        } else {
            refreshApiKey();
            curseForgeApi.downloadFile(url, dest);
        }
    }

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

    public interface SearchCallback {
        void onResult(UnifiedModResult result);
        void onComplete();
        void onError(String message, Exception e);
    }

    public interface DownloadCallback {
        void onSuccess(File file);
        void onError(String message, Exception e);
    }

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
