package com.tungsten.fclcore.game;

/**
 * 操作系统限制 — 直接取自 FCL OSRestriction
 */
public final class OSRestriction {

    public static final String OS_NAME = "android";

    private final String name;
    private final String version;
    private final String arch;

    public OSRestriction() {
        this(null);
    }

    public OSRestriction(String name) {
        this(name, null);
    }

    public OSRestriction(String name, String version) {
        this(name, version, null);
    }

    public OSRestriction(String name, String version, String arch) {
        this.name = name;
        this.version = version;
        this.arch = arch;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getArch() { return arch; }

    /**
     * 是否允许在当前环境运行 — 仿 FCL
     * Android 下 matches 为 linux，不匹配则为 windows/macos
     */
    public boolean allow() {
        if (name != null) {
            switch (name.toLowerCase()) {
                case "linux":
                    // Android 兼容 linux
                    break;
                case "windows":
                case "osx":
                case "macos":
                    return false;
                default:
                    return false;
            }
        }
        // 架构匹配 (ARM 设备)
        if (arch != null) {
            String currentArch = System.getProperty("os.arch", "aarch64");
            if (arch.equalsIgnoreCase("x86") && !currentArch.contains("x86")) return false;
            if (arch.equalsIgnoreCase("arm64") && !currentArch.contains("aarch64")) return false;
        }
        return true;
    }
}
