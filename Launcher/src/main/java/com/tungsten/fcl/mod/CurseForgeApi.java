package com.tungsten.fcl.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * CurseForge API 客户端 — 仿 FCL CurseForgeRemoteModRepository
 *
 * CurseForge API: https://api.curseforge.com/v1
 * 需要 API Key (通过 X-API-KEY header)
 *
 * 支持搜索 mod / modpack / shader / resourcepack
 */
public class CurseForgeApi {

    private static final String API_BASE = "https://api.curseforge.com/v1";
    private static final String API_KEY = "$2a$07$w8f1y2E3Y4R5T6Y7U8I9O0P1Q2R3S4T5U6V7W8X9Y0Z1a2b3c4d5e6f7g8h9i0j1k2l3";
    // CurseForge 需要真实的 API Key，这里用占位值
    // 实际使用时需要从 CurseForge 控制台获取

    public enum Section {
        MOD(6),
        MODPACK(4471),
        RESOURCE_PACK(12),
        SHADER_PACK(6552);

        private final int classId;
        Section(int classId) { this.classId = classId; }
        public int getClassId() { return classId; }
    }

    public enum SortField {
        FEATURED(0),
        POPULARITY(1),
        LAST_UPDATED(2),
        NAME(3),
        AUTHOR(4),
        TOTAL_DOWNLOADS(6),
        DATE_CREATED(11);

        private final int value;
        SortField(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    /**
     * 搜索项目
     */
    public List<ModSearchResult> search(Section section, String gameVersion,
                                       String searchFilter, int page, int pageSize,
                                       SortField sort) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(API_BASE + "/mods/search?");
        urlBuilder.append("gameId=432");
        urlBuilder.append("&classId=").append(section.getClassId());
        if (gameVersion != null && !gameVersion.isEmpty()) {
            urlBuilder.append("&gameVersion=").append(urlEncode(gameVersion));
        }
        if (searchFilter != null && !searchFilter.isEmpty()) {
            urlBuilder.append("&searchFilter=").append(urlEncode(searchFilter));
        }
        urlBuilder.append("&sortField=").append(sort.getValue());
        urlBuilder.append("&sortOrder=desc");
        urlBuilder.append("&index=").append(page * pageSize);
        urlBuilder.append("&pageSize=").append(pageSize);

        String response = httpGet(urlBuilder.toString());
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        List<ModSearchResult> results = new ArrayList<>();
        if (json.has("data")) {
            JsonArray data = json.getAsJsonArray("data");
            for (JsonElement elem : data) {
                results.add(parseSearchResult(elem.getAsJsonObject()));
            }
        }
        return results;
    }

    /**
     * 获取 Mod 的版本列表
     */
    public List<ModFile> getFiles(int modId) throws IOException {
        String url = API_BASE + "/mods/" + modId + "/files?pageSize=50";
        String response = httpGet(url);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        List<ModFile> files = new ArrayList<>();
        if (json.has("data")) {
            for (JsonElement elem : json.getAsJsonArray("data")) {
                files.add(parseFile(elem.getAsJsonObject()));
            }
        }
        return files;
    }

    /**
     * 下载 Mod 文件
     */
    public void downloadFile(String downloadUrl, java.io.File destFile) throws IOException {
        destFile.getParentFile().mkdirs();
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);
        try (InputStream is = conn.getInputStream()) {
            Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
        }
    }

    // === 数据解析 ===

    private ModSearchResult parseSearchResult(JsonObject obj) {
        ModSearchResult result = new ModSearchResult();
        result.id = getInt(obj, "id");
        result.slug = getString(obj, "slug");
        result.name = getString(obj, "name");
        result.summary = getString(obj, "summary");
        result.downloadCount = getInt(obj, "downloadCount");
        result.websiteUrl = getString(obj, "websiteUrl");

        if (obj.has("logo")) {
            result.iconUrl = getString(obj.getAsJsonObject("logo"), "thumbnailUrl");
        }
        if (obj.has("latestFiles")) {
            for (JsonElement f : obj.getAsJsonArray("latestFiles")) {
                result.latestFileUrls.add(getString(f.getAsJsonObject(), "downloadUrl"));
            }
        }
        if (obj.has("gameVersionLatestFiles")) {
            for (JsonElement gv : obj.getAsJsonArray("gameVersionLatestFiles")) {
                result.gameVersions.add(getString(gv.getAsJsonObject(), "gameVersion"));
            }
        }
        return result;
    }

    private ModFile parseFile(JsonObject obj) {
        ModFile file = new ModFile();
        file.id = getInt(obj, "id");
        file.fileName = getString(obj, "fileName");
        file.downloadUrl = getString(obj, "downloadUrl");
        file.fileDate = getString(obj, "fileDate");
        file.fileLength = getInt(obj, "fileLength");
        file.releaseType = getInt(obj, "releaseType");

        if (obj.has("gameVersions")) {
            for (JsonElement gv : obj.getAsJsonArray("gameVersions")) {
                file.gameVersions.add(gv.getAsString());
            }
        }
        return file;
    }

    // === 工具方法 ===

    private String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "AI-Minecraft-Launcher/1.0");
        conn.setRequestProperty("Accept", "application/json");
        if (!API_KEY.isEmpty() && !API_KEY.startsWith("$2a")) {
            conn.setRequestProperty("X-API-KEY", API_KEY);
        }
        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), "UTF-8");
        } finally {
            conn.disconnect();
        }
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) { return s; }
    }

    private String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private int getInt(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : 0;
    }

    // === 数据类 ===

    public static class ModSearchResult {
        public int id;
        public String slug;
        public String name;
        public String summary;
        public int downloadCount;
        public String websiteUrl;
        public String iconUrl;
        public List<String> latestFileUrls = new ArrayList<>();
        public List<String> gameVersions = new ArrayList<>();
    }

    public static class ModFile {
        public int id;
        public String fileName;
        public String downloadUrl;
        public String fileDate;
        public int fileLength;
        public int releaseType;
        public List<String> gameVersions = new ArrayList<>();
    }
}
