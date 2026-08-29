package com.aimc.ai_bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * AI Bridge HTTP API 服务器
 * 
 * 在游戏内运行一个本地 HTTP 服务器，提供以下接口:
 * 
 * GET  /api/state       — 获取玩家状态 (位置/生命/饥饿/经验)
 * POST /api/action      — 执行动作 (移动/挖矿/放置/攻击等)
 * GET  /api/inventory   — 获取背包内容
 * GET  /api/blocks      — 扫描附近方块和实体
 * GET  /api/screenshot  — 获取游戏截图 (PNG)
 * POST /api/chat        — 发送聊天消息
 * GET  /api/recipe      — 查询合成配方
 * GET  /api/health      — 健康检查
 * WS   /api/events      — WebSocket 实时事件流 (后续实现)
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
        
        // 设置 CORS 头
        server.createContext("/", exchange -> {
            String response = "AI Bridge API is running.\n" +
                "Available endpoints:\n" +
                "  GET  /api/health     - Health check\n" +
                "  GET  /api/state      - Game state\n" +
                "  POST /api/action     - Execute action\n" +
                "  GET  /api/inventory  - Inventory\n" +
                "  GET  /api/blocks     - Nearby blocks\n" +
                "  GET  /api/screenshot - Screenshot (PNG)\n" +
                "  POST /api/chat       - Send chat\n" +
                "  GET  /api/recipe     - Recipe lookup\n";
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
    
    // ==================== 处理器 ====================
    
    private void handleHealth(HttpExchange exchange) throws IOException {
        sendJson(exchange, Map.of(
            "status", "ok",
            "mod", "ai_bridge",
            "version", "1.0.0",
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
            // 从查询参数获取扫描半径
            int radius = 5;
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("radius=")) {
                String r = query.split("radius=")[1].split("&")[0];
                radius = Math.min(Integer.parseInt(r), 20); // 最大 20
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
        // 后续实现: 查询合成配方
        sendJson(exchange, Map.of(
            "status", "not_implemented",
            "message", "Recipe lookup will be available in a future version"
        ));
    }
    
    // ==================== 辅助方法 ====================
    
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