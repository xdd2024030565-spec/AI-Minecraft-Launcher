package com.aimc.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * LLM 客户端
 *
 * 封装对 OpenAI Chat Completions API 的调用。
 * 支持文本对话和 JSON 动作列表提取。
 */
public class LlmClient {

    private final Retrofit retrofit;
    private final LlmApi api;
    private final Gson gson = new Gson();
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public LlmClient(String apiKey, String model) {
        this(apiKey, model, "https://api.openai.com/v1/");
    }

    public LlmClient(String apiKey, String model, String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
                return chain.proceed(request);
            })
            .build();

        retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        api = retrofit.create(LlmApi.class);
    }

    private interface LlmApi {
        @POST("chat/completions")
        Call<ChatResponse> chatCompletions(@Body ChatRequest request);
    }

    // ==================== 数据类 ====================

    public static class Message {
        public String role;
        public String content;
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ChatRequest {
        public String model;
        public List<Message> messages;
        public double temperature;
        public Integer max_tokens;

        public ChatRequest(String model, List<Message> messages, double temperature, Integer max_tokens) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
            this.max_tokens = max_tokens;
        }
    }

    public static class Choice {
        public Message message;
        public String finish_reason;
    }

    public static class ChatResponse {
        public List<Choice> choices;
    }

    // ==================== 公开方法 ====================

    /**
     * 向 LLM 发送对话请求，返回文本回复
     */
    public String chat(String systemPrompt, String userPrompt) throws Exception {
        ChatRequest request = new ChatRequest(
            model,
            List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
            0.7,
            2000
        );
        Response<ChatResponse> resp = api.chatCompletions(request).execute();
        if (!resp.isSuccessful()) {
            throw new Exception("LLM request failed: " + resp.code());
        }
        ChatResponse response = resp.body();
        if (response == null || response.choices == null || response.choices.isEmpty()) {
            throw new Exception("Empty LLM response");
        }
        return response.choices.get(0).message.content;
    }

    /**
     * 获取结构化的动作列表
     *
     * 要求 LLM 返回 JSON 数组格式的动作。
     * 返回解析后的 Map 列表，每个 Map 包含 action 和参数。
     */
    public List<Map<String, Object>> getActions(String systemPrompt, String userPrompt) throws Exception {
        String response = chat(systemPrompt, userPrompt);
        String json = extractJsonArray(response);
        if (json == null) {
            throw new Exception("LLM did not return valid JSON array: " + response);
        }

        try {
            return gson.fromJson(json, new TypeToken<List<Map<String, Object>>>(){}.getType());
        } catch (Exception e) {
            throw new Exception("Failed to parse JSON actions: " + e.getMessage(), e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 LLM 回复中提取 JSON 数组
     * 支持 ```json ... ``` 代码块包裹的格式
     */
    private String extractJsonArray(String text) {
        // 尝试提取代码块中的 JSON
        Pattern jsonBlockRegex = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```");
        Matcher match = jsonBlockRegex.matcher(text);
        if (match.find()) {
            return match.group(1).trim();
        }

        // 尝试直接提取 JSON 数组
        Pattern arrayRegex = Pattern.compile("(\\[.*\\])", Pattern.DOTALL);
        Matcher arrayMatch = arrayRegex.matcher(text);
        if (arrayMatch.find()) {
            return arrayMatch.group(1).trim();
        }

        return null;
    }
}