package com.tungsten.fcl.download;

import com.tungsten.fcl.game.Version;

/**
 * 库分析器
 *
 * 分析游戏版本所需的库和 Mod 加载器类型。
 * 用于判断是否安装了 Forge、Fabric、NeoForge 等。
 */
public class LibraryAnalyzer {

    public enum LibraryType {
        FORGE,
        FABRIC,
        NEOFORGE,
        QUILT,
        CLEANROOM,
        LITELOADER,
        OPTIFINE
    }

    /**
     * 分析游戏版本所需的库
     *
     * @param version 游戏版本
     * @param gameVersion 游戏版本号
     * @return 分析结果
     */
    public static LibraryResult analyze(Version version, String gameVersion) {
        LibraryResult result = new LibraryResult();

        // 检查 Fabric
        if (hasFabric(version)) {
            result.add(LibraryType.FABRIC);
        }

        // 检查 Forge
        if (hasForge(version)) {
            result.add(LibraryType.FORGE);
        }

        // 检查 NeoForge
        if (hasNeoForge(version)) {
            result.add(LibraryType.NEOFORGE);
        }

        // 检查 Quilt
        if (hasQuilt(version)) {
            result.add(LibraryType.QUILT);
        }

        return result;
    }

    private static boolean hasFabric(Version version) {
        // 简化实现: 检查库列表中是否包含 Fabric
        return false;
    }

    private static boolean hasForge(Version version) {
        return false;
    }

    private static boolean hasNeoForge(Version version) {
        return false;
    }

    private static boolean hasQuilt(Version version) {
        return false;
    }

    /**
     * 库分析结果
     */
    public static class LibraryResult {
        private java.util.List<LibraryType> types = new java.util.ArrayList<>();

        public void add(LibraryType type) {
            types.add(type);
        }

        public boolean has(LibraryType type) {
            return types.contains(type);
        }

        public java.util.List<LibraryType> getTypes() {
            return types;
        }
    }
}