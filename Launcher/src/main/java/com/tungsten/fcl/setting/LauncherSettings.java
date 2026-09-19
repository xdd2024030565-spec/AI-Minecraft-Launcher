package com.tungsten.fcl.setting;

import android.content.Context;
import android.content.SharedPreferences;

import com.tungsten.fcl.download.AutoDownloadProvider;
import com.tungsten.fcl.download.BMCLAPIDownloadProvider;
import com.tungsten.fcl.download.DownloadProvider;
import com.tungsten.fcl.download.MojangDownloadProvider;

/**
 * 启动器全局配置 — 仿 FCL GlobalConfig / Settings
 *
 * 集中管理下载源、API Key、镜像地址等可配置项。
 */
public class LauncherSettings {

    private static final String PREFS_NAME = "fcl_launcher_settings";
    private static LauncherSettings instance;

    private final SharedPreferences prefs;

    public static final int SOURCE_AUTO = 0;
    public static final int SOURCE_BMCLAPI = 1;
    public static final int SOURCE_MOJANG = 2;

    public static final int MOD_SOURCE_MODRINTH = 0;
    public static final int MOD_SOURCE_CURSEFORGE = 1;
    public static final int MOD_SOURCE_AUTO = 2;

    private LauncherSettings(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static LauncherSettings getInstance(Context context) {
        if (instance == null) {
            synchronized (LauncherSettings.class) {
                if (instance == null) {
                    instance = new LauncherSettings(context);
                }
            }
        }
        return instance;
    }

    // === 下载源 ===

    public int getDownloadSource() {
        return prefs.getInt("download_source", SOURCE_AUTO);
    }

    public void setDownloadSource(int source) {
        prefs.edit().putInt("download_source", source).apply();
    }

    public DownloadProvider getDownloadProvider() {
        switch (getDownloadSource()) {
            case SOURCE_BMCLAPI:
                return new BMCLAPIDownloadProvider();
            case SOURCE_MOJANG:
                return new MojangDownloadProvider();
            case SOURCE_AUTO:
            default:
                return new AutoDownloadProvider(true);
        }
    }

    // === CurseForge API Key (用户自行填写) ===

    public String getCurseForgeApiKey() {
        return prefs.getString("curseforge_api_key", "");
    }

    public void setCurseForgeApiKey(String apiKey) {
        prefs.edit().putString("curseforge_api_key", apiKey == null ? "" : apiKey.trim()).apply();
    }

    public boolean hasCurseForgeApiKey() {
        String key = getCurseForgeApiKey();
        return key != null && !key.isEmpty();
    }

    // === Modrinth (无需 Key，仅镜像开关) ===

    /**
     * 是否使用 Modrinth 镜像 (国内推荐开启)
     */
    public boolean isModrinthMirrorEnabled() {
        return prefs.getBoolean("modrinth_mirror_enabled", true);
    }

    public void setModrinthMirrorEnabled(boolean enabled) {
        prefs.edit().putBoolean("modrinth_mirror_enabled", enabled).apply();
    }

    public String getModrinthMirror() {
        return prefs.getString("modrinth_mirror", "https://mod.mcimirror.top/modrinth");
    }

    public void setModrinthMirror(String mirror) {
        prefs.edit().putString("modrinth_mirror", mirror).apply();
    }

    // === 镜像地址 ===

    public String getBMCLAPIRoot() {
        return prefs.getString("bmclapi_root", "https://bmclapi2.bangbang93.com");
    }

    public void setBMCLAPIRoot(String root) {
        prefs.edit().putString("bmclapi_root", root).apply();
    }

    public String getCurseForgeMirror() {
        return prefs.getString("curseforge_mirror", "https://mod.mcimirror.top/curseforge");
    }

    public void setCurseForgeMirror(String mirror) {
        prefs.edit().putString("curseforge_mirror", mirror).apply();
    }

    // === Mod 源偏好 ===

    public int getModSource() {
        return prefs.getInt("mod_source", MOD_SOURCE_AUTO);
    }

    public void setModSource(int source) {
        prefs.edit().putInt("mod_source", source).apply();
    }

    // === 下载并发数 ===

    public int getDownloadConcurrency() {
        return prefs.getInt("download_concurrency", 8);
    }

    public void setDownloadConcurrency(int concurrency) {
        prefs.edit().putInt("download_concurrency", Math.max(1, Math.min(32, concurrency))).apply();
    }

    // === 完整性校验 ===

    public boolean isIntegrityCheck() {
        return prefs.getBoolean("integrity_check", true);
    }

    public void setIntegrityCheck(boolean check) {
        prefs.edit().putBoolean("integrity_check", check).apply();
    }

    // === 加载器类型 (用于 Modrinth/CF 筛选) ===

    public String getModLoader() {
        return prefs.getString("mod_loader", "fabric");
    }

    public void setModLoader(String loader) {
        prefs.edit().putString("mod_loader", loader).apply();
    }
}
