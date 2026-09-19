package com.aimc.ai_bridge;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 对话收集器 —— AI 与玩家的聊天交流
 *
 * 收集：
 * - 玩家收到的聊天消息（他人发言，AI 可参与对话）
 * - 自己发送的消息（用于向 AI 下指令）
 *
 * AI 控制器通过 /api/dialogue/poll 拉取新消息，
 * 通过 /api/dialogue/reply 回复。
 */
public final class ChatDialogueCollector {

    private static final int MAX_QUEUE = 100;

    private static final Queue<DialogueMessage> INCOMING = new ConcurrentLinkedQueue<>();
    private static final Queue<DialogueMessage> OUTGOING = new ConcurrentLinkedQueue<>();

    private ChatDialogueCollector() {
    }

    /**
     * 注册聊天事件监听（由 AiBridgeMod.onInitialize 调用）
     */
    public static void register() {
        // 接收侧：他人聊天（AI 需要参与对话的消息）
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String content = message.getString();
            String senderName = sender != null ? sender.getName() : "?";
            add(INCOMING, new DialogueMessage("chat", senderName, content));
        });

        // 发送侧：自己发出的消息（用于指令触发）
        ClientSendMessageEvents.CHAT.register(message -> {
            add(OUTGOING, new DialogueMessage("self", "me", message));
        });
    }

    private static void add(Queue<DialogueMessage> queue, DialogueMessage message) {
        queue.add(message);
        while (queue.size() > MAX_QUEUE) {
            queue.poll();
        }
    }

    /**
     * 拉取并清空收到的聊天消息
     */
    public static List<DialogueMessage> pollIncoming() {
        List<DialogueMessage> result = new ArrayList<>();
        DialogueMessage msg;
        while ((msg = INCOMING.poll()) != null) {
            result.add(msg);
        }
        return result;
    }

    /**
     * 拉取并清空自己发送的消息
     */
    public static List<DialogueMessage> pollOutgoing() {
        List<DialogueMessage> result = new ArrayList<>();
        DialogueMessage msg;
        while ((msg = OUTGOING.poll()) != null) {
            result.add(msg);
        }
        return result;
    }

    /**
     * 发送 AI 回复到游戏聊天（通过 ActionExecutor 以玩家身份发送）
     */
    public static boolean reply(String text) {
        try {
            ActionExecutor.sendChat(text);
            return true;
        } catch (Exception e) {
            AiBridgeMod.LOGGER.error("[AI Bridge] 对话回复失败", e);
            return false;
        }
    }

    /**
     * 对话消息数据类
     */
    public static class DialogueMessage {
        /** "chat"（他人）或 "self"（自己） */
        public String channel;
        public String sender;
        public String content;
        public long timestamp;

        public DialogueMessage(String channel, String sender, String content) {
            this.channel = channel;
            this.sender = sender;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
