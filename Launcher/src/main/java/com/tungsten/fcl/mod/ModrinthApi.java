package com.tungsten.fcl.mod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Modrinth API 客户端 — 对齐 FCL ModrinthRemoteModRepository
 *
 * Modrinth API v2: https://api.modrinth.com/v2
 *
 * 支持:
 * - 搜索 mod / modpack / shader / resourcepack
 * - facets 分类 / 游戏版本 / 加载器 筛选
 * - 项目详情、版本列表、依赖解析
 * - 本地文件 SHA1 反查远程版本
 * - 镜像回退 (mod.mcimirror.top)
 * - 文件 SHA1/SHA512 校验
 */
public class ModrinthApi {

    private static final String OFFICIAL_API = "https://api.modrinth.com";
    private static final String CDN_HOST = "https://cdn.modrinth.com";
    private static final String MIRROR_API = "https://mod.mcimirror.top/modrinth";
    private static final String MIRROR_CDN = "https://mod.mcimirror.top";

    private static final String USER_AGENT = "AI-Minecraft-Launcher/1.0";

    /** 是否优先使用镜像 (国内推荐) */
    private boolean preferMirror = true;

    public ModrinthApi() {}

    public ModrinthApi(boolean preferMirror) {
        this.preferMirror = preferMirror;
    }

    public void setPreferMirror(boolean preferMirror) {
        this.preferMirror = preferMirror;
    }

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

    // === 搜索 ===

    /**
     * 搜索项目 (简易版)
     */
    public List<ModSearchResult> search(ProjectType type, String gameVersion,
                                        String searchFilter, int page, int pageSize,
                                        SortType sort) throws IOException {
        return search(type, gameVersion, null, null, searchFilter, page, pageSize, sort);
    }

    /**
     * 搜索项目 (完整版) — 对齐 FCL search()
     *
     * @param type        项目类型
     * @param gameVersion 游戏版本 (如 1.20.1)
     * @param category    分类 (如 "adventure")
     * @param loader      加载器 (如 "fabric" / "forge") — 归入 categories facet
     * @param searchFilter 搜索关键词
     * @param page        页码 (从0开始)
     * @param pageSize    每页数量
     * @param sort        排序方式
     */
    public List<ModSearchResult> search(ProjectType type, String gameVersion,
                                        String category, String loader,
                                        String searchFilter, int page, int pageSize,
                                        SortType sort) throws IOException {
        List<String> facets = new ArrayList<>();
        facets.add("[\"project_type:" + type.getApiValue() + "\"]");
        if (gameVersion != null && !gameVersion.isEmpty()) {
            facets.add("[\"versions:" + gameVersion + "\"]");
        }
        if (category != null && !category.isEmpty()) {
            facets.add("[\"categories:" + category + "\"]");
        }
        if (loader != null && !loader.isEmpty()) {
            facets.add("[\"categories:" + loader + "\"]");
        }

        String query = String.format("query=%s&facets=[%s]&offset=%d&limit=%d&index=%s",
                urlEncode(searchFilter == null ? "" : searchFilter),
                String.join(",", facets),
                page * pageSize,
                pageSize,
                sort.apiValue);

        String response = httpGet("/v2/search?" + query);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        List<ModSearchResult> results = new ArrayList<>();
        if (json.has("hits")) {
            for (JsonElement elem : json.getAsJsonArray("hits")) {
                results.add(parseSearchHit(elem.getAsJsonObject()));
            }
        }
        return results;
    }

    // === 项目详情 ===

    /**
     * 获取项目详情 — 对齐 FCL getModById()
     */
    public ModProject getProject(String idOrSlug) throws IOException {
        String response = httpGet("/v2/project/" + urlEncodePath(idOrSlug));
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        ModProject project = new ModProject();
        project.projectId = getString(json, "id");
        project.slug = getString(json, "slug");
        project.title = getString(json, "title");
        project.description = getString(json, "description");
        project.body = getString(json, "body");
        project.iconUrl = getString(json, "icon_url");
        project.projectType = getString(json, "project_type");
        project.downloads = getInt(json, "downloads");
        project.followers = getInt(json, "followers");
        project.updated = getString(json, "updated");
        project.published = getString(json, "published");

        if (json.has("categories")) {
            for (JsonElement c : json.getAsJsonArray("categories")) {
                project.categories.add(c.getAsString());
            }
        }
        if (json.has("game_versions")) {
            for (JsonElement v : json.getAsJsonArray("game_versions")) {
                project.gameVersions.add(v.getAsString());
            }
        }
        if (json.has("loaders")) {
            for (JsonElement l : json.getAsJsonArray("loaders")) {
                project.loaders.add(l.getAsString());
            }
        }
        return project;
    }

    // === 版本列表 ===

    /**
     * 获取项目的所有版本
     */
    public List<ModVersion> getVersions(String projectId) throws IOException {
        return getVersions(projectId, null, null);
    }

    /**
     * 获取项目的版本列表 (可按游戏版本/加载器筛选) — 对齐 FCL getRemoteVersionsById()
     */
    public List<ModVersion> getVersions(String projectId, String gameVersion, String loader) throws IOException {
        StringBuilder url = new StringBuilder("/v2/project/")
                .append(urlEncodePath(projectId)).append("/version");

        List<String> params = new ArrayList<>();
        if (gameVersion != null && !gameVersion.isEmpty()) {
            params.add("game_versions=[\"" + gameVersion + "\"]");
        }
        if (loader != null && !loader.isEmpty()) {
            params.add("loaders=[\"" + loader + "\"]");
        }
        if (!params.isEmpty()) {
            url.append("?").append(String.join("&", params));
        }

        String response = httpGet(url.toString());
        JsonArray arr = JsonParser.parseString(response).getAsJsonArray();

        List<ModVersion> versions = new ArrayList<>();
        for (JsonElement elem : arr) {
            versions.add(parseVersion(elem.getAsJsonObject()));
        }
        return versions;
    }

    /**
     * 获取指定版本的文件信息 — 对齐 FCL getModFile()
     */
    public ModVersion getVersion(String versionId) throws IOException {
        String response = httpGet("/v2/version/" + urlEncodePath(versionId));
        return parseVersion(JsonParser.parseString(response).getAsJsonObject());
    }

    /**
     * 根据文件 SHA1 反查版本 — 对齐 FCL getRemoteVersionByLocalFile()
     *
     * @param sha1 本地文件的 SHA1
     * @return 匹配的版本，未找到返回 null
     */
    public ModVersion getVersionByHash(String sha1) throws IOException {
        try {
            String response = httpGetPost("/v2/version_file/" + sha1,
                    "{\"algorithm\":\"sha1\"}");
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            return parseVersion(json);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 计算本地文件 SHA1
     */
    public static String sha1Of(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("计算 SHA1 失败: " + e.getMessage(), e);
        }
    }

    // === 下载 ===

    /**
     * 下载文件 (使用镜像回退) — 对齐 FCL 的多候选源策略
     *
     * @param downloadUrl 原始下载 URL
     * @param destFile    目标文件
     * @param expectedSha1 期望 SHA1 (可为 null 表示不校验)
     */
    public void downloadFile(String downloadUrl, File destFile, String expectedSha1) throws IOException {
        destFile.getParentFile().mkdirs();
        List<String> candidates = buildDownloadCandidates(downloadUrl);

        IOException lastEx = null;
        for (String candidate : candidates) {
            try {
                downloadRaw(candidate, destFile);
                if (expectedSha1 != null && !expectedSha1.isEmpty()) {
                    String actual = sha1Of(destFile);
                    if (!actual.equalsIgnoreCase(expectedSha1)) {
                        // 校验失败，删除并尝试下一个源
                        destFile.delete();
                        lastEx = new IOException("SHA1 校验失败: 期望 " + expectedSha1 + " 实际 " + actual);
                        continue;
                    }
                }
                return; // 成功
            } catch (IOException e) {
                lastEx = e;
            }
        }
        throw lastEx != null ? lastEx : new IOException("所有下载源均失败");
    }

    public void downloadFile(String downloadUrl, File destFile) throws IOException {
        downloadFile(downloadUrl, destFile, null);
    }

    /**
     * 构建下载候选 URL (镜像优先)
     */
    private List<String> buildDownloadCandidates(String downloadUrl) {
        List<String> result = new ArrayList<>();
        if (downloadUrl == null || downloadUrl.isEmpty()) return result;

        String mirrored = downloadUrl;
        if (downloadUrl.contains(CDN_HOST)) {
            mirrored = downloadUrl.replace(CDN_HOST, MIRROR_CDN);
        }

        if (preferMirror && !mirrored.equals(downloadUrl)) {
            result.add(mirrored);
            result.add(downloadUrl);
        } else {
            result.add(downloadUrl);
            if (!mirrored.equals(downloadUrl)) result.add(mirrored);
        }
        return result;
    }

    private void downloadRaw(String urlStr, File destFile) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", USER_AGENT);
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
            for (JsonElement cat : hit.getAsJsonArray("categories")) {
                result.categories.add(cat.getAsString());
            }
        }
        if (hit.has("versions")) {
            for (JsonElement v : hit.getAsJsonArray("versions")) {
                result.gameVersions.add(v.getAsString());
            }
        }
        if (hit.has("display_categories")) {
            for (JsonElement c : hit.getAsJsonArray("display_categories")) {
                result.displayCategories.add(c.getAsString());
            }
        }
        return result;
    }

    private ModVersion parseVersion(JsonObject json) {
        ModVersion version = new ModVersion();
        version.id = getString(json, "id");
        version.projectId = getString(json, "project_id");
        version.name = getString(json, "version_number");
        version.versionTitle = getString(json, "name");
        version.changelog = getString(json, "changelog");
        version.datePublished = getString(json, "date_published");
        version.versionType = getString(json, "version_type");
        version.downloads = getInt(json, "downloads");

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
        if (json.has("dependencies")) {
            for (JsonElement d : json.getAsJsonArray("dependencies")) {
                JsonObject dep = d.getAsJsonObject();
                ModDependency dependency = new ModDependency();
                dependency.versionId = getString(dep, "version_id");
                dependency.projectId = getString(dep, "project_id");
                dependency.fileName = getString(dep, "file_name");
                dependency.dependencyType = getString(dep, "dependency_type");
                version.dependencies.add(dependency);
            }
        }
        if (json.has("files")) {
            for (JsonElement f : json.getAsJsonArray("files")) {
                JsonObject fileObj = f.getAsJsonObject();
                ModVersionFile file = new ModVersionFile();
                file.url = getString(fileObj, "url");
                file.filename = getString(fileObj, "filename");
                file.size = getInt(fileObj, "size");
                file.primary = fileObj.has("primary") && fileObj.get("primary").getAsBoolean();
                if (fileObj.has("hashes")) {
                    JsonObject hashes = fileObj.getAsJsonObject("hashes");
                    file.sha1 = getString(hashes, "sha1");
                    file.sha512 = getString(hashes, "sha512");
                }
                version.files.add(file);
            }
        }
        return version;
    }

    // === HTTP 工具 ===

    private String httpGet(String path) throws IOException {
        List<String> bases = preferMirror
                ? java.util.Arrays.asList(MIRROR_API, OFFICIAL_API)
                : java.util.Arrays.asList(OFFICIAL_API, MIRROR_API);

        IOException lastEx = null;
        for (String base : bases) {
            try {
                return httpGetRaw(base + path);
            } catch (IOException e) {
                lastEx = e;
            }
        }
        throw lastEx != null ? lastEx : new IOException("请求失败: " + path);
    }

    private String httpGetRaw(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        try (InputStream is = conn.getInputStream()) {
            return new String(readAll(is), "UTF-8");
        } finally {
            conn.disconnect();
        }
    }

    private String httpGetPost(String path, String jsonBody) throws IOException {
        List<String> bases = preferMirror
                ? java.util.Arrays.asList(MIRROR_API, OFFICIAL_API)
                : java.util.Arrays.asList(OFFICIAL_API, MIRROR_API);

        IOException lastEx = null;
        for (String base : bases) {
            try {
                URL url = new URL(base + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.getOutputStream().write(jsonBody.getBytes("UTF-8"));
                try (InputStream is = conn.getInputStream()) {
                    return new String(readAll(is), "UTF-8");
                } finally {
                    conn.disconnect();
                }
            } catch (IOException e) {
                lastEx = e;
            }
        }
        throw lastEx != null ? lastEx : new IOException("请求失败: " + path);
    }

    private static byte[] readAll(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = is.read(buffer)) != -1) bos.write(buffer, 0, n);
        return bos.toByteArray();
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) { return s; }
    }

    private static String urlEncodePath(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
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
        public List<String> displayCategories = new ArrayList<>();
        public List<String> gameVersions = new ArrayList<>();
    }

    public static class ModProject {
        public String projectId;
        public String slug;
        public String title;
        public String description;
        public String body;
        public String iconUrl;
        public String projectType;
        public int downloads;
        public int followers;
        public String updated;
        public String published;
        public List<String> categories = new ArrayList<>();
        public List<String> gameVersions = new ArrayList<>();
        public List<String> loaders = new ArrayList<>();
    }

    public static class ModVersion {
        public String id;
        public String projectId;
        public String name;
        public String versionTitle;
        public String changelog;
        public String datePublished;
        public String versionType;
        public int downloads;
        public List<String> gameVersions = new ArrayList<>();
        public List<String> loaders = new ArrayList<>();
        public List<ModVersionFile> files = new ArrayList<>();
        public List<ModDependency> dependencies = new ArrayList<>();

        /** 获取主文件 */
        public ModVersionFile getPrimaryFile() {
            for (ModVersionFile f : files) {
                if (f.primary) return f;
            }
            return files.isEmpty() ? null : files.get(0);
        }
    }

    public static class ModVersionFile {
        public String url;
        public String filename;
        public int size;
        public boolean primary;
        public String sha1;
        public String sha512;
    }

    public static class ModDependency {
        public String versionId;
        public String projectId;
        public String fileName;
        public String dependencyType; // required / optional / incompatible / embedded

        public boolean isRequired() { return "required".equals(dependencyType); }
    }
}
