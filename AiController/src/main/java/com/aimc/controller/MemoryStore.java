package com.aimc.controller;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * AI 记忆系统
 *
 * 存储 AI 的过去行为和观察，供 LLM 在决策时参考。
 * 记忆条目包括: 动作执行、状态快照、重要事件。
 * 最大保留 50 条，超出后自动清除最旧的。
 */
public class MemoryStore {

    private static final int MAX_ENTRIES = 50;
    private final LinkedList<MemoryEntry> entries = new LinkedList<>();
    private boolean enabled = true;

    /**
     * 添加记忆条目
     */
    public void addEntry(String type, String content) {
        if (!enabled) return;
        entries.addLast(new MemoryEntry(type, content));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    /**
     * 获取最近的 N 条记忆
     */
    public List<MemoryEntry> getRecentEntries(int count) {
        count = Math.min(count, entries.size());
        List<MemoryEntry> result = new ArrayList<>();
        int start = Math.max(0, entries.size() - count);
        for (int i = start; i < entries.size(); i++) {
            result.add(entries.get(i));
        }
        return result;
    }

    /**
     * 获取记忆摘要 (用于 LLM prompt)
     */
    public String getSummary() {
        if (!enabled || entries.isEmpty()) {
            return "(no memory yet)";
        }
        StringBuilder sb = new StringBuilder();
        int count = Math.min(entries.size(), 10);
        List<MemoryEntry> recent = getRecentEntries(count);
        for (MemoryEntry entry : recent) {
            sb.append("  [").append(entry.type).append("] ")
              .append(entry.content).append("\n");
        }
        return sb.toString();
    }

    public void clear() { entries.clear(); }
    public int size() { return entries.size(); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * 记忆条目数据类
     */
    public static class MemoryEntry {
        public String type;    // "action", "state", "event"
        public String content;
        public long timestamp;

        public MemoryEntry(String type, String content) {
            this.type = type;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
