package com.aimc.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.List;

/**
 * 游戏 API 客户端
 *
 * 封装对 AI Bridge Mod HTTP API 的所有请求。
 */
public class GameApiClient {

    private final Retrofit retrofit;
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

        retrofit = new Retrofit.Builder()
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
        CallActionResult postAction(@Body ActionRequest action);

        @POST("api/chat")
        CallActionResult sendChat(@Body ChatRequest request);
    }

    // 响应数据类
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
    }

    public static class ActionRequest {
        public String action;
        public Map<String, Object> params;

        public ActionRequest(String action, Map<String, Object> params) {
            this.action = action;
            this.params = params != null ? params : new java.util.HashMap<>();
        }
    }

    // 同步方法
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
        Response ActionResult resp = api.postAction(new ActionRequest(action, params)).execute();
        if (!resp.isSuccessful()) throw new Exception("HTTP " + resp.code());
        return resp.body();
    }

    public ActionResult sendChat(String message) throws Exception {
        Response ActionResult resp = api.sendChat(new ChatRequest(message)).execute();
        if (!resp.isSuccessful()) throw new Exception("HTTP " + resp.code());
        return resp.body();
    }
}