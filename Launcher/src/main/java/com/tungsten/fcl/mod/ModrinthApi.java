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
 * Modrinth API 客户端 — 仿 FCL ModrinthRemoteModRepository
 *
 * Modrinth API v2: https://api.modrinth.com/v2
 * 支持搜索 mod / modpack / shader / resourcepack
 */
public class ModrinthApi {

    private static final String API_BASE = "https://api.modrinth.com/v2";

    public enum ProjectType {
        MOD("mod"),
        MODPACK("modpack"),
        RESOURCE_PACK("resourcepack"),
        SHADER_PACK("shader");

        private final String apiValue;
        ProjectType(String apiValue) { this.apiValue = apiValue; }
        public String getApiValue() { return apiValue; }
    }

    public enum SortType {
        RELEVANCE("relevance"),
        NEWEST("newest"),
        UPDATED("updated"),
        DOWNLOADS("downloads");

        private final String apiValue;
        SortType(String apiValue) { this.apiValue = apiValue; }
    }

    /**
     * 搜索项目
     *
     * @param type        项目类型
     * @param gameVersion 游戏版本 (可为空)
     * @param searchFilter 搜索关键词
     * @param page        页码 (从0开始)
     * @param pageSize     每页数量
     * @param sort         排序方式
     * @return 搜索结果列表
     */
    public List<ModSearchResult> search(ProjectType type, String gameVersion,
                                        String searchFilter, int page, int pageSize,
                                        SortType sort) throws IOException {
        // 构建 facets
        List<String> facets = new ArrayList<>();
        facets.add("[\"project_type:" + type.getApiValue() + "\"]");
        if (gameVersion != null && !gameVersion.isEmpty()) {
            facets.add("[\"versions:" + gameVersion + "\"]");
        }

        String query = String.format("query=%s&facets=[%s]&offset=%d&limit=%d&index=%s",
                urlEncode(searchFilter),
                String.join(",", facets),
                page * pageSize,
                pageSize,
                sort.apiValue
        );

        String response = httpGet(API_BASE + "/search?" + query);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        List<ModSearchResult> results = new ArrayList<>();
        if (json.has("hits")) {
            JsonArray hits = json.getAsJsonArray("hits");
            for (JsonElement elem : hits) {
                JsonObject hit = elem.getAsJsonObject();
                results.add(parseSearchHit(hit));
            }
        }
        return results;
    }

    /**
     * 获取项目的版本列表
     */
    public List<ModVersion> getVersions(String projectId, String gameVersion) throws IOException {
        String url = API_BASE + "/project/" + projectId + "/version";
        if (gameVersion != null && !gameVersion.isEmpty()) {
            url += "?game_versions=[\"" + gameVersion + "\"]";
        }
        String response = httpGet(url);
        JsonArray arr = JsonParser.parseString(response).getAsJsonArray();

        List<ModVersion> versions = new ArrayList<>();
        for (JsonElement elem : arr) {
            versions.add(parseVersion(elem.getAsJsonObject()));
        }
        return versions;
    }

    /**
     * 下载 Mod 文件
     *
     * @param downloadUrl 下载 URL
     * @param destFile    目标文件
     */
    public void downloadFile(String downloadUrl, java.io.File destFile) throws IOException {
        destFile.getParentFile().mkdirs();
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);
        // Modrinth 要求 User-Agent
        conn.setRequestProperty("User-Agent", "AI-Minecraft-Launcher/1.0");
        try (InputStream is = conn.getInputStream()) {
            Files.copy(is, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
        }
    }

    // === 数据解析 ===

    private ModSearchResult parseSearchHit(JsonObject hit) {
        ModSearchResult result = new ModSearchResult();
        result.projectId = getString(hit, "project_id");
        result.slug = getString(hit, "slug");
        result.title = getString(hit, "title");
        result.description = getString(hit, "description");
        result.author = getString(hit, "author");
        result.downloadCount = getInt(hit, "downloads");
        result.follows = getInt(hit, "follows");
        result.iconUrl = getString(hit, "icon_url");
        result.pageUrl = getString(hit, "page_url");

        if (hit.has("categories")) {
            result.categories = new ArrayList<>();
            for (JsonElement cat : hit.getAsJsonArray("categories")) {
                result.categories.add(cat.getAsString());
            }
        }
        if (hit.has("versions")) {
            result.gameVersions = new ArrayList<>();
            for (JsonElement v : hit.getAsJsonArray("versions")) {
                result.gameVersions.add(v.getAsString());
            }
        }
        return result;
    }

    private ModVersion parseVersion(JsonObject json) {
        ModVersion version = new ModVersion();
        version.id = getString(json, "id");
        version.name = getString(json, "version_number");
        version.datePublished = getString(json, "date_published");
        version.versionType = getString(json, "version_type");

        if (json.has("game_versions")) {
            for (JsonElement v : json.getAsJsonArray("game_versions")) {
                version.gameVersions.add(v.getAsString());
            }
        }
        if (json.has("loaders")) {
            for (JsonElement l : json.getAsJsonArray("loaders")) {
                version.loaders.add(l.getAsString());
            }
        }
        if (json.has("files")) {
            for (JsonElement f : json.getAsJsonArray("files")) {
                JsonObject fileObj = f.getAsJsonObject();
                ModVersionFile file = new ModVersionFile();
                file.url = getString(fileObj, "url");
                file.filename = getString(fileObj, "filename");
                file.size = getInt(fileObj, "size");
                version.files.add(file);
            }
        }
        return version;
    }

    // === 工具方法 ===

    private String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "AI-Minecraft-Launcher/1.0");
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
        public String projectId;
        public String slug;
        public String title;
        public String description;
        public String author;
        public int downloadCount;
        public int follows;
        public String iconUrl;
        public String pageUrl;
        public List<String> categories = new ArrayList<>();
        public List<String> gameVersions = new ArrayList<>();
    }

    public static class ModVersion {
        public String id;
        public String name;
        public String datePublished;
        public String versionType;
        public List<String> gameVersions = new ArrayList<>();
        public List<String> loaders = new ArrayList<>();
        public List<ModVersionFile> files = new ArrayList<>();
    }

    public static class ModVersionFile {
        public String url;
        public String filename;
        public int size;
    }
}
