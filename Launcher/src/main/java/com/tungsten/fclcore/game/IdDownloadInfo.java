package com.tungsten.fclcore.game;

/**
 * 带 ID 的下载信息 — 直接取自 FCL IdDownloadInfo
 */
public class IdDownloadInfo extends DownloadInfo {

    private final String id;

    public IdDownloadInfo() {
        this("", "");
    }

    public IdDownloadInfo(String id, String url) {
        this(id, url, null);
    }

    public IdDownloadInfo(String id, String url, String sha1) {
        this(id, url, sha1, 0);
    }

    public IdDownloadInfo(String id, String url, String sha1, int size) {
        super(url, sha1, size);
        this.id = id;
    }

    public String getId() { return id; }
}
