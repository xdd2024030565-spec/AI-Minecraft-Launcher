package com.aimc.controller;

import java.util.*;
import java.util.Map.Entry;

/**
 * AI 决策引擎
 *
 * 将游戏状态转换为 LLM Prompt，获取动作列表，并通过 GameApiClient 执行。
 * 支持多种任务模式（探索、收集、建造等）。
 */
public class DecisionEngine {

    private final GameApiClient gameApi;
    private final LlmClient llmClient;

    private static final int MAX_ACTIONS_PER_CYCLE = 5;
    private static final int MAX_PROMPT_LENGTH = 4000;

    private String currentTask = "Explore the world and survive. Collect resources, build shelter, and stay alive.";
    private int cycleCount = 0;
    private String lastError = null;

    public DecisionEngine(GameApiClient gameApi, LlmClient llmClient) {
        this.gameApi = gameApi;
        this.llmClient = llmClient;
    }

    public String getCurrentTask() { return currentTask; }
    public void setCurrentTask(String task) { this.currentTask = task; }
    public int getCycleCount() { return cycleCount; }
    public String getLastError() { return lastError; }

    /**
     * 执行一次决策循环:
     * 1. 收集游戏状态
     * 2. 构建 Prompt
     * 3. 调用 LLM 获取动作
     * 4. 执行动作
     *
     * @return 执行结果
     */
    public DecisionResult runDecisionCycle() {
        cycleCount++;
        lastError = null;

        try {
            // 1. 收集游戏状态
            GameApiClient.GameStateResponse gameState = gameApi.getGameState();
            if (!gameState.connected) {
                return new Skip("Player not connected to world");
            }

            GameApiClient.InventoryResponse inventory = null;
            GameApiClient.BlocksResponse blocks = null;
            try { inventory = gameApi.getInventory(); } catch (Exception e) { /* ignore */ }
            try { blocks = gameApi.getNearbyBlocks(6); } catch (Exception e) { /* ignore */ }

            // 2. 构建 Prompt
            String prompt = buildPrompt(gameState, inventory, blocks);

            // 3. 调用 LLM 获取动作
            String systemPrompt = buildSystemPrompt();
            List<Map<String, Object>> actions = llmClient.getActions(systemPrompt, prompt);

            if (actions.isEmpty()) {
                return new Skip("LLM returned no actions");
            }

            // 4. 执行动作 (限制每周期最多执行 MAX_ACTIONS_PER_CYCLE)
            List<String> executed = new ArrayList<>();
            int limit = Math.min(actions.size(), MAX_ACTIONS_PER_CYCLE);
            for (int i = 0; i < limit; i++) {
                Map<String, Object> action = actions.get(i);
                Object actionNameObj = action.get("action");
                if (actionNameObj == null) continue;
                String actionName = actionNameObj.toString();
                try {
                    gameApi.executeAction(actionName, action);
                    executed.add(actionName);
                } catch (Exception e) {
                    lastError = "Failed to execute " + actionName + ": " + e.getMessage();
                    System.err.println(lastError);
                }
            }

            return new Success(cycleCount, executed, prompt.length());

        } catch (Exception e) {
            lastError = e.getMessage();
            return new Error(e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return "You are an AI playing Minecraft. Your goal is to complete the current task.\n\n" +
            "Available actions:\n" +
            "- move: {direction: forward/back/left/right, duration: ticks}\n" +
            "- look: {yaw: -180~180, pitch: -90~90}\n" +
            "- jump: {}\n" +
            "- mine: {duration: ticks}\n" +
            "- place: {}\n" +
            "- attack: {}\n" +
            "- use: {}\n" +
            "- inventory_click: {slot: 0-35, button: left/right}\n" +
            "- drop_item: {slot: 0-35}\n" +
            "- toggle_sprint: {}\n" +
            "- chat: {message: string}\n\n" +
            "Rules:\n" +
            "- Return ONLY a JSON array of actions, no explanation.\n" +
            "- Example: [{\"action\":\"move\",\"direction\":\"forward\",\"duration\":10}]\n" +
            "- Be efficient and goal-oriented.\n" +
            "- If the task requires crafting, first mine necessary resources.\n" +
            "- If health < 5, consider searching for food or retreating.";
    }

    /**
     * 构建用户提示词 (包含当前游戏状态)
     */
    private String buildPrompt(GameApiClient.GameStateResponse gameState,
                                GameApiClient.InventoryResponse inventory,
                                GameApiClient.BlocksResponse blocks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current task: ").append(currentTask).append("\n");
        sb.append("=== PLAYER STATE ===\n");
        sb.append("Position: ").append(formatDoubleList(gameState.position)).append("\n");
        sb.append("Health: ").append(gameState.health).append("/20\n");
        sb.append("Food: ").append(gameState.food).append("/20\n");
        sb.append("Level: ").append(gameState.xpLevel).append(" (")
          .append((int)(gameState.xpProgress * 100)).append("%)\n");
        sb.append("Rotation: yaw=").append(formatDoubleList(gameState.rotation)).append("\n");
        sb.append("Dimension: ").append(gameState.dimension != null ? gameState.dimension : "unknown").append("\n");
        sb.append("Time: ").append(gameState.isDay ? "DAY" : "NIGHT")
          .append(" (tick ").append(gameState.worldTime).append(")\n");

        // 背包摘要
        if (inventory != null && inventory.mainInventory != null) {
            sb.append("=== INVENTORY ===\n");
            List<GameApiClient.ItemInfo> items = new ArrayList<>();
            for (GameApiClient.ItemInfo item : inventory.mainInventory) {
                if (item != null && !"air".equals(item.name) && item.count > 0) {
                    items.add(item);
                }
            }
            if (!items.isEmpty()) {
                for (int i = 0; i < Math.min(items.size(), 10); i++) {
                    GameApiClient.ItemInfo item = items.get(i);
                    sb.append("  [").append(item.slot).append("] ")
                      .append(item.count).append("x ").append(item.name).append("\n");
                }
                if (items.size() > 10) {
                    sb.append("  ... and ").append(items.size() - 10).append(" more items\n");
                }
            } else {
                sb.append("  (empty)\n");
            }
        }

        // 附近方块
        if (blocks != null && blocks.blockSummary != null) {
            sb.append("=== NEARBY BLOCKS (radius ").append(blocks.scanRadius).append(") ===\n");
            Set<Entry<String, Integer>> entries = blocks.blockSummary.entrySet();
            int count = 0;
            for (Entry<String, Integer> entry : entries) {
                if (count++ >= 10) break;
                sb.append("  ").append(entry.getValue()).append(" x ").append(entry.getKey()).append("\n");
            }
        }

        // 附近实体
        if (blocks != null && blocks.nearbyEntities != null && !blocks.nearbyEntities.isEmpty()) {
            sb.append("=== NEARBY ENTITIES ===\n");
            for (int i = 0; i < Math.min(blocks.nearbyEntities.size(), 5); i++) {
                GameApiClient.EntityInfo entity = blocks.nearbyEntities.get(i);
                sb.append("  ").append(entity.type)
                  .append(" at distance ").append(String.format("%.1f", entity.distance)).append("\n");
            }
        }

        String prompt = sb.toString();
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            prompt = prompt.substring(0, MAX_PROMPT_LENGTH) + "\n... [truncated]";
        }
        return prompt;
    }

    private String formatDoubleList(List<Double> list) {
        if (list == null) return "??";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.1f", list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    // ==================== 决策结果类 (非静态内部类，可直接 instanceof) ====================

    public static abstract class DecisionResult {
        @Override
        public abstract String toString();
    }

    public static class Success extends DecisionResult {
        public final int cycleNumber;
        public final List<String> actionsExecuted;
        public final int promptLength;

        public Success(int cycleNumber, List<String> actionsExecuted, int promptLength) {
            this.cycleNumber = cycleNumber;
            this.actionsExecuted = actionsExecuted;
            this.promptLength = promptLength;
        }

        @Override
        public String toString() {
            return "Success{cycle=" + cycleNumber + ", actions=" + actionsExecuted + "}";
        }
    }

    public static class Skip extends DecisionResult {
        public final String reason;
        public Skip(String reason) { this.reason = reason; }
        @Override public String toString() { return "Skip{" + reason + "}"; }
    }

    public static class Error extends DecisionResult {
        public final String message;
        public Error(String message) { this.message = message; }
        @Override public String toString() { return "Error{" + message + "}"; }
    }
}