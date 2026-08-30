package com.tungsten.fcl;

import android.content.Context;

import java.io.File;

/**
 * FCL Repository
 *
 * 管理游戏版本、Mod、资源文件等。
 * 提供统一的文件路径和数据访问接口。
 */
public class FCLRepository {

    private static FCLRepository instance;
    private final Context context;
    private final File rootDir;
    private final File versionsDir;
    private final File librariesDir;
    private final File modDir;
    private final File resourceDir;

    private FCLRepository(Context context) {
        this.context = context.getApplicationContext();

        // 设置基础目录
        rootDir = new File(context.getFilesDir(), "FCL");
        versionsDir = new File(rootDir, "versions");
        librariesDir = new File(rootDir, "libraries");
        modDir = new File(rootDir, "mods");
        resourceDir = new File(rootDir, "resources");

        // 创建目录
        rootDir.mkdirs();
        versionsDir.mkdirs();
        librariesDir.mkdirs();
        modDir.mkdirs();
        resourceDir.mkdirs();
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

    public File getRootDir() { return rootDir; }
    public File getVersionsDir() { return versionsDir; }
    public File getLibrariesDir() { return librariesDir; }
    public File getModDir() { return modDir; }
    public File getResourceDir() { return resourceDir; }

    public File getVersionDir(String versionId) {
        return new File(versionsDir, versionId);
    }

    public File getVersionJar(String versionId) {
        return new File(getVersionDir(versionId), versionId + ".jar");
    }
}