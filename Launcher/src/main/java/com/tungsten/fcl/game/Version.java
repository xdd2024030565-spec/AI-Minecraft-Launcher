package com.tungsten.fcl.game;

import java.util.UUID;

/**
 * 游戏版本
 *
 * 表示一个 Minecraft 游戏版本，包含版本 ID、继承关系、资源索引等信息。
 */
public class Version {

    private String id;
    private String inheritsFrom;
    private String type;
    private String assetIndex;
    private String mainClass;
    private String minecraftArguments;
    private UUID uuid;
    private long releaseTime;

    public Version() {
    }

    public Version(String id, String inheritsFrom, String type) {
        this.id = id;
        this.inheritsFrom = inheritsFrom;
        this.type = type;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInheritsFrom() { return inheritsFrom; }
    public void setInheritsFrom(String inheritsFrom) { this.inheritsFrom = inheritsFrom; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAssetIndex() { return assetIndex; }
    public void setAssetIndex(String assetIndex) { this.assetIndex = assetIndex; }

    public String getMainClass() { return mainClass; }
    public void setMainClass(String mainClass) { this.mainClass = mainClass; }

    public String getMinecraftArguments() { return minecraftArguments; }
    public void setMinecraftArguments(String minecraftArguments) { this.minecraftArguments = minecraftArguments; }

    public UUID getUUID() { return uuid; }
    public void setUUID(UUID uuid) { this.uuid = uuid; }

    public long getReleaseTime() { return releaseTime; }
    public void setReleaseTime(long releaseTime) { this.releaseTime = releaseTime; }
}