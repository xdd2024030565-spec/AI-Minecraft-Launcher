package com.tungsten.fcl.download;

import com.google.gson.JsonArray;
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

/**
 * 游戏版本下载器
 *
 * 从 Mojang 官方源下载 Minecraft Java 版:
 * 1. 下载版本清单 (version_manifest_v2.json)
 * 2. 根据版本 ID 找到对应的版本 URL
 * 3. 下载版本 JSON
 * 4. 解析并下载 client JAR
 *
 * TODO: 下载 libraries / assets / 索引文件
 */
public class GameDownloader {

    private static final String VERSION_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final FCLRepository repository;

    public GameDownloader(FCLRepository repository) {
        this.repository = repository;
    }

    /**
     * 下载指定游戏版本
     *
     * @param versionId 版本 ID (如 "1.20.1")
     * @return 下载完成的版本 ID
     */
    public String downloadVersion(String versionId) throws IOException {
        // 1. 解析版本清单，找到版本 URL
        File manifestFile = File.createTempFile("version_manifest", ".json");
        downloadFile(VERSION_MANIFEST_URL, manifestFile);

        String versionUrl = findVersionUrl(manifestFile, versionId);
        manifestFile.delete();
        if (versionUrl == null) {
            throw new IOException("未找到版本: " + versionId);
        }

        // 2. 下载版本 JSON
        File versionDir = repository.getVersionDir(versionId);
        versionDir.mkdirs();
        File versionJson = new File(versionDir, versionId + ".json");
        downloadFile(versionUrl, versionJson);

        // 3. 解析版本 JSON，下载 client JAR
        String clientUrl = findClientUrl(versionJson);
        if (clientUrl != null) {
            File clientJar = repository.getVersionJar(versionId);
            downloadFile(clientUrl, clientJar);
        } else {
            throw new IOException("版本 JSON 中未找到 client 下载信息");
        }

        // TODO: 下载 libraries (从 versionJson 的 libraries 字段)
        // TODO: 下载 assets 索引和资源

        return versionId;
    }

    /**
     * 从版本清单中找到指定版本对应的 URL
     */
    private String findVersionUrl(File manifestFile, String versionId) {
        try {
            String content = new String(Files.readAllBytes(manifestFile.toPath()), "UTF-8");
            JsonObject manifest = JsonParser.parseString(content).getAsJsonObject();
            JsonArray versions = manifest.getAsJsonArray("versions");
            for (int i = 0; i < versions.size(); i++) {
                JsonObject v = versions.get(i).getAsJsonObject();
                if (versionId.equals(v.get("id").getAsString())) {
                    return v.get("url").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从版本 JSON 中找到 client JAR 的下载 URL
     */
    private String findClientUrl(File versionJson) {
        try {
            String content = new String(Files.readAllBytes(versionJson.toPath()), "UTF-8");
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            JsonObject downloads = json.getAsJsonObject("downloads");
            if (downloads != null && downloads.has("client")) {
                JsonObject client = downloads.getAsJsonObject("client");
                return client.get("url").getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 下载文件到目标路径
     */
    private void downloadFile(String urlStr, File dest) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(true);

        try (InputStream is = conn.getInputStream()) {
            Files.copy(is, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
        }
    }
}
