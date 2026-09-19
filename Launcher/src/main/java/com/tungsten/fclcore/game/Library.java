package com.tungsten.fclcore.game;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * 库（依赖） — 直接取自 FCL Library
 *
 * 描述一个 Minecraft 依赖，含下载信息、natives、规则等。
 */
public class Library implements Comparable<Library> {

    public static final String DEFAULT_LIBRARY_URL = "https://libraries.minecraft.net/";

    @SerializedName("name")
    private final Artifact artifact;
    private final String url;
    private final LibrariesDownloadInfo downloads;
    private final ExtractRules extract;
    private final Map<String, String> natives;
    private final List<CompatibilityRule> rules;
    private final List<String> checksums;

    @SerializedName(value = "hint", alternate = {"MMC-hint"})
    private final String hint;

    @SerializedName(value = "filename", alternate = {"MMC-filename"})
    private final String fileName;

    public Library(Artifact artifact) {
        this(artifact, null, null);
    }

    public Library(Artifact artifact, String url, LibrariesDownloadInfo downloads) {
        this(artifact, url, downloads, null, null, null, null, null, null);
    }

    public Library(Artifact artifact, String url, LibrariesDownloadInfo downloads,
                   List<String> checksums, ExtractRules extract, Map<String, String> natives,
                   List<CompatibilityRule> rules, String hint, String filename) {
        this.artifact = artifact;
        this.url = url;
        this.downloads = downloads;
        this.extract = extract;
        this.natives = natives;
        this.rules = rules;
        this.checksums = checksums;
        this.hint = hint;
        this.fileName = filename;
    }

    public Artifact getArtifact() { return artifact; }
    public String getGroupId() { return artifact.getGroup(); }
    public String getArtifactId() { return artifact.getName(); }
    public String getName() { return artifact.toString(); }
    public String getVersion() { return artifact.getVersion(); }
    public String getClassifier() { return artifact.getClassifier(); }

    public ExtractRules getExtract() { return extract == null ? ExtractRules.EMPTY : extract; }

    public boolean appliesToCurrentEnvironment() {
        return CompatibilityRule.appliesToCurrentEnvironment(rules);
    }

    public boolean isNative() {
        return natives != null && appliesToCurrentEnvironment();
    }

    protected LibraryDownloadInfo getRawDownloadInfo() {
        if (downloads != null) {
            if (isNative())
                return downloads.getClassifiers().get(getClassifier());
            else
                return downloads.getArtifact();
        }
        return null;
    }

    public String getPath() {
        LibraryDownloadInfo temp = getRawDownloadInfo();
        if (temp != null && temp.getPath() != null)
            return temp.getPath();
        else
            return artifact.setClassifier(getClassifier()).getPath();
    }

    public LibraryDownloadInfo getDownload() {
        LibraryDownloadInfo temp = getRawDownloadInfo();
        String path = getPath();
        return new LibraryDownloadInfo(path,
                temp != null && temp.getUrl() != null && !temp.getUrl().isEmpty()
                        ? temp.getUrl()
                        : (url != null ? url : DEFAULT_LIBRARY_URL) + path,
                temp != null ? temp.getSha1() : null,
                temp != null ? temp.getSize() : 0
        );
    }

    public boolean hasDownloadURL() {
        LibraryDownloadInfo temp = getRawDownloadInfo();
        if (temp != null) return temp.getUrl() != null;
        else return url != null;
    }

    public List<String> getChecksums() { return checksums; }
    public List<CompatibilityRule> getRules() { return rules; }

    public String getHint() { return hint; }
    public String getFileName() { return fileName; }

    /**
     * 当前平台的 natives classifier (如 natives-linux)
     */
    public String getNativeClassifier() {
        if (natives == null) return null;
        return natives.get("linux");
    }

    @Override
    public int compareTo(Library o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public String toString() { return getName(); }
}
