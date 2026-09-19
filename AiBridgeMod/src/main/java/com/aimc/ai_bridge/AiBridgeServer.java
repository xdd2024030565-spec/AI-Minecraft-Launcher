package com.aimc.ai_bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * AI Bridge HTTP API 服务器
 *
 * 在游戏内运行一个本地 HTTP 服务器，提供以下接口:
 *
 * 基础:
 * GET  /api/health       — 健康检查
 * GET  /api/state        — 获取玩家状态
 * POST /api/action       — 执行动作
 * GET  /api/inventory    — 获取背包
 * GET  /api/blocks       — 扫描附近方块
 * GET  /api/screenshot   — 截图 (PNG)
 * POST /api/chat         — 发送聊天
 * GET  /api/recipe       — 查询合成配方
 * GET  /api/events       — 游戏事件队列
 *
 * AI 假人 (本地存档):
 * POST /api/fakeplayer/spawn   — 生成假人 {name, x, y, z}
 * POST /api/fakeplayer/remove  — 移除假人 {name}
 * GET  /api/fakeplayer/list    — 假人列表
 * GET  /api/fakeplayer/state   — 假人状态 ?name=
 * POST /api/fakeplayer/action  — 控制假人 {name, action, ...}
 *
 * 对话:
 * GET  /api/dialogue/poll      — 拉取新聊天消息
 * POST /api/dialogue/reply     — AI 回复到聊天 {message}
 */
public class AiBridgeServer {

    private final int port;
    private HttpServer server;
    private final Gson gson;

    public AiBridgeServer(int port) {
        this.port = port;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // 健康检查
        server.createContext("/api/health", this::handleHealth);

        // 获取游戏状态
        server.createContext("/api/state", this::handleGetState);

        // 执行动作
        server.createContext("/api/action", this::handleAction);

        // 获取背包
        server.createContext("/api/inventory", this::handleInventory);

        // 扫描附近方块
        server.createContext("/api/blocks", this::handleGetBlocks);

        // 获取截图
        server.createContext("/api/screenshot", this::handleScreenshot);

        // 发送聊天
        server.createContext("/api/chat", this::handleChat);

        // 查询合成配方
        server.createContext("/api/recipe", this::handleGetRecipe);

        // 获取游戏事件
        server.createContext("/api/events", this::handleEvents);

        // ============ AI 假人 (本地存档) ============
        server.createContext("/api/fakeplayer/spawn", this::handleFakePlayerSpawn);
        server.createContext("/api/fakeplayer/remove", this::handleFakePlayerRemove);
        server.createContext("/api/fakeplayer/list", this::handleFakePlayerList);
        server.createContext("/api/fakeplayer/state", this::handleFakePlayerState);
        server.createContext("/api/fakeplayer/action", this::handleFakePlayerAction);

        // ============ 对话 ============
        server.createContext("/api/dialogue/poll", this::handleDialoguePoll);
        server.createContext("/api/dialogue/reply", this::handleDialogueReply);

        // 根路径说明
        server.createContext("/", exchange -> {
            String response = "AI Bridge API is running.\n"
                + "Endpoints: /api/health /api/state /api/action /api/inventory /api/blocks\n"
                + "           /api/screenshot /api/chat /api/recipe /api/events\n"
                + "FakePlayer: /api/fakeplayer/spawn|remove|list|state|action\n"
                + "Dialogue:   /api/dialogue/poll|reply\n";
            sendText(exchange, 200, response);
        });

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ==================== 基础处理器 ====================

    private void handleHealth(HttpExchange exchange) throws IOException {
        sendJson(exchange, Map.of(
            "status", "ok",
            "mod", "ai_bridge",
            "version", "1.1.0",
            "port", port,
            "minecraft", GameStateCollector.getMinecraftVersion()
        ));
    }

    private void handleGetState(HttpExchange exchange) throws IOException {
        try {
            GameStateCollector.GameState state = GameStateCollector.collect();
            sendJson(exchange, state);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleAction(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed. Use POST.");
            return;
        }

        try {
            String body = readBody(exchange);
            @SuppressWarnings("unchecked")
            Map<String, Object> request = gson.fromJson(body, Map.class);

            String action = (String) request.get("action");
            if (action == null || action.isEmpty()) {
                sendError(exchange, 400, "'action' field is required");
                return;
            }

            ActionExecutor.ActionResult result = ActionExecutor.execute(action, request);
            sendJson(exchange, result);
        } catch (Exception e) {
            sendJson(exchange, new ActionExecutor.ActionResult(false, "Error: " + e.getMessage()));
        }
    }

    private void handleInventory(HttpExchange exchange) throws IOException {
        try {
            InventoryInspector.InventoryData inv = InventoryInspector.inspect();
            sendJson(exchange, inv);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleGetBlocks(HttpExchange exchange) throws IOException {
        try {
            int radius = 5;
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("radius=")) {
                String r = query.split("radius=")[1].split("&")[0];
                radius = Math.min(Integer.parseInt(r), 20);
            }

            BlockScanner.BlockScanResult blocks = BlockScanner.scan(radius);
            sendJson(exchange, blocks);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleScreenshot(HttpExchange exchange) throws IOException {
        try {
            byte[] png = ScreenshotCapture.capture();
            if (png == null || png.length == 0) {
                sendError(exchange, 503, "Screenshot not available (game not rendered yet)");
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, png.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(png);
            }
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleChat(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed. Use POST.");
            return;
        }

        try {
            String body = readBody(exchange);
            @SuppressWarnings("unchecked")
            Map<String, Object> request = gson.fromJson(body, Map.class);
            String message = (String) request.get("message");

            if (message == null || message.isEmpty()) {
                sendError(exchange, 400, "'message' field is required");
                return;
            }

            ActionExecutor.sendChat(message);
            sendJson(exchange, Map.of("success", true, "message", message));
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleGetRecipe(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            String item = null;
            if (query != null && query.contains("item=")) {
                item = query.split("item=")[1].split("&")[0];
            }

            RecipeLookup.RecipeInfoList recipes;
            if (item != null && !item.isEmpty()) {
                recipes = RecipeLookup.findRecipes(item);
            } else {
                recipes = RecipeLookup.getAllRecipes();
            }

            sendJson(exchange, recipes);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleEvents(HttpExchange exchange) throws IOException {
        try {
            List<GameEventCollector.GameEvent> events = GameEventCollector.drainEvents();
            sendJson(exchange, Map.of(
                "events", events,
                "count", events.size()
            ));
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    // ==================== AI 假人处理器 ====================

    private void handleFakePlayerSpawn(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed. Use POST.");
            return;
        }
        try {
            String body = readBody(exchange);
            @SuppressWarnings("unchecked")
            Map<String, Object> request = gson.fromJson(body, Map.class);

            final String name = request.get("name") != null
                    ? request.get("name").toString() : "AI-Bot";
            final double x = toDouble(request.get("x"), 0.0);
            final double y = toDouble(request.get("y"), 64.0);
            final double z = toDouble(request.get("z"), 0.0);
            final float yaw = (float) toDouble(request.get("yaw"), 0.0);
            final float pitch = (float) toDouble(request.get("pitch"), 0.0);

            Map<String, Object> result = onServer(() -> {
                MinecraftServer srv = MinecraftClient.getInstance().getServer();
                Map<String, Object> r = new HashMap<>();
                if (srv == null) {
                    r.put("success", false);
                    r.put("message", "未进入世界（仅本地存档可用）");
                    return r;
                }
                boolean ok = FakePlayerManager.spawn(srv, name, x, y, z, yaw, pitch) != null;
                r.put("success", ok);
                r.put("name", name);
                return r;
            }, 8000);

            sendJson(exchange, result);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleFakePlayerRemove(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed. Use POST.");
            return;
        }
        try {
            String body = readBody(exchange);
            @SuppressWarnings("unchecked")
            Map<String, Object> request = gson.fromJson(body, Map.class);
            final String name = request.get("name") != null
                    ? request.get("name").toString() : "AI-Bot";

            Map<String, Object> result = onServer(() -> {
                MinecraftServer srv = MinecraftClient.getInstance().getServer();
                Map<String, Object> r = new HashMap<>();
                r.put("success", FakePlayerManager.remove(srv, name));
                r.put("name", name);
                return r;
            }, 8000);

            sendJson(exchange, result);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleFakePlayerList(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("fakePlayers", FakePlayerManager.listNames());
            result.put("count", FakePlayerManager.listNames().size());
            sendJson(exchange, result);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleFakePlayerState(HttpExchange exchange) throws IOException {
        try {
            String name = "AI-Bot";
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("name=")) {
                name = query.split("name=")[1].split("&")[0];
            }
            sendJson(exchange, FakePlayerManager.state(name));
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleFakePlayerAction(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed. Use POST.");
            return;
        }
        try {
            String body = readBody(exchange);
            @SuppressWarnings("unchecked")
            Map<String, Object> request = gson.fromJson(body, Map.class);
            final String name = request.get("name") != null
                    ? request.get("name").toString() : "AI-Bot";

            Map<String, Object> result = onServer(() ->
                    FakePlayerManager.execute(name, request), 8000);
            sendJson(exchange, result);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    // ==================== 对话处理器 ====================

    private void handleDialoguePoll(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("incoming", ChatDialogueCollector.pollIncoming());
            result.put("outgoing", ChatDialogueCollector.pollOutgoing());
            sendJson(exchange, result);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleDialogueReply(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed. Use POST.");
            return;
        }
        try {
            String body = readBody(exchange);
            @SuppressWarnings("unchecked")
            Map<String, Object> request = gson.fromJson(body, Map.class);
            String message = request.get("message") == null ? "" : request.get("message").toString();

            boolean ok = ChatDialogueCollector.reply(message);
            sendJson(exchange, Map.of("success", ok));
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 在服务器主线程执行任务并等待结果（HTTP 线程 → 主线程调度）
     */
    private static <T> T onServer(Callable<T> task, long timeoutMs) throws Exception {
        MinecraftServer server = MinecraftClient.getInstance().getServer();
        if (server == null) {
            throw new IllegalStateException("游戏未运行或未进入世界");
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private static double toDouble(Object value, double fallback) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private void sendJson(HttpExchange exchange, Object data) throws IOException {
        String json = gson.toJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        byte[] bytes = json.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendText(HttpExchange exchange, int code, String text) throws IOException {
        byte[] bytes = text.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        sendJson(exchange, Map.of("error", true, "code", code, "message", message));
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), "UTF-8");
        }
    }
}
