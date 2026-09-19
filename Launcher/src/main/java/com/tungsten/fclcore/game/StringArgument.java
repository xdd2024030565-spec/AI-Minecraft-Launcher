package com.tungsten.fclcore.game;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串参数 — 直接取自 FCL StringArgument
 *
 * 支持 ${key} 占位符替换。
 */
public final class StringArgument implements Argument {

    private static final Pattern ARG_PATTERN = Pattern.compile("\\$\\{(?<name>[^}]+)}");

    private final String argument;

    public StringArgument(String argument) {
        this.argument = argument;
    }

    public String getArgument() { return argument; }

    @Override
    public List<String> toString(Map<String, String> keys, Map<String, Boolean> features) {
        String result = argument;
        Matcher matcher = ARG_PATTERN.matcher(argument);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        boolean resolved = true;
        while (matcher.find()) {
            sb.append(argument, lastEnd, matcher.start());
            String name = matcher.group("name");
            String value = keys.get(name);
            if (value == null) {
                resolved = false;
                break;
            }
            sb.append(value);
            lastEnd = matcher.end();
        }
        if (resolved) {
            sb.append(argument, lastEnd, argument.length());
            result = sb.toString();
        }

        if (result.isEmpty()) return Collections.emptyList();
        return Collections.singletonList(result);
    }

    @Override
    public String toString() { return argument; }
}
