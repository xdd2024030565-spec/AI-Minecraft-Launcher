package com.tungsten.fclcore.game;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 兼容性规则 — 直接取自 FCL CompatibilityRule
 */
public final class CompatibilityRule {

    private final Action action;
    private final OSRestriction os;
    private final Map<String, Boolean> features;

    public CompatibilityRule() {
        this(Action.ALLOW, null);
    }

    public CompatibilityRule(Action action, OSRestriction os) {
        this(action, os, null);
    }

    public CompatibilityRule(Action action, OSRestriction os, Map<String, Boolean> features) {
        this.action = action;
        this.os = os;
        this.features = features;
    }

    public Optional<Action> getAppliedAction(Map<String, Boolean> supportedFeatures) {
        if (os != null && !os.allow())
            return Optional.empty();

        if (features != null)
            for (Map.Entry<String, Boolean> entry : features.entrySet())
                if (!Objects.equals(supportedFeatures.get(entry.getKey()), entry.getValue()))
                    return Optional.empty();

        return Optional.ofNullable(action);
    }

    public static boolean appliesToCurrentEnvironment(Collection<CompatibilityRule> rules) {
        return appliesToCurrentEnvironment(rules, Collections.emptyMap());
    }

    public static boolean appliesToCurrentEnvironment(Collection<CompatibilityRule> rules, Map<String, Boolean> features) {
        if (rules == null || rules.isEmpty())
            return true;

        Action action = Action.DISALLOW;
        for (CompatibilityRule rule : rules) {
            Optional<Action> thisAction = rule.getAppliedAction(features);
            if (thisAction.isPresent())
                action = thisAction.get();
        }

        return action == Action.ALLOW;
    }

    public enum Action {
        ALLOW,
        DISALLOW
    }
}
