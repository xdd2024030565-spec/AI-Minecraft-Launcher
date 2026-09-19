package com.tungsten.fclcore.game;

import com.google.gson.JsonParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 游戏版本 — 直接取自 FCL Version
 *
 * 描述一个 Minecraft 版本，支持继承(inheritsFrom)、补丁(patches)等。
 */
public class Version implements Comparable<Version> {

    private String id;
    private final String version;
    private final Integer priority;
    private final String minecraftArguments;
    private final Arguments arguments;
    private final String mainClass;
    private final String inheritsFrom;
    private final String jar;
    private final AssetIndexInfo assetIndex;
    private final String assets;
    private final Integer complianceLevel;
    private final GameJavaVersion javaVersion;
    private final List<Library> libraries;
    private final List<CompatibilityRule> compatibilityRules;
    private final Map<String, DownloadInfo> downloads;
    private final ReleaseType type;
    private final Instant time;
    private final Instant releaseTime;
    private final Integer minimumLauncherVersion;
    private final Boolean hidden;
    private final List<Version> patches;

    private transient final boolean resolved;

    public Version(String id) {
        this(false, id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true, null);
    }

    /**
     * 补丁构造器
     */
    public Version(String id, String version, int priority, Arguments arguments, String mainClass, List<Library> libraries) {
        this(false, id, version, priority, null, arguments, mainClass, null, null, null, null, null, null, libraries, null, null, null, null, null, null, null, null, null, null);
    }

    public Version(boolean resolved, String id, String version, Integer priority, String minecraftArguments,
                   Arguments arguments, String mainClass, String inheritsFrom, String jar, AssetIndexInfo assetIndex,
                   String assets, Integer complianceLevel, GameJavaVersion javaVersion, List<Library> libraries,
                   List<CompatibilityRule> compatibilityRules, Map<String, DownloadInfo> downloads,
                   ReleaseType type, Instant time, Instant releaseTime, Integer minimumLauncherVersion,
                   Boolean hidden, Boolean root, List<Version> patches) {
        this.resolved = resolved;
        this.id = id;
        this.version = version;
        this.priority = priority;
        this.minecraftArguments = minecraftArguments;
        this.arguments = arguments;
        this.mainClass = mainClass;
        this.inheritsFrom = inheritsFrom;
        this.jar = jar;
        this.assetIndex = assetIndex;
        this.assets = assets;
        this.complianceLevel = complianceLevel;
        this.javaVersion = javaVersion;
        this.libraries = copyList(libraries);
        this.compatibilityRules = copyList(compatibilityRules);
        this.downloads = downloads;
        this.type = type;
        this.time = time;
        this.releaseTime = releaseTime;
        this.minimumLauncherVersion = minimumLauncherVersion;
        this.hidden = hidden;
        this.patches = copyList(patches);
    }

    private static <T> List<T> copyList(List<T> list) {
        return list == null ? null : new ArrayList<>(list);
    }

    public String getId() { return id; }
    public Version setId(String id) { this.id = id; return this; }

    public String getVersion() { return version; }
    public Integer getPriority() { return priority; }
    public String getMinecraftArguments() { return minecraftArguments; }
    public Arguments getArguments() { return arguments; }
    public String getMainClass() { return mainClass; }
    public String getInheritsFrom() { return inheritsFrom; }
    public String getJar() { return jar; }
    public AssetIndexInfo getAssetIndex() { return assetIndex; }
    public String getAssets() { return assets; }
    public Integer getComplianceLevel() { return complianceLevel; }
    public GameJavaVersion getJavaVersion() { return javaVersion; }
    public List<Library> getLibraries() { return libraries == null ? Collections.emptyList() : Collections.unmodifiableList(libraries); }
    public List<CompatibilityRule> getCompatibilityRules() { return compatibilityRules == null ? Collections.emptyList() : Collections.unmodifiableList(compatibilityRules); }
    public Map<String, DownloadInfo> getDownloads() { return downloads; }
    public DownloadInfo getDownloadInfo() { return downloads == null ? null : downloads.get("client"); }
    public ReleaseType getType() { return type; }
    public Instant getTime() { return time; }
    public Instant getReleaseTime() { return releaseTime; }
    public Integer getMinimumLauncherVersion() { return minimumLauncherVersion; }
    public List<Version> getPatches() { return patches == null ? Collections.emptyList() : Collections.unmodifiableList(patches); }
    public boolean isResolved() { return resolved; }
    public boolean isHidden() { return hidden != null && hidden; }

    /**
     * 解析继承链 — 仿 FCL Version.resolve
     */
    public Version resolve(VersionProvider provider) throws VersionNotFoundException {
        if (isResolved()) return this;
        Version resolved = new Version(
                true, id, version, priority, minecraftArguments, arguments, mainClass,
                inheritsFrom, jar, assetIndex, assets, complianceLevel, javaVersion,
                libraries, compatibilityRules, downloads, type, time, releaseTime,
                minimumLauncherVersion, hidden, true, null
        );
        resolved.mergeFrom(this);
        if (inheritsFrom != null) {
            Version parent = provider.getVersion(inheritsFrom);
            resolved = parent.resolve(provider).merge(resolved);
        }
        return resolved;
    }

    private void mergeFrom(Version version) {
        // 拷贝字段（实际由 merge 处理）
    }

    /**
     * 合并另一个版本（other 优先级更高）
     */
    public Version merge(Version other) {
        return new Version(
                true, id, other.version != null ? other.version : version,
                other.priority != null ? other.priority : priority,
                other.minecraftArguments != null ? other.minecraftArguments : minecraftArguments,
                Arguments.merge(arguments, other.arguments),
                other.mainClass != null ? other.mainClass : mainClass,
                inheritsFrom,
                other.jar != null ? other.jar : jar,
                other.assetIndex != null ? other.assetIndex : assetIndex,
                other.assets != null ? other.assets : assets,
                other.complianceLevel != null ? other.complianceLevel : complianceLevel,
                other.javaVersion != null ? other.javaVersion : javaVersion,
                mergeLibs(other.libraries),
                other.compatibilityRules != null ? other.compatibilityRules : compatibilityRules,
                other.downloads != null ? other.downloads : downloads,
                other.type != null ? other.type : type,
                other.time != null ? other.time : time,
                other.releaseTime != null ? other.releaseTime : releaseTime,
                other.minimumLauncherVersion != null ? other.minimumLauncherVersion : minimumLauncherVersion,
                hidden, other.root != null ? other.root : (resolved),
                patches
        );
    }

    private List<Library> mergeLibs(List<Library> other) {
        if (libraries == null) return other;
        if (other == null) return libraries;
        List<Library> result = new ArrayList<>(libraries);
        result.addAll(other);
        return result;
    }

    private Boolean root;

    @Override
    public int compareTo(Version o) {
        return getId().compareTo(o.getId());
    }

    @Override
    public String toString() { return id; }
}
