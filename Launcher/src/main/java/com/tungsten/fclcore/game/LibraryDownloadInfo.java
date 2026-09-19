package com.tungsten.fclcore.game;

import com.google.gson.annotations.SerializedName;

/**
 * 库下载信息 — 直接取自 FCL LibraryDownloadInfo
 */
public class LibraryDownloadInfo extends DownloadInfo {

    @SerializedName("path")
    private final String path;

    public LibraryDownloadInfo() {
        this(null);
    }

    public LibraryDownloadInfo(String path) {
        this(path, "");
    }

    public LibraryDownloadInfo(String path, String url) {
        this(path, url, null);
    }

    public LibraryDownloadInfo(String path, String url, String sha1) {
        this(path, url, sha1, 0);
    }

    public LibraryDownloadInfo(String path, String url, String sha1, int size) {
        super(url, sha1, size);
        this.path = path;
    }

    public String getPath() { return path; }
}
