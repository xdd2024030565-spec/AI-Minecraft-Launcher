package com.tungsten.fclcore.game;

import java.util.Collections;
import java.util.Map;

/**
 * 库下载信息容器 — 直接取自 FCL LibrariesDownloadInfo
 */
public class LibrariesDownloadInfo {

    private final LibraryDownloadInfo artifact;
    private final Map<String, LibraryDownloadInfo> classifiers;

    public LibrariesDownloadInfo() {
        this(null, Collections.emptyMap());
    }

    public LibrariesDownloadInfo(LibraryDownloadInfo artifact, Map<String, LibraryDownloadInfo> classifiers) {
        this.artifact = artifact;
        this.classifiers = classifiers == null ? Collections.emptyMap() : classifiers;
    }

    public LibraryDownloadInfo getArtifact() { return artifact; }
    public Map<String, LibraryDownloadInfo> getClassifiers() { return classifiers; }
}
