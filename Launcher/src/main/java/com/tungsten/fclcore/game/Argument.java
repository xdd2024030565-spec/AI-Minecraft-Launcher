package com.tungsten.fclcore.game;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * 启动参数 — 直接取自 FCL Argument
 *
 * 参数可以是纯字符串，也可以是带条件规则的参数。
 */
@JsonAdapter(Argument.Deserializer.class)
public interface Argument {

    /**
     * 解析参数中的 ${key} 占位符
     *
     * @param keys     占位符映射
     * @param features 特性映射 (如 has_custom_resolution)
     * @return 解析后的参数列表，空表示忽略
     */
    List<String> toString(Map<String, String> keys, Map<String, Boolean> features);

    class Deserializer implements JsonDeserializer<Argument> {
        @Override
        public Argument deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive())
                return new StringArgument(json.getAsString());
            else
                return context.deserialize(json, RuledArgument.class);
        }
    }
}
