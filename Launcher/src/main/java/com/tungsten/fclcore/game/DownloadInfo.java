package com.tungsten.fclcore.game;

import com.google.gson.annotations.SerializedName;

/**
 * 下载信息 — 直接取自 FCL DownloadInfo
 */
public class DownloadInfo {

    @SerializedName("url")
    private final String url;
    @SerializedName("sha1")
    private final String sha1;
    @SerializedName("size")
    private final int size;

    public DownloadInfo() {
        this("");
    }

    public DownloadInfo(String url) {
        this(url, null);
    }

    public DownloadInfo(String url, String sha1) {
        this(url, sha1, 0);
    }

    public DownloadInfo(String url, String sha1, int size) {
        this.url = url;
        this.sha1 = sha1;
        this.size = size;
    }

    public String getUrl() { return url; }
    public String getSha1() { return "invalid".equals(sha1) ? null : sha1; }
    public int getSize() { return size; }

    public boolean isPresent() {
        return url != null && !url.isEmpty();
    }
}
