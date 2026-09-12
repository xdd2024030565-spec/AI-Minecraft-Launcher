package com.aimc.controller;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 游戏 API 客户端
 *
 * 封装对 AI Bridge Mod HTTP API 的所有请求。
 * 支持: 游戏状态、背包、方块扫描、动作执行、聊天、截图、配方、事件。
 */
public class GameApiClient {

    private final GameApi api;
    private final Gson gson = new Gson();
    private final int port;

    public GameApiClient(int port) {
        this.port = port;

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://127.0.0.1:" + port + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        api = retrofit.create(GameApi.class);
    }

    private interface GameApi {
        @GET("api/state")
        Call<GameStateResponse> getState();

        @GET("api/inventory")
        Call<InventoryResponse> getInventory();

        @GET("api/blocks")
        Call<BlocksResponse> getBlocks(@Query("radius") int radius);

        @POST("api/action")
        Call<ActionResult> postAction(@Body ActionRequest action);

        @POST("api/chat")
        Call<ActionResult> sendChat(@Body ChatRequest request);

        @GET("api/recipe")
        Call<RecipeListResponse> getRecipes(@Query("item") String item);

        @GET("api/events")
        Call<EventsResponse> getEvents();
    }

    // ==================== 响应数据类 ====================

    public static class GameStateResponse {
        public boolean connected;
        public List<Double> position;
        public float health;
        public int food;
        public float saturation;
        public int xpLevel;
        public float xpProgress;
        public List<Double> rotation;
        public String dimension;
        public long worldTime;
        public boolean isDay;
        public boolean sprinting;
        public boolean sneaking;
        public boolean onGround;
        public boolean inWater;
        public boolean hasSkyLight;
        public int serverPort;
    }

    public static class InventoryResponse {
        public boolean connected;
        public List<ItemInfo> mainInventory;
        public List<ItemInfo> armor;
        public int selectedSlot;
        public ItemInfo offhand;
    }

    public static class ItemInfo {
        public String name;
        public int count;
        public int slot;
    }

    public static class BlocksResponse {
        public boolean connected;
        public List<BlockInfo> nearbyBlocks;
        public Map<String, Integer> blockSummary;
        public List<EntityInfo> nearbyEntities;
        public int scanRadius;
    }

    public static class BlockInfo {
        public String name;
        public int dx;
        public int dy;
        public int dz;
    }

    public static class EntityInfo {
        public String type;
        public double dx;
        public double dy;
        public double dz;
        public double distance;
    }

    public static class ActionResult {
        public boolean success;
        public String message;
    }

    public static class ChatRequest {
        public String message;
        public ChatRequest(String message) { this.message = message; }
    }

    public static class ActionRequest {
        public String action;
        public Map<String, Object> params;
        public ActionRequest(String action, Map<String, Object> params) {
            this.action = action;
            this.params = params != null ? params : new HashMap<>();
        }
    }

    // 新增: 配方相关数据类

    public static class RecipeListResponse {
        public boolean connected;
        public List<RecipeInfo> recipes;
        public int count;
    }

    public static class RecipeInfo {
        public String id;
        public String group;
        public String resultItem;
        public int resultCount;
        public List<String> ingredients;
    }

    // 新增: 事件相关数据类

    public static class EventsResponse {
        public List<GameEvent> events;
        public int count;
    }

    public static class GameEvent {
        public String type;
        public String message;
        public long timestamp;
    }

    // ==================== 同步方法 ====================

    public GameStateResponse getGameState() throws Exception {
        Response<GameStateResponse> resp = api.getState().execute();
        if (!resp.isSuccessful()) throw new Exception("HTTP " + resp.code());
        return resp.body();
    }

    public InventoryResponse getInventory() throws Exception {
        Response<InventoryResponse> resp = api.getInventory().execute();
        if (!resp.isSuccessful()) throw new Exception("HTTP " + resp.code());
        return resp.body();
    }

    public BlocksResponse getNearbyBlocks(int radius) throws Exception {
        Response<BlocksResponse> resp = api.getBlocks(radius).execute();
        if (!resp.isSuccessful()) throw new Exception("HTTP " + resp.code());
        return resp.body();
    }

    public ActionResult executeAction(String action, Map<String, Object> params) throws Exception {
        Response<ActionResult> resp = api.postAction(new ActionRequest(action, params)).execute();
        if (!resp.isSuccessful()) throw new Exception("HTTP " + resp.code());
        return resp.body();
    }

    public ActionResult sendChat(String message) throws Exception {
        Response<ActionResult> resp = api.sendChat(new ChatRequest(message)).execute();
        if (!resp.isSuccessful()) throw new Exception("HTTP " + resp.code());
        return resp.body();
    }

    /**
     * 获取游戏截图 (PNG 二进制数据)
     * 使用 HttpURLConnection 直接下载二进制数据
     */
    public byte[] getScreenshot() throws Exception {
        java.net.URL url = new java.net.URL("http://127.0.0.1:" + port + "/api/screenshot");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        try (java.io.InputStream is = conn.getInputStream()) {
            return is.readAllBytes();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * 查询合成配方
     * @param item 物品名称 (可为 null 返回全部)
     */
    public RecipeListResponse getRecipes(String item) throws Exception {
        Response<RecipeListResponse> resp = api.getRecipes(item).execute();
        if (!resp.isSuccessful()) throw new Exception("HTTP " + resp.code());
        return resp.body();
    }

    /**
     * 获取游戏事件队列
     */
    public EventsResponse getEvents() throws Exception {
        Response<EventsResponse> resp = api.getEvents().execute();
        if (!resp.isSuccessful()) throw new Exception("HTTP " + resp.code());
        return resp.body();
    }
}
