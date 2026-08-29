package com.aimc.ai_bridge;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI Bridge Mod — 核心入口
 * 
 * 在 Minecraft 客户端启动时，自动启动一个本地 HTTP API 服务器，
 * 允许外部 AI 控制器通过 HTTP 接口查询游戏状态并执行动作。
 * 
 * 默认端口: 25580
 */
public class AiBridgeMod implements ModInitializer {
    
    public static final Logger LOGGER = LoggerFactory.getLogger("AI-Bridge");
    public static final String MOD_ID = "ai_bridge";
    public static final int DEFAULT_PORT = 25580;
    
    private static AiBridgeServer apiServer;
    private static final AtomicBoolean serverStarted = new AtomicBoolean(false);
    
    @Override
    public void onInitialize() {
        LOGGER.info("[AI Bridge] Initializing AI Bridge Mod...");
        
        // 注册游戏事件收集器
        GameEventCollector.register();
        
        // 注册每 tick 回调 — 用于更新游戏状态缓存
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GameStateCollector.updateTickCache(client);
        });
        
        // 启动 HTTP API 服务器 (在单独线程中)
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // 等待游戏完全加载
                Thread.sleep(3000);
                
                int port = DEFAULT_PORT;
                // 从系统属性读取端口 (允许启动器配置)
                String portProp = System.getProperty("ai.bridge.port");
                if (portProp != null) {
                    port = Integer.parseInt(portProp);
                }
                
                apiServer = new AiBridgeServer(port);
                apiServer.start();
                serverStarted.set(true);
                LOGGER.info("[AI Bridge] HTTP API started on port {}", port);
            } catch (Exception e) {
                LOGGER.error("[AI Bridge] Failed to start HTTP API", e);
            }
        });
        
        LOGGER.info("[AI Bridge] AI Bridge Mod initialized. API will be available at http://127.0.0.1:{}", DEFAULT_PORT);
    }
    
    /**
     * 获取 HTTP API 服务器实例
     */
    public static AiBridgeServer getServer() {
        return apiServer;
    }
    
    /**
     * API 服务器是否已启动
     */
    public static boolean isServerStarted() {
        return serverStarted.get();
    }
}