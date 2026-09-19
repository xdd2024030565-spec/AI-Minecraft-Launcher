package com.tungsten.fclcore.game;

/**
 * 资源对象 — 直接取自 FCL AssetObject
 */
public final class AssetObject {

    private final String hash;
    private final long size;

    public AssetObject() {
        this("", 0);
    }

    public AssetObject(String hash, long size) {
        this.hash = hash;
        this.size = size;
    }

    public String getHash() { return hash; }
    public long getSize() { return size; }

    /**
     * 相对位置: aa/aabbcc...
     */
    public String getLocation() {
        return hash.substring(0, 2) + "/" + hash;
    }
}
