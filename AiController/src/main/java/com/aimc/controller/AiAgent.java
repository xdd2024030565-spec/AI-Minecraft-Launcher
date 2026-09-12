package com.aimc.controller;

/**
 * AI 智能体
 *
 * 代表一个独立的 AI 角色，有自己的名称、角色和任务。
 * 每个智能体有独立的 DecisionEngine，可独立决策或与其他智能体协作。
 *
 * 多智能体协作场景:
 * - explorer: 探索世界，寻找资源和结构
 * - collector: 收集指定资源
 * - builder: 建造建筑
 * - miner: 挖矿
 * - farmer: 种植和收获
 */
public class AiAgent {

    private final String name;
    private final String role;
    private final DecisionEngine engine;
    private String lastResult = "";
    private boolean active = true;

    /**
     * @param name     智能体名称 (如 "explorer")
     * @param role     角色描述 (如 "Explorer")
     * @param gameApi  游戏 API 客户端
     * @param llmClient LLM 客户端
     * @param task     任务描述
     */
    public AiAgent(String name, String role, GameApiClient gameApi, LlmClient llmClient, String task) {
        this.name = name;
        this.role = role;
        this.engine = new DecisionEngine(gameApi, llmClient);
        this.engine.setCurrentTask(task);
    }

    /**
     * 运行一个决策循环
     */
    public DecisionEngine.DecisionResult runCycle() {
        DecisionEngine.DecisionResult result = engine.runDecisionCycle();
        lastResult = result.toString();
        return result;
    }

    public String getName() { return name; }
    public String getRole() { return role; }
    public int getCycleCount() { return engine.getCycleCount(); }
    public String getLastError() { return engine.getLastError(); }
    public String getLastResult() { return lastResult; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * 配置视觉模式
     */
    public void setVisualMode(boolean enabled) { engine.setVisualMode(enabled); }

    /**
     * 配置记忆系统
     */
    public void setMemoryEnabled(boolean enabled) { engine.setMemoryEnabled(enabled); }

    /**
     * 获取该智能体的记忆
     */
    public MemoryStore getMemory() { return engine.getMemory(); }

    @Override
    public String toString() {
        return "Agent{name='" + name + "', role='" + role + "', active=" + active + "}";
    }
}
