package com.aimc.controller

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * LLM 客户端
 *
 * 封装对 OpenAI Chat Completions API 的调用。
 * 支持通过系统提示词 (System Prompt) 定义 AI 行为。
 *
 * 注意: 此版本为 OpenAI 兼容接口。如果使用其他 LLM (Claude, DeepSeek 等)，
 * 请修改 BASE_URL 和请求格式。
 */
class LlmClient(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    private val baseUrl: String = "https://api.openai.com/v1/"
) {
    private val retrofit: Retrofit
    private val api: LlmApi
    private val gson = Gson()

    init {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(LlmApi::class.java)
    }

    // ==================== API 接口 ====================

    private interface LlmApi {
        @POST("chat/completions")
        suspend fun chatCompletions(@Body request: ChatRequest): ChatResponse
    }

    // ==================== 请求/响应数据类 ====================

    data class Message(val role: String, val content: String)
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double = 0.7,
        val max_tokens: Int? = 2000
    )

    data class Choice(val message: Message, val finish_reason: String? = null)
    data class ChatResponse(val choices: List<Choice>)

    // ==================== 公开方法 ====================

    /**
     * 向 LLM 发送对话请求，返回文本回复
     */
    suspend fun chat(systemPrompt: String, userPrompt: String): String {
        val request = ChatRequest(
            model = model,
            messages = listOf(
                Message("system", systemPrompt),
                Message("user", userPrompt)
            )
        )
        val response = api.chatCompletions(request)
        return response.choices.firstOrNull()?.message?.content ?: "(no response)"
    }

    /**
     * 获取结构化的动作列表
     *
     * 要求 LLM 返回 JSON 数组格式的动作。
     * 返回解析后的 Map 列表，每个 Map 包含 action 和参数。
     */
    suspend fun getActions(systemPrompt: String, userPrompt: String): List<Map<String, Any>> {
        val response = chat(systemPrompt, userPrompt)

        // 尝试从回复中提取 JSON 数组
        val json = extractJsonArray(response)
            ?: throw Exception("LLM did not return valid JSON array: $response")

        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            gson.fromJson<List<Map<String, Any>>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            throw Exception("Failed to parse JSON actions: ${e.message}", e)
        }
    }

    /**
     * 获取图像 + 文本的多模态回复
     *
     * @param textPrompt 文本提示
     * @param imageBytes PNG 图片字节数组
     */
    suspend fun chatWithImage(textPrompt: String, imageBytes: ByteArray): String {
        // 注意: 多模态需要不同的请求格式 (OpenAI Vision API)
        // 这里提供一个基础实现，实际使用时需要根据 LLM 调整
        val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)

        val content = listOf(
            mapOf("type" to "text", "text" to textPrompt),
            mapOf(
                "type" to "image_url",
                "image_url" to mapOf(
                    "url" to "data:image/png;base64,$base64Image"
                )
            )
        )

        // 此处使用 OkHttp 直接发送请求（因为 Retrofit 接口不同）
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        val jsonBody = gson.toJson(mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "user", "content" to content)
            ),
            "max_tokens" to 2000
        ))

        val request = Request.Builder()
            .url(baseUrl + "chat/completions")
            .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("LLM request failed: ${response.code()} ${response.body?.string()}")
        }

        val respJson = response.body?.string() ?: "{}"
        val respMap = gson.fromJson(respJson, Map::class.java)
        val choices = respMap["choices"] as? List<*>
        val first = choices?.firstOrNull() as? Map<String, Any>
        val message = first?.["message"] as? Map<String, Any>
        return message?.["content"] as? String ?: "(no response)"
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 LLM 回复中提取 JSON 数组
     * 支持 ```json ... ``` 代码块包裹的格式
     */
    private fun extractJsonArray(text: String): String? {
        // 尝试提取代码块中的 JSON
        val jsonBlockRegex = Regex("""```(?:json)?\s*\n?([\s\S]*?)\n?```""")
        val match = jsonBlockRegex.find(text)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // 尝试直接提取 JSON 数组
        val arrayRegex = Regex("""(\[.*\])""", RegexOption.DOT_MATCH_ALL)
        val arrayMatch = arrayRegex.find(text)
        if (arrayMatch != null) {
            return arrayMatch.groupValues[1].trim()
        }

        return null
    }
}