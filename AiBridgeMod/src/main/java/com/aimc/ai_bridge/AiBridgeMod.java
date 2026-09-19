package com.aimc.ai_bridge;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI Bridge Mod — 核心入口
 *
 * 在 Minecraft 客户端启动时，自动启动一个本地 HTTP API 服务器，
 * 允许外部 AI 控制器通过 HTTP 接口查询游戏状态并执行动作。
 *
 * v1.1.0: 新增 AI 假人（本地存档）与对话系统。
 * v1.1.1: 端口支持从游戏目录文件读取（多实例支持）。
 *
 * 端口解析顺序：
 * 1. 游戏目录下 ai_bridge_port.txt（启动器写入，多实例用）
 * 2. 系统属性 ai.bridge.port
 * 3. 默认 25580
 */
public class AiBridgeMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("AI-Bridge");
    public static final String MOD_ID = "ai_bridge";
    public static final int DEFAULT_PORT = 25580;

    /** 启动器写入的端口文件名（多实例区分） */
    public static final String PORT_FILE_NAME = "ai_bridge_port.txt";

    private static AiBridgeServer apiServer;
    private static final AtomicBoolean serverStarted = new AtomicBoolean(false);

    @Override
    public void onInitialize() {
        LOGGER.info("[AI Bridge] Initializing AI Bridge Mod...");

        // 注册游戏事件收集器
        GameEventCollector.register();

        // 注册对话收集器（聊天监听）
        ChatDialogueCollector.register();

        // 注册每 tick 回调 — 用于更新游戏状态缓存
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GameStateCollector.updateTickCache(client);
        });

        // 启动 HTTP API 服务器 (在单独线程中)
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // 等待游戏完全加载
                Thread.sleep(3000);

                int port = resolvePort();
                apiServer = new AiBridgeServer(port);
                apiServer.start();
                serverStarted.set(true);
                LOGGER.info("[AI Bridge] HTTP API started on port {}", port);
            } catch (Exception e) {
                LOGGER.error("[AI Bridge] Failed to start HTTP API", e);
            }
        });

        LOGGER.info("[AI Bridge] AI Bridge Mod initialized.");
    }

    /**
     * 解析端口：文件 > 系统属性 > 默认
     */
    private static int resolvePort() {
        // 1. 游戏目录中的端口文件（多实例支持）
        try {
            Path portFile = FabricLoader.getInstance().getGameDir().resolve(PORT_FILE_NAME);
            if (Files.exists(portFile)) {
                String content = new String(Files.readAllBytes(portFile), StandardCharsets.UTF_8).trim();
                if (!content.isEmpty()) {
                    int port = Integer.parseInt(content);
                    LOGGER.info("[AI Bridge] 使用端口文件指定端口: {}", port);
                    return port;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[AI Bridge] 读取端口文件失败，回退其它方式", e);
        }

        // 2. 系统属性（启动器 JVM 参数）
        String portProp = System.getProperty("ai.bridge.port");
        if (portProp != null) {
            try {
                return Integer.parseInt(portProp);
            } catch (NumberFormatException ignored) {
                // 保持默认端口
            }
        }

        // 3. 默认端口
        return DEFAULT_PORT;
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
