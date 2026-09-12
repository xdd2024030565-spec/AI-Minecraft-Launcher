package com.aimc.controller;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * AI 记忆系统
 *
 * 存储决策历史和重要事实，供 LLM 在后续决策中参考。
 * - 短期记忆: 最近 N 次决策的摘要
 * - 长期记忆: 重要地点、资源、危险等关键信息
 */
public class MemorySystem {

    private static final int MAX_SHORT_TERM = 10;
    private static final int MAX_LONG_TERM = 50;

    private final LinkedList<String> shortTermMemory = new LinkedList<>();
    private final List<String> longTermMemory = new ArrayList<>();

    /**
     * 添加一条短期记忆 (最近的决策)
     */
    public void addShortTermEntry(String entry) {
        shortTermMemory.addFirst(entry);
        while (shortTermMemory.size() > MAX_SHORT_TERM) {
            shortTermMemory.removeLast();
        }
    }

    /**
     * 添加一条长期记忆 (重要事实)
     */
    public void addLongTermFact(String fact) {
        if (!longTermMemory.contains(fact)) {
            longTermMemory.add(fact);
            if (longTermMemory.size() > MAX_LONG_TERM) {
                longTermMemory.remove(0);
            }
        }
    }

    /**
     * 获取格式化的记忆文本 (用于 LLM Prompt)
     */
    public String getMemoryText() {
        StringBuilder sb = new StringBuilder();

        if (!longTermMemory.isEmpty()) {
            sb.append("=== MEMORY ===\n");
            for (String fact : longTermMemory) {
                sb.append("  - ").append(fact).append("\n");
            }
        }

        if (!shortTermMemory.isEmpty()) {
            sb.append("=== RECENT ACTIONS ===\n");
            for (int i = 0; i < shortTermMemory.size(); i++) {
                sb.append("  ").append(shortTermMemory.size() - i).append(". ")
                  .append(shortTermMemory.get(i)).append("\n");
            }
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 清空所有记忆
     */
    public void clear() {
        shortTermMemory.clear();
        longTermMemory.clear();
    }

    public int getShortTermCount() { return shortTermMemory.size(); }
    public int getLongTermCount() { return longTermMemory.size(); }
}
