package com.tungsten.fcl.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tungsten.fcl.FCLRepository;
import com.tungsten.fcl.setting.LauncherSettings;

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

/**
 * 游戏版本下载器 — 仿 FCL GameInstallTask + DefaultGameBuilder
 *
 * 完整下载流程 (与 FCL 一致):
 * 1. 下载版本清单 (version_manifest_v2.json)
 * 2. 下载版本 JSON
 * 3. 下载 client.jar
 * 4. 解析并下载 libraries (库文件)
 * 5. 下载 asset index (资源索引)
 * 6. 下载 asset objects (资源文件)
 *
 * 下载源从 LauncherSettings 读取。
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
     * 使用 LauncherSettings 配置创建下载器
     */
    public static GameDownloader create(FCLRepository repository, LauncherSettings settings) {
        return new GameDownloader(repository, settings.getDownloadProvider());
    }

    public interface DownloadCallback {
        void onProgress(String stage, int current, int total, String message);
        void onComplete(String versionId);
        void onError(String message, Exception e);
    }

    public String downloadVersion(String versionId, DownloadCallback callback) throws IOException {
        if (callback != null) callback.onProgress("manifest", 0, 6, "获取版本清单...");
        JsonObject manifest = downloadVersionManifest();

        if (callback != null) callback.onProgress("version_json", 1, 6, "下载版本信息: " + versionId);
        String versionUrl = findVersionUrl(manifest, versionId);
        if (versionUrl == null) {
            throw new IOException("未找到版本: " + versionId);
        }

        File versionDir = repository.getVersionDir(versionId);
        versionDir.mkdirs();
        File versionJsonFile = repository.getVersionJson(versionId);
        downloadFileWithCandidates(versionUrl, versionJsonFile);

        JsonObject versionJson = JsonParser.parseString(
                new String(Files.readAllBytes(versionJsonFile.toPath()), "UTF-8")
        ).getAsJsonObject();

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

        if (callback != null) callback.onProgress("libraries", 3, 6, "下载库文件...");
        downloadLibraries(versionJson, callback);

        if (callback != null) callback.onProgress("assets", 4, 6, "下载资源索引...");
        downloadAssets(versionJson, callback);

        if (callback != null) {
            callback.onProgress("done", 6, 6, "下载完成");
            callback.onComplete(versionId);
        }
        return versionId;
    }

    public String downloadVersion(String versionId) throws IOException {
        return downloadVersion(versionId, null);
    }

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
            for (JsonElement elem : manifest.getAsJsonArray("versions")) {
                JsonObject v = elem.getAsJsonObject();
                if (versionId.equals(v.get("id").getAsString())) {
                    return v.get("url").getAsString();
                }
            }
        }
        return null;
    }

    private void downloadLibraries(JsonObject versionJson, DownloadCallback callback) {
        if (!versionJson.has("libraries")) return;
        JsonArray libraries = versionJson.getAsJsonArray("libraries");
        int total = libraries.size();
        int done = 0;
        for (JsonElement elem : libraries) {
            try {
                JsonObject lib = elem.getAsJsonObject();
                if (lib.has("downloads")) {
                    JsonObject downloads = lib.getAsJsonObject("downloads");
                    if (downloads.has("artifact")) {
                        downloadLibraryArtifact(downloads.getAsJsonObject("artifact"));
                    }
                    if (downloads.has("classifiers")) {
                        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                        for (String key : classifiers.keySet()) {
                            downloadLibraryArtifact(classifiers.getAsJsonObject(key));
                        }
                    }
                } else if (lib.has("url")) {
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
            done++;
            if (callback != null && done % 10 == 0) {
                callback.onProgress("libraries", 3, 6, "下载库文件: " + done + "/" + total);
            }
        }
    }

    private void downloadLibraryArtifact(JsonObject artifact) throws IOException {
        String url = artifact.get("url").getAsString();
        String path = artifact.has("path") ? artifact.get("path").getAsString() : "";
        if (!path.isEmpty()) {
            File destFile = new File(repository.getLibrariesDir(), path);
            destFile.getParentFile().mkdirs();
            if (!destFile.exists()) {
                downloadFileWithCandidates(url, destFile);
            }
        }
    }

    private void downloadAssets(JsonObject versionJson, DownloadCallback callback) {
        try {
            if (!versionJson.has("assetIndex")) return;
            JsonObject assetIndexInfo = versionJson.getAsJsonObject("assetIndex");
            String indexId = assetIndexInfo.get("id").getAsString();
            String indexUrl = assetIndexInfo.get("url").getAsString();

            File indexFile = repository.getAssetIndexFile(indexId);
            indexFile.getParentFile().mkdirs();
            downloadFileWithCandidates(indexUrl, indexFile);

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
                        e.printStackTrace();
                    }
                }
                int done = current.incrementAndGet();
                if (callback != null && done % 50 == 0) {
                    callback.onProgress("assets", 4, 6, "下载资源文件: " + done + "/" + total);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                return;
            } catch (IOException e) {
                lastEx = e;
            }
        }
        throw lastEx != null ? lastEx : new IOException("所有候选源均失败");
    }

    private String mavenToPath(String coordinate) {
        String[] parts = coordinate.split(":");
        if (parts.length < 3) return "";
        String group = parts[0].replace(".", "/");
        String artifact = parts[1];
        String version = parts[2];
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
    }

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
