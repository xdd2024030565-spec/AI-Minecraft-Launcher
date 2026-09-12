package com.aimc.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 多智能体管理器
 *
 * 管理多个 AI 智能体，以轮询方式 (round-robin) 依次运行。
 * 由于 Minecraft 单玩家机制，智能体轮流控制同一角色，
 * 每个智能体负责不同的子任务，实现协作。
 */
public class AiAgentManager {

    private final List<AiAgent> agents = new ArrayList<>();
    private int currentIndex = 0;

    /**
     * 添加智能体
     */
    public AiAgent addAgent(AiAgent agent) {
        agents.add(agent);
        return agent;
    }

    /**
     * 创建并添加一个预设角色的智能体
     */
    public AiAgent addPresetAgent(String name, GameApiClient gameApi, LlmClient llmClient) {
        String task = PresetRoles.getTaskForRole(name);
        String role = PresetRoles.getRoleName(name);
        AiAgent agent = new AiAgent(name, role, gameApi, llmClient, task);
        agents.add(agent);
        return agent;
    }

    /**
     * 按名称移除智能体
     */
    public void removeAgent(String name) {
        Iterator<AiAgent> it = agents.iterator();
        while (it.hasNext()) {
            if (it.next().getName().equals(name)) {
                it.remove();
            }
        }
        currentIndex = Math.min(currentIndex, Math.max(0, agents.size() - 1));
    }

    public void removeAllAgents() {
        agents.clear();
        currentIndex = 0;
    }

    public List<AiAgent> getAgents() {
        return new ArrayList<>(agents);
    }

    public int getAgentCount() { return agents.size(); }

    /**
     * 运行下一个智能体的决策循环 (轮询)
     *
     * @return 运行结果的字符串描述
     */
    public String runNextTurn() {
        if (agents.isEmpty()) {
            return "No active agents";
        }

        // 跳过非活跃的智能体
        int checked = 0;
        while (checked < agents.size()) {
            AiAgent agent = agents.get(currentIndex % agents.size());
            currentIndex = (currentIndex + 1) % agents.size();
            checked++;

            if (agent.isActive()) {
                DecisionEngine.DecisionResult result = agent.runCycle();
                return agent.getName() + " [" + agent.getRole() + "]: " + result;
            }
        }
        return "All agents inactive";
    }

    /**
     * 运行一整轮 (所有活跃智能体各执行一次)
     */
    public void runRound() {
        int count = agents.size();
        for (int i = 0; i < count; i++) {
            runNextTurn();
        }
    }

    /**
     * 停止所有智能体
     */
    public void shutdown() {
        for (AiAgent agent : agents) {
            agent.setActive(false);
        }
        agents.clear();
        currentIndex = 0;
    }

    // ==================== 预设角色 ====================

    /**
     * 预设智能体角色配置
     */
    public static class PresetRoles {

        private PresetRoles() {}

        public static String getRoleName(String name) {
            switch (name) {
                case "explorer":  return "Explorer";
                case "collector": return "Collector";
                case "builder":   return "Builder";
                case "miner":     return "Miner";
                case "farmer":    return "Farmer";
                default:          return "Agent";
            }
        }

        public static String getTaskForRole(String name) {
            switch (name) {
                case "explorer":
                    return "Explore the world. Search for useful resources, structures, and biomes. " +
                        "Report findings through chat. Move efficiently and avoid danger (lava, cliffs, monsters at night).";
                case "collector":
                    return "Collect resources for the team. Gather wood, stone, iron, and food. " +
                        "Prioritize essential materials: wood, stone, coal, iron, and food items.";
                case "builder":
                    return "Build a shelter and useful structures. Construct a safe house with a bed, crafting table, " +
                        "furnace, and storage. Use materials from the inventory.";
                case "miner":
                    return "Mine underground for ores. Look for coal, iron, gold, diamond. " +
                        "Be careful of lava, monsters, and keep health high. Use torches for light.";
                case "farmer":
                    return "Farm food: plant wheat, carrots, potatoes, and breed animals. " +
                        "Harvest crops when mature and collect animal drops.";
                default:
                    return "Survive and help the team. Collect resources, craft tools, and stay alive.";
            }
        }

        /**
         * 获取所有预设角色名称
         */
        public static List<String> getPresetNames() {
            List<String> names = new ArrayList<>();
            names.add("explorer");
            names.add("collector");
            names.add("builder");
            names.add("miner");
            names.add("farmer");
            return names;
        }
    }
}
