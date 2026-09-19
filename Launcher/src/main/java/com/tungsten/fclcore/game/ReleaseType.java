package com.tungsten.fclcore.game;

/**
 * 发布类型 — 直接取自 FCL ReleaseType
 */
public enum ReleaseType {
    RELEASE("release"),
    SNAPSHOT("snapshot"),
    OLD_BETA("old_beta"),
    OLD_ALPHA("old_alpha"),
    UNKNOWN("unknown");

    private final String id;

    ReleaseType(String id) { this.id = id; }
    public String getId() { return id; }

    public static ReleaseType getById(String id) {
        for (ReleaseType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return UNKNOWN;
    }

    public boolean isReleased() {
        return this == RELEASE;
    }
}
