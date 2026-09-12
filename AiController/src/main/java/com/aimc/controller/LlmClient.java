package com.aimc.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 客户端
 *
 * 封装对 OpenAI Chat Completions API 的调用。
 * 支持: 文本对话、JSON 动作列表提取、多模态 (图片+文字) 请求。
 */
public class LlmClient {

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

    // ==================== 公开方法 ====================

    /**
     * 向 LLM 发送对话请求，返回文本回复
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
     * 向多模态 LLM 发送对话请求 (含图片)
     *
     * @param systemPrompt 系统提示
     * @param userPrompt   用户文本
     * @param base64Image  Base64 编码的 PNG 截图
     */
    public String chatWithImage(String systemPrompt, String userPrompt, String base64Image) throws Exception {
        // 构建多模态消息
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");

        JsonArray content = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", userPrompt);
        content.add(textPart);

        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("type", "image_url");
        JsonObject imageUrl = new JsonObject();
        imageUrl.addProperty("url", "data:image/png;base64," + base64Image);
        imagePart.add("image_url", imageUrl);
        content.add(imagePart);

        userMsg.add("content", content);

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.7);

        // 使用 OkHttp 直接发送请求
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
            okhttp3.MediaType.parse("application/json"),
            gson.toJson(requestBody)
        );

        Request request = new Request.Builder()
            .url(baseUrl + "chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("LLM request failed: " + response.code());
            }
            String responseBody = response.body().string();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new Exception("Empty LLM response");
            }
            return choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
        }
    }

    /**
     * 获取结构化的动作列表
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

    /**
     * 获取结构化的动作列表 (含视觉)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActionsWithImage(String systemPrompt, String userPrompt, String base64Image) throws Exception {
        String response = chatWithImage(systemPrompt, userPrompt, base64Image);
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
