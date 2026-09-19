package com.tungsten.fcl.mod;

import android.content.Context;

import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.setting.LauncherSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Mod 下载管理器 — 统一管理 Modrinth / CurseForge 多源下载
 *
 * 从 LauncherSettings 读取 API Key、源偏好、镜像开关。
 * 支持 Modrinth 依赖自动下载。
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

    private final FCLRepository repository;
    private final LauncherSettings settings;
    private ModrinthApi modrinthApi;
    private CurseForgeApi curseForgeApi;

    public ModDownloadManager(FCLRepository repository, Context context) {
        this.repository = repository;
        this.settings = LauncherSettings.getInstance(context);
        refreshApis();
    }

    /**
     * 从设置重新加载 API 配置 (Key / 镜像开关)
     */
    public void refreshApis() {
        // Modrinth 无需 API Key，只需镜像开关 (国内默认开启)
        this.modrinthApi = new ModrinthApi(settings.isModrinthMirrorEnabled());
        this.curseForgeApi = new CurseForgeApi(settings.getCurseForgeApiKey());
    }

    /** 已弃用别名，保留兼容 */
    public void refreshApiKey() { refreshApis(); }

    public ModrinthApi getModrinthApi() { return modrinthApi; }
    public CurseForgeApi getCurseForgeApi() { return curseForgeApi; }

    public boolean isCurseForgeAvailable() {
        return settings.hasCurseForgeApiKey();
    }

    // === 搜索 ===

    public void searchMods(AddonType type, String gameVersion, String searchFilter,
                           int page, int pageSize, ModSource source, SearchCallback callback) {
        searchMods(type, gameVersion, null, null, searchFilter, page, pageSize, source, callback);
    }

    /**
     * 搜索 (支持分类和加载器筛选)
     */
    public void searchMods(AddonType type, String gameVersion, String category, String loader,
                           String searchFilter, int page, int pageSize,
                           ModSource source, SearchCallback callback) {
        new Thread(() -> {
            try {
                refreshApis();
                boolean hasCfKey = settings.hasCurseForgeApiKey();

                if (source == ModSource.MODRINTH || source == ModSource.AUTO) {
                    searchModrinth(type, gameVersion, category, loader,
                            searchFilter, page, pageSize, callback);
                }
                if (source == ModSource.CURSEFORGE || (source == ModSource.AUTO && hasCfKey)) {
                    try {
                        searchCurseForge(type, gameVersion, searchFilter, page, pageSize, callback);
                    } catch (Exception e) {
                        if (source == ModSource.CURSEFORGE) throw e;
                    }
                }
                callback.onComplete();
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    private void searchModrinth(AddonType type, String gameVersion, String category, String loader,
                                String searchFilter, int page, int pageSize,
                                SearchCallback callback) throws Exception {
        ModrinthApi.ProjectType pt = toModrinthType(type);
        List<ModrinthApi.ModSearchResult> results = modrinthApi.search(
                pt, gameVersion, category, loader, searchFilter, page, pageSize,
                ModrinthApi.SortType.RELEVANCE);
        for (ModrinthApi.ModSearchResult r : results) {
            callback.onResult(toUnified(r));
        }
    }

    private UnifiedModResult toUnified(ModrinthApi.ModSearchResult r) {
        UnifiedModResult u = new UnifiedModResult();
        u.source = ModSource.MODRINTH;
        u.id = r.projectId;
        u.slug = r.slug;
        u.title = r.title;
        u.description = r.description;
        u.author = r.author;
        u.downloadCount = r.downloadCount;
        u.iconUrl = r.iconUrl;
        u.pageUrl = r.pageUrl;
        u.categories = r.categories;
        u.gameVersions = r.gameVersions;
        return u;
    }

    private void searchCurseForge(AddonType type, String gameVersion, String searchFilter,
                                  int page, int pageSize, SearchCallback callback) throws Exception {
        CurseForgeApi.Section section = toCurseForgeSection(type);
        List<CurseForgeApi.ModSearchResult> results = curseForgeApi.search(
                section, gameVersion, searchFilter, page, pageSize,
                CurseForgeApi.SortField.POPULARITY);
        for (CurseForgeApi.ModSearchResult r : results) {
            UnifiedModResult u = new UnifiedModResult();
            u.source = ModSource.CURSEFORGE;
            u.id = String.valueOf(r.id);
            u.title = r.name;
            u.description = r.summary;
            u.downloadCount = r.downloadCount;
            u.iconUrl = r.iconUrl;
            u.pageUrl = r.websiteUrl;
            u.gameVersions = r.gameVersions;
            callback.onResult(u);
        }
    }

    // === 单文件下载 ===

    public void downloadMod(UnifiedModResult mod, String downloadUrl, String fileName,
                           String versionId, DownloadCallback callback) {
        downloadTo(resolveModsDir(versionId), mod.source, downloadUrl, fileName, null, callback);
    }

    public void downloadModpack(UnifiedModResult modpack, String downloadUrl, String fileName,
                                DownloadCallback callback) {
        downloadTo(repository.getModpacksDir(), modpack.source, downloadUrl, fileName, null, callback);
    }

    public void downloadShaderPack(UnifiedModResult shader, String downloadUrl, String fileName,
                                   DownloadCallback callback) {
        downloadTo(repository.getShaderPacksDir(), shader.source, downloadUrl, fileName, null, callback);
    }

    public void downloadResourcePack(UnifiedModResult pack, String downloadUrl, String fileName,
                                     DownloadCallback callback) {
        downloadTo(repository.getResourcePacksDir(), pack.source, downloadUrl, fileName, null, callback);
    }

    private File resolveModsDir(String versionId) {
        File versionMods = repository.getVersionModsDir(versionId);
        versionMods.mkdirs();
        return versionMods;
    }

    private void downloadTo(File dir, ModSource source, String url, String fileName,
                            String expectedSha1, DownloadCallback callback) {
        new Thread(() -> {
            try {
                dir.mkdirs();
                File destFile = new File(dir, fileName);
                if (source == ModSource.MODRINTH) {
                    modrinthApi.downloadFile(url, destFile, expectedSha1);
                } else {
                    curseForgeApi.downloadFile(url, destFile);
                }
                callback.onSuccess(destFile);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
    }

    // === Modrinth 依赖自动下载 ===

    /**
     * 安装 Modrinth 版本及其必需依赖 — 对齐 FCL ModDependenciesResolver
     *
     * @param projectId   项目 ID
     * @param version     选中的版本
     * @param gameVersion 当前游戏版本
     * @param loader      当前加载器 (fabric/forge/quilt/neoforge)
     * @param versionId   目标游戏版本目录 ID
     * @param callback    进度回调
     */
    public void installWithDependencies(String projectId, ModrinthApi.ModVersion version,
                                        String gameVersion, String loader,
                                        String versionId, DependencyCallback callback) {
        new Thread(() -> {
            try {
                File modsDir = resolveModsDir(versionId);
                List<File> installed = new ArrayList<>();

                // 1. 下载主文件
                ModrinthApi.ModVersionFile primary = version.getPrimaryFile();
                if (primary != null) {
                    File dest = new File(modsDir, primary.filename);
                    modrinthApi.downloadFile(primary.url, dest, primary.sha1);
                    installed.add(dest);
                    callback.onProgress("已安装主文件: " + primary.filename);
                }

                // 2. 下载必需依赖
                for (ModrinthApi.ModDependency dep : version.dependencies) {
                    if (!dep.isRequired()) continue;
                    try {
                        ModrinthApi.ModVersion resolved;
                        if (dep.versionId != null && !dep.versionId.isEmpty()) {
                            resolved = modrinthApi.getVersion(dep.versionId);
                        } else if (dep.projectId != null && !dep.projectId.isEmpty()) {
                            List<ModrinthApi.ModVersion> list = modrinthApi.getVersions(
                                    dep.projectId, gameVersion, loader);
                            if (list.isEmpty()) {
                                list = modrinthApi.getVersions(dep.projectId);
                            }
                            resolved = list.isEmpty() ? null : list.get(0);
                        } else {
                            resolved = null;
                        }

                        if (resolved == null) continue;
                        ModrinthApi.ModVersionFile file = resolved.getPrimaryFile();
                        if (file == null) continue;

                        File dest = new File(modsDir, file.filename);
                        if (!dest.exists()) {
                            modrinthApi.downloadFile(file.url, dest, file.sha1);
                            installed.add(dest);
                            callback.onProgress("已安装依赖: " + file.filename);
                        }
                    } catch (Exception e) {
                        callback.onProgress("依赖下载失败 (忽略): " + dep.projectId);
                    }
                }

                callback.onSuccess(installed);
            } catch (Exception e) {
                callback.onError(e.getMessage(), e);
            }
        }).start();
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

    public interface DependencyCallback {
        void onProgress(String message);
        void onSuccess(List<File> installed);
        void onError(String message, Exception e);
    }

    public static class UnifiedModResult {
        public ModSource source;
        public String id;
        public String slug;
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
