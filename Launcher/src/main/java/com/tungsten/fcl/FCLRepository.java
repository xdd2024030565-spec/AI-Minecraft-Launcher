package com.tungsten.fcl;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/**
 * FCL Repository — 游戏文件仓库
 *
 * 仿 FCL FCLGameRepository / FCLRepository，管理游戏版本、库、资源、Mod等文件。
 * 支持自定义游戏目录 — 可选择本地目录(如与 FCL 共用同一游戏目录)。
 */
public class FCLRepository {

    private static FCLRepository instance;
    private final Context context;

    private File rootDir;
    private File versionsDir;
    private File librariesDir;
    private File assetsDir;
    private File assetObjectsDir;
    private File assetIndexesDir;
    private File modDir;
    private File resourceDir;
    private File logDir;
    private File modpacksDir;
    private File shaderPacksDir;
    private File resourcePacksDir;

    private FCLRepository(Context context) {
        this.context = context.getApplicationContext();
        // 从 SharedPreferences 读取自定义游戏目录
        SharedPreferences prefs = context.getSharedPreferences("fcl_config", Context.MODE_PRIVATE);
        String customPath = prefs.getString("game_directory", "");

        if (customPath != null && !customPath.isEmpty()) {
            rootDir = new File(customPath);
        } else {
            rootDir = new File(context.getFilesDir(), "FCL");
        }
        initDirs();
    }

    private void initDirs() {
        versionsDir = new File(rootDir, "versions");
        librariesDir = new File(rootDir, "libraries");
        assetsDir = new File(rootDir, "assets");
        assetObjectsDir = new File(assetsDir, "objects");
        assetIndexesDir = new File(assetsDir, "indexes");
        modDir = new File(rootDir, "mods");
        resourceDir = new File(rootDir, "resources");
        logDir = new File(rootDir, "logs");
        modpacksDir = new File(rootDir, "modpacks");
        shaderPacksDir = new File(rootDir, "shaderpacks");
        resourcePacksDir = new File(rootDir, "resourcepacks");

        rootDir.mkdirs();
        versionsDir.mkdirs();
        librariesDir.mkdirs();
        assetsDir.mkdirs();
        assetObjectsDir.mkdirs();
        assetIndexesDir.mkdirs();
        modDir.mkdirs();
        resourceDir.mkdirs();
        logDir.mkdirs();
        modpacksDir.mkdirs();
        shaderPacksDir.mkdirs();
        resourcePacksDir.mkdirs();
    }

    public static FCLRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (FCLRepository.class) {
                if (instance == null) {
                    instance = new FCLRepository(context);
                }
            }
        }
        return instance;
    }

    /**
     * 切换游戏目录 — 仿 FCL Profile.gameDir setter
     */
    public void changeDirectory(File newRootDir) {
        if (newRootDir == null || !newRootDir.exists()) {
            return;
        }
        rootDir = newRootDir;
        initDirs();
        // 持久化
        SharedPreferences prefs = context.getSharedPreferences("fcl_config", Context.MODE_PRIVATE);
        prefs.edit().putString("game_directory", newRootDir.getAbsolutePath()).apply();
    }

    /**
     * 获取当前游戏目录路径
     */
    public String getGameDirectoryPath() {
        return rootDir.getAbsolutePath();
    }

    /**
     * 获取 FCL 默认游戏目录路径 (用于共享)
     */
    public static String getFCLDefaultPath(Context context) {
        // FCL 的默认路径
        File fclShared = new File(context.getExternalFilesDir(null), "games");
        if (!fclShared.exists()) {
            fclShared = new File("/sdcard/Android/data/com.fcl.android/files/games");
        }
        return fclShared.getAbsolutePath();
    }

    // === 路径获取方法 ===

    public File getRootDir() { return rootDir; }
    public File getVersionsDir() { return versionsDir; }
    public File getLibrariesDir() { return librariesDir; }
    public File getAssetsDir() { return assetsDir; }
    public File getAssetObjectsDir() { return assetObjectsDir; }
    public File getAssetIndexesDir() { return assetIndexesDir; }
    public File getModDir() { return modDir; }
    public File getResourceDir() { return resourceDir; }
    public File getLogDir() { return logDir; }
    public File getModpacksDir() { return modpacksDir; }
    public File getShaderPacksDir() { return shaderPacksDir; }
    public File getResourcePacksDir() { return resourcePacksDir; }

    public File getVersionDir(String versionId) {
        return new File(versionsDir, versionId);
    }

    public File getVersionJson(String versionId) {
        return new File(getVersionDir(versionId), versionId + ".json");
    }

    public File getVersionJar(String versionId) {
        return new File(getVersionDir(versionId), versionId + ".jar");
    }

    /**
     * 获取资源索引文件路径
     */
    public File getAssetIndexFile(String assetIndexId) {
        return new File(assetIndexesDir, assetIndexId + ".json");
    }

    /**
     * 获取资源对象文件路径
     * @param hash 资源 hash
     * @return 如 assets/objects/aa/bb/aabb....
     */
    public File getAssetObject(String hash) {
        String prefix = hash.substring(0, 2);
        String subPrefix = hash.substring(0, 4);
        return new File(assetObjectsDir, prefix + "/" + subPrefix + "/" + hash);
    }

    /**
     * 获取库文件路径 — 仿 FCL GameRepository.getLibraryFile
     * 将 Maven 坐标转换为路径
     */
    public File getLibraryFile(String group, String artifact, String version, String classifier, String ext) {
        String path = group.replace(".", "/") + "/" + artifact + "/" + version + "/";
        if (classifier != null && !classifier.isEmpty()) {
            path += artifact + "-" + version + "-" + classifier + "." + (ext != null ? ext : "jar");
        } else {
            path += artifact + "-" + version + "." + (ext != null ? ext : "jar");
        }
        return new File(librariesDir, path);
    }

    /**
     * 获取版本对应的 mods 目录
     */
    public File getVersionModsDir(String versionId) {
        return new File(getVersionDir(versionId), "mods");
    }

    /**
     * 获取版本对应的 saves 目录
     */
    public File getVersionSavesDir(String versionId) {
        return new File(getVersionDir(versionId), "saves");
    }

    /**
     * 获取版本对应的 options.txt
     */
    public File getVersionOptionsFile(String versionId) {
        return new File(getVersionDir(versionId), "options.txt");
    }
}
