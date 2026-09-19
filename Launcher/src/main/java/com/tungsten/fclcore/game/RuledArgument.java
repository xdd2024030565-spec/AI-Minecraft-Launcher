package com.tungsten.fclcore.game;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 条件参数 — 直接取自 FCL RuledArgument
 *
 * 只有当 CompatibityRule 全部通过时才输出参数。
 */
public final class RuledArgument implements Argument {

    private final List<CompatibilityRule> rules;
    private final List<String> value;

    public RuledArgument() {
        this(null, null);
    }

    public RuledArgument(List<CompatibilityRule> rules, List<String> value) {
        this.rules = rules;
        this.value = value;
    }

    public List<CompatibilityRule> getRules() { return rules; }
    public List<String> getValue() { return value; }

    @Override
    public List<String> toString(Map<String, String> keys, Map<String, Boolean> features) {
        if (CompatibilityRule.appliesToCurrentEnvironment(rules, features) && value != null) {
            java.util.List<String> result = new java.util.ArrayList<>();
            for (String v : value) {
                result.addAll(new StringArgument(v).toString(keys, features));
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return value == null ? "[]" : value.toString();
    }
}
