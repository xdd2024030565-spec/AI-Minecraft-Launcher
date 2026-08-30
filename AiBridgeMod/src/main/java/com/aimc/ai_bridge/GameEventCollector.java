package com.aimc.ai_bridge;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.Text;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 游戏事件收集器
 *
 * 监听 Minecraft 客户端事件（连接/断开、聊天、tick），
 * 将事件存入队列，供 WebSocket /api/events 接口推送。
 */
public class GameEventCollector {

    // 事件队列（最大保留 100 条）
    private static final ConcurrentLinkedQueue<GameEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_EVENTS = 100;

    /**
     * 注册所有事件监听器
     */
    public static void register() {
        // 连接到服务器/世界
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            pushEvent(new GameEvent("world_join", "Joined world/server"));
        });

        // 断开连接
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pushEvent(new GameEvent("world_leave", "Left world/server"));
        });

        // 聊天消息
        ClientSendMessageEvents.MODIFY_CHAT.register((message) -> {
            pushEvent(new GameEvent("chat_send", message.getString()));
            return message;
        });
    }

    /**
     * 向事件队列推入一条事件
     */
    public static void pushEvent(GameEvent event) {
        eventQueue.add(event);
        // 超出上限时移除最旧的
        while (eventQueue.size() > MAX_EVENTS) {
            eventQueue.poll();
        }
    }

    /**
     * 获取并清空事件队列
     */
    public static java.util.List<GameEvent> drainEvents() {
        java.util.List<GameEvent> list = new java.util.ArrayList<>();
        GameEvent e;
        while ((e = eventQueue.poll()) != null) {
            list.add(e);
        }
        return list;
    }

    /**
     * 游戏事件数据结构
     */
    public static class GameEvent {
        public String type;
        public String message;
        public long timestamp;

        public GameEvent(String type, String message) {
            this.type = type;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
