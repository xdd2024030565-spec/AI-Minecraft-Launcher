package com.tungsten.fcl.game;

/**
 * 游戏版本信息
 */
public class GameVersion {
    private String id;
    private String type;
    private String releaseTime;
    private String url;
    private String sha1;
    private long size;

    public GameVersion(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getReleaseTime() { return releaseTime; }
    public void setReleaseTime(String releaseTime) { this.releaseTime = releaseTime; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSha1() { return sha1; }
    public void setSha1(String sha1) { this.sha1 = sha1; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    @Override
    public String toString() {
        return id + " (" + type + ")";
    }
}
