package com.aimc.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 客户端
 *
 * 封装对 OpenAI Chat Completions API 的调用。
 * 支持: 文本对话、JSON 动作列表提取、多模态视觉决策 (text + image)。
 */
public class LlmClient {

    private final LlmApi api;
    private final Gson gson = new Gson();
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final OkHttpClient httpClient;

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

        this.httpClient = client;

        Retrofit retrofit = new Retrofit.Builder()
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

        public ChatRequest(String model, List<Message> messages,
                           double temperature, Integer max_tokens) {
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

    // ==================== 纯文本方法 ====================

    /**
     * 向 LLM 发送纯文本对话请求，返回文本回复
     */
    public String chat(String systemPrompt, String userPrompt) throws Exception {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", userPrompt));

        ChatRequest request = new ChatRequest(model, messages, 0.7, 2000);

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
     * 获取结构化的动作列表 (纯文本)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActions(String systemPrompt, String userPrompt) throws Exception {
        String response = chat(systemPrompt, userPrompt);
        String json = extractJsonArray(response);
        if (json == null) {
            throw new Exception("LLM did not return valid JSON array: " + response);
        }
        try {
            List<Map<String, Object>> result = gson.fromJson(json,
                new TypeToken<List<Map<String, Object>>>(){}.getType());
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            throw new Exception("Failed to parse JSON actions: " + e.getMessage(), e);
        }
    }

    // ==================== 多模态视觉方法 ====================

    /**
     * 向 LLM 发送带图片的对话请求 (多模态)
     *
     * 使用 OpenAI Chat Completions API 的 content array 格式:
     * [{"type":"text","text":"..."}, {"type":"image_url","image_url":{"url":"data:..."}}]
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户文本提示
     * @param imageBytes PNG 图片二进制数据
     * @return LLM 文本回复
     */
    public String chatWithImage(String systemPrompt, String userPrompt, byte[] imageBytes) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("model", model);
        request.addProperty("temperature", 0.7);
        request.addProperty("max_tokens", 2000);

        JsonArray messages = new JsonArray();

        // System message (text only)
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        // User message (text + image)
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        JsonArray content = new JsonArray();

        // Text part
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", userPrompt);
        content.add(textPart);

        // Image part
        if (imageBytes != null && imageBytes.length > 0) {
            JsonObject imagePart = new JsonObject();
            imagePart.addProperty("type", "image_url");
            JsonObject imageUrl = new JsonObject();
            imageUrl.addProperty("url", "data:image/png;base64," +
                Base64.getEncoder().encodeToString(imageBytes));
            imagePart.add("image_url", imageUrl);
            content.add(imagePart);
        }

        userMsg.add("content", content);
        messages.add(userMsg);

        request.add("messages", messages);

        // 使用 OkHttp 直接发送请求 (Retrofit 不便处理 content array 格式)
        MediaType jsonType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(request.toString(), jsonType);
        Request httpRequest = new Request.Builder()
            .url(baseUrl + "chat/completions")
            .post(body)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .build();

        okhttp3.Response response = httpClient.newCall(httpRequest).execute();
        if (!response.isSuccessful()) {
            throw new Exception("LLM request failed: " + response.code());
        }

        String responseStr = response.body().string();
        JsonObject responseJson = JsonParser.parseString(responseStr).getAsJsonObject();
        JsonArray choices = responseJson.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new Exception("Empty LLM response");
        }
        return choices.get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();
    }

    /**
     * 获取结构化的动作列表 (带图片)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActionsWithImage(String systemPrompt, String userPrompt, byte[] imageBytes) throws Exception {
        String response = chatWithImage(systemPrompt, userPrompt, imageBytes);
        String json = extractJsonArray(response);
        if (json == null) {
            throw new Exception("LLM did not return valid JSON array: " + response);
        }
        try {
            List<Map<String, Object>> result = gson.fromJson(json,
                new TypeToken<List<Map<String, Object>>>(){}.getType());
            return result != null ? result : new ArrayList<>();
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
        Pattern jsonBlockRegex = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```");
        Matcher match = jsonBlockRegex.matcher(text);
        if (match.find()) {
            return match.group(1).trim();
        }

        Pattern arrayRegex = Pattern.compile("(\\[.*\\])", Pattern.DOTALL);
        Matcher arrayMatch = arrayRegex.matcher(text);
        if (arrayMatch.find()) {
            return arrayMatch.group(1).trim();
        }

        return null;
    }
}
