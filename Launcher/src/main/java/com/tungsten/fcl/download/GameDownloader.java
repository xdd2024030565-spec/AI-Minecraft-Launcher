package com.tungsten.fcl.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tungsten.fcl.FCLRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 游戏版本下载器 — 仿 FCL GameInstallTask + DefaultGameBuilder
 *
 * 完整下载流程:
 * 1. 下载版本清单 (version_manifest_v2.json)
 * 2. 下载版本 JSON
 * 3. 下载 client.jar
 * 4. 解析并下载 libraries (库文件)
 * 5. 下载 asset index (资源索引)
 * 6. 下载 asset objects (资源文件)
 *
 * 支持下载进度回调、多源候选。
 */
public class GameDownloader {

    private final FCLRepository repository;
    private final DownloadProvider downloadProvider;

    public GameDownloader(FCLRepository repository, DownloadProvider downloadProvider) {
        this.repository = repository;
        this.downloadProvider = downloadProvider;
    }

    public GameDownloader(FCLRepository repository) {
        this(repository, new AutoDownloadProvider());
    }

    /**
     * 下载进度回调接口
     */
    public interface DownloadCallback {
        void onProgress(String stage, int current, int total, String message);
        void onComplete(String versionId);
        void onError(String message, Exception e);
    }

    /**
     * 下载指定游戏版本 (完整下载)
     *
     * @param versionId 版本 ID (如 "1.20.1")
     * @param callback 下载进度回调
     * @return 下载完成的版本 ID
     */
    public String downloadVersion(String versionId, DownloadCallback callback) throws IOException {
        // 1. 下载版本清单
        if (callback != null) callback.onProgress("manifest", 0, 6, "获取版本清单...");
        JsonObject manifest = downloadVersionManifest();

        // 2. 查找版本 URL
        if (callback != null) callback.onProgress("version_json", 1, 6, "下载版本信息: " + versionId);
        String versionUrl = findVersionUrl(manifest, versionId);
        if (versionUrl == null) {
            throw new IOException("未找到版本: " + versionId);
        }

        // 3. 下载版本 JSON
        File versionDir = repository.getVersionDir(versionId);
        versionDir.mkdirs();
        File versionJsonFile = repository.getVersionJson(versionId);
        downloadFileWithCandidates(versionUrl, versionJsonFile);

        JsonObject versionJson = JsonParser.parseString(
                new String(Files.readAllBytes(versionJsonFile.toPath()), "UTF-8")
        ).getAsJsonObject();

        // 4. 下载 client.jar
        if (callback != null) callback.onProgress("client_jar", 2, 6, "下载客户端 JAR...");
        if (versionJson.has("downloads")) {
            JsonObject downloads = versionJson.getAsJsonObject("downloads");
            if (downloads.has("client")) {
                JsonObject client = downloads.getAsJsonObject("client");
                String clientUrl = client.get("url").getAsString();
                File clientJar = repository.getVersionJar(versionId);
                downloadFileWithCandidates(clientUrl, clientJar);
            }
        }

        // 5. 下载 libraries
        if (callback != null) callback.onProgress("libraries", 3, 6, "下载库文件...");
        downloadLibraries(versionJson);

        // 6. 下载 asset 索引和资源文件
        if (callback != null) callback.onProgress("assets", 4, 6, "下载资源索引...");
        downloadAssets(versionJson, callback);

        // 7. 完成
        if (callback != null) {
            callback.onProgress("done", 6, 6, "下载完成");
            callback.onComplete(versionId);
        }

        return versionId;
    }

    /**
     * 下载版本清单 (不传回调的简化版)
     */
    public String downloadVersion(String versionId) throws IOException {
        return downloadVersion(versionId, null);
    }

    /**
     * 获取远程版本清单
     */
    public List<RemoteVersionInfo> getRemoteVersions() throws IOException {
        JsonObject manifest = downloadVersionManifest();
        List<RemoteVersionInfo> versions = new ArrayList<>();
        if (manifest.has("versions")) {
            JsonArray arr = manifest.getAsJsonArray("versions");
            for (JsonElement elem : arr) {
                JsonObject v = elem.getAsJsonObject();
                String id = v.get("id").getAsString();
                String type = v.has("type") ? v.get("type").getAsString() : "unknown";
                String url = v.has("url") ? v.get("url").getAsString() : "";
                String releaseTime = v.has("releaseTime") ? v.get("releaseTime").getAsString() : "";
                versions.add(new RemoteVersionInfo(id, type, url, releaseTime));
            }
        }
        return versions;
    }

    /**
     * 获取最新版本信息
     */
    public RemoteVersionInfo getLatestRelease() throws IOException {
        JsonObject manifest = downloadVersionManifest();
        if (manifest.has("latest")) {
            JsonObject latest = manifest.getAsJsonObject("latest");
            String releaseId = latest.has("release") ? latest.get("release").getAsString() : "";
            List<RemoteVersionInfo> versions = getRemoteVersions();
            for (RemoteVersionInfo v : versions) {
                if (v.getId().equals(releaseId)) {
                    return v;
                }
            }
        }
        return null;
    }

    // === 内部方法 ===

    private JsonObject downloadVersionManifest() throws IOException {
        List<URL> urls = downloadProvider.getVersionListURLs();
        IOException lastEx = null;
        for (URL url : urls) {
            try {
                String content = downloadText(url.toString());
                return JsonParser.parseString(content).getAsJsonObject();
            } catch (IOException e) {
                lastEx = e;
            }
        }
        throw lastEx != null ? lastEx : new IOException("无法获取版本清单");
    }

    private String findVersionUrl(JsonObject manifest, String versionId) {
        if (manifest.has("versions")) {
            JsonArray versions = manifest.getAsJsonArray("versions");
            for (JsonElement elem : versions) {
                JsonObject v = elem.getAsJsonObject();
                if (versionId.equals(v.get("id").getAsString())) {
                    return v.get("url").getAsString();
                }
            }
        }
        return null;
    }

    /**
     * 下载 libraries — 仿 FCL GameLibrariesTask
     */
    private void downloadLibraries(JsonObject versionJson) {
        if (!versionJson.has("libraries")) return;
        JsonArray libraries = versionJson.getAsJsonArray("libraries");
        for (JsonElement elem : libraries) {
            try {
                JsonObject lib = elem.getAsJsonObject();
                // 解析库的下载信息
                if (lib.has("downloads")) {
                    JsonObject downloads = lib.getAsJsonObject("downloads");
                    if (downloads.has("artifact")) {
                        JsonObject artifact = downloads.getAsJsonObject("artifact");
                        String url = artifact.get("url").getAsString();
                        String path = artifact.has("path") ? artifact.get("path").getAsString() : "";
                        if (!path.isEmpty()) {
                            File destFile = new File(repository.getLibrariesDir(), path);
                            destFile.getParentFile().mkdirs();
                            downloadFileWithCandidates(url, destFile);
                        }
                    }
                    // classifiers (natives)
                    if (downloads.has("classifiers")) {
                        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                        for (String key : classifiers.keySet()) {
                            JsonObject classifier = classifiers.getAsJsonObject(key);
                            String url = classifier.get("url").getAsString();
                            String path = classifier.has("path") ? classifier.get("path").getAsString() : "";
                            if (!path.isEmpty()) {
                                File destFile = new File(repository.getLibrariesDir(), path);
                                destFile.getParentFile().mkdirs();
                                downloadFileWithCandidates(url, destFile);
                            }
                        }
                    }
                } else if (lib.has("url")) {
                    // 简单 URL 格式 (旧版本)
                    String url = lib.get("url").getAsString();
                    String name = lib.has("name") ? lib.get("name").getAsString() : "";
                    String path = mavenToPath(name);
                    if (!path.isEmpty()) {
                        File destFile = new File(repository.getLibrariesDir(), path);
                        destFile.getParentFile().mkdirs();
                        downloadFileWithCandidates(url + "/" + path, destFile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 下载 assets — 仿 FCL GameAssetDownloadTask + GameAssetIndexDownloadTask
     */
    private void downloadAssets(JsonObject versionJson, DownloadCallback callback) {
        try {
            if (!versionJson.has("assetIndex")) return;
            JsonObject assetIndexInfo = versionJson.getAsJsonObject("assetIndex");
            String indexId = assetIndexInfo.get("id").getAsString();
            String indexUrl = assetIndexInfo.get("url").getAsString();

            // 下载资源索引
            File indexFile = repository.getAssetIndexFile(indexId);
            downloadFileWithCandidates(indexUrl, indexFile);

            // 解析资源索引并下载资源文件
            JsonObject indexJson = JsonParser.parseString(
                    new String(Files.readAllBytes(indexFile.toPath()), "UTF-8")
            ).getAsJsonObject();

            if (!indexJson.has("objects")) return;
            JsonObject objects = indexJson.getAsJsonObject("objects");

            int total = objects.size();
            AtomicInteger current = new AtomicInteger(0);

            for (String name : objects.keySet()) {
                JsonObject obj = objects.getAsJsonObject(name);
                String hash = obj.get("hash").getAsString();
                int size = obj.get("size").getAsInt();

                String location = hash.substring(0, 2) + "/" + hash.substring(0, 4) + "/" + hash;
                File destFile = repository.getAssetObject(hash);
                destFile.getParentFile().mkdirs();

                if (!destFile.exists() || destFile.length() != size) {
                    try {
                        List<URL> candidates = downloadProvider.getAssetObjectCandidates(location);
                        downloadFromCandidates(candidates, destFile);
                    } catch (Exception e) {
                        // 单个资源文件失败不中断
                        e.printStackTrace();
                    }
                }

                int done = current.incrementAndGet();
                if (callback != null && done % 50 == 0) {
                    callback.onProgress("assets", 4, 6, 
                            "下载资源文件: " + done + "/" + total);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === 下载工具方法 ===

    private String downloadText(String urlStr) throws IOException {
        List<URL> candidates = downloadProvider.injectURLWithCandidates(urlStr);
        IOException lastEx = null;
        for (URL url : candidates) {
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                try (InputStream is = conn.getInputStream()) {
                    return new String(is.readAllBytes(), "UTF-8");
                } finally {
                    conn.disconnect();
                }
            } catch (IOException e) {
                lastEx = e;
            }
        }
        // 如果候选源失败，尝试原始 URL
        if (lastEx != null) {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            try (InputStream is = conn.getInputStream()) {
                return new String(is.readAllBytes(), "UTF-8");
            } finally {
                conn.disconnect();
            }
        }
        throw lastEx != null ? lastEx : new IOException("下载失败: " + urlStr);
    }

    private void downloadFileWithCandidates(String urlStr, File dest) throws IOException {
        List<URL> candidates = downloadProvider.injectURLWithCandidates(urlStr);
        downloadFromCandidates(candidates, dest);
    }

    private void downloadFromCandidates(List<URL> candidates, File dest) throws IOException {
        IOException lastEx = null;
        for (URL url : candidates) {
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setInstanceFollowRedirects(true);
                try (InputStream is = conn.getInputStream()) {
                    Files.copy(is, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    conn.disconnect();
                }
                return; // 成功
            } catch (IOException e) {
                lastEx = e;
            }
        }
        throw lastEx != null ? lastEx : new IOException("所有候选源均失败");
    }

    /**
     * Maven 坐标转路径 — group:artifact:version → group/artifact/version/artifact-version.jar
     */
    private String mavenToPath(String coordinate) {
        String[] parts = coordinate.split(":");
        if (parts.length < 3) return "";
        String group = parts[0].replace(".", "/");
        String artifact = parts[1];
        String version = parts[2];
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
    }

    /**
     * 远程版本信息
     */
    public static class RemoteVersionInfo {
        private final String id;
        private final String type;
        private final String url;
        private final String releaseTime;

        public RemoteVersionInfo(String id, String type, String url, String releaseTime) {
            this.id = id;
            this.type = type;
            this.url = url;
            this.releaseTime = releaseTime;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public String getUrl() { return url; }
        public String getReleaseTime() { return releaseTime; }

        public boolean isRelease() { return "release".equals(type); }
        public boolean isSnapshot() { return "snapshot".equals(type); }
    }
}
