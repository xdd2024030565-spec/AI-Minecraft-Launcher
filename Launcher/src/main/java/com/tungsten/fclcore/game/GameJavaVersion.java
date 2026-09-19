package com.tungsten.fclcore.game;

/**
 * 游戏 Java 版本需求 — 直接取自 FCL GameJavaVersion
 */
public class GameJavaVersion {

    private final String component;
    private final int majorVersion;

    public GameJavaVersion() {
        this(null, 0);
    }

    public GameJavaVersion(String component, int majorVersion) {
        this.component = component;
        this.majorVersion = majorVersion;
    }

    public String getComponent() { return component; }
    public int getMajorVersion() { return majorVersion; }
}
